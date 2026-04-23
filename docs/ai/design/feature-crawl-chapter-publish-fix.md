# Feature Design: Fix Comic Crawl — Chapter Event Publishing

**Date:** 2026-04-23  
**Status:** Approved  
**Scope:** Fix missing chapter `ChapterPendingEvent` publish after comic crawl completes; move chapter publish ownership to `crawl-service`; add manual retry endpoint.

---

## Problem Statement

When `crawl-service` crawls a comic, it publishes `ComicExtractedEvent` to RabbitMQ. `core-service` consumes this, persists the comic and stub chapters, then publishes `ChapterPendingEvent × N` back to RabbitMQ. This two-hop async chain means `crawl-service` has no visibility into whether chapter events were published. `retryFailedChapters()` in `crawl-service` is a no-op — there is no manual trigger path when chapter publishing fails or is never reached.

---

## Goals

1. Fix the root cause: crawl-service owns chapter event publishing, not core-service.
2. Remove `ComicExtractedEvent` RabbitMQ queue — replace with synchronous REST call.
3. Add `POST /api/v1/crawl/nettruyen/comic/{slug}/chapters/retry` endpoint to manually re-publish `ChapterPendingEvent` for INIT/STARTED chapters.
4. Auto-publish chapter events on first successful comic crawl.

---

## Architecture

### Before

```
crawl-svc.crawlNettruyenComic()
  → publish ComicExtractedEvent [RabbitMQ: comic-extracted]
    → core-svc consumes → persists Comic + Chapters
    → core-svc publishes ChapterPendingEvent × N [RabbitMQ: chapter-pending]
      → crawl-svc consumes → crawls images
      → crawl-svc publishes ChapterImagesExtractedEvent
        → core-svc persists images
```

### After

```
crawl-svc.crawlNettruyenComic()
  → POST /internal/comics (REST, sync)
    → core-svc persists Comic + stub Chapters
    → returns List<SavedChapterInfo>
  ← crawl-svc receives chapter list
  → crawl-svc publishes ChapterPendingEvent × N [RabbitMQ: chapter-pending]
    → crawl-svc consumes → crawls images           (unchanged)
    → crawl-svc publishes ChapterImagesExtractedEvent  (unchanged)
      → core-svc persists images                   (unchanged)

Manual retry:
POST /api/v1/crawl/nettruyen/comic/{slug}/chapters/retry
  → crawl-svc calls GET /internal/comics/{slug}/chapters?status=INIT,STARTED
    → core-svc returns List<SavedChapterInfo> for pending chapters
  → crawl-svc publishes ChapterPendingEvent × N
```

**RabbitMQ queues removed:** `comic-extracted` exchange and queue.  
**RabbitMQ queues unchanged:** `chapter-pending`, `chapter-images-extracted`.

---

## Component Changes

### `shared-lib`

| Item | Action | Detail |
|---|---|---|
| `SavedChapterInfo` | **Add** record | `{Long chapterId, String chapterNumber, String originUrl, String originPathParams, String comicSlug}` |

### `core-service`

| Item | Action | Detail |
|---|---|---|
| `ComicPersistService` | **New** `@ApplicationScoped` | `@Transactional persistComic(ComicExtractedEvent)` — extracts persist logic from old consumer; returns `List<SavedChapterInfo>` |
| `InternalResource` | **Add** endpoint | `POST /internal/comics` — delegates to `ComicPersistService`, returns `201 Created + List<SavedChapterInfo>` |
| `InternalResource` | **Add** endpoint | `GET /internal/comics/{slug}/chapters?status=INIT,STARTED` — queries `ChapterRepository.findByComicSlugAndStatusIn()`, returns `List<SavedChapterInfo>` |
| `ComicExtractedConsumer` | **Delete** | No longer needed |
| `ChapterPendingProducer` | **Delete** | Moved to crawl-service |
| `application.properties` | **Remove** | `comic-extracted-in` and `chapter-pending-out` channel config blocks |

### `crawl-service`

| Item | Action | Detail |
|---|---|---|
| `CoreServiceClient` | **Add** method | `saveComic(ComicExtractedEvent): List<SavedChapterInfo>` → `POST /internal/comics` (expects 201) |
| `CoreServiceClient` | **Add** method | `getPendingChapters(String slug): List<SavedChapterInfo>` → `GET /internal/comics/{slug}/chapters?status=INIT,STARTED` |
| `ChapterPendingProducer` | **New** | Moved from core-service; publishes to `chapter.pending.exchange` (direct, durable, routing-key=`pending`) |
| `CrawlServiceImpl.crawlNettruyenComic()` | **Fix** | Replace `comicExtractedProducer.send(event)` with `coreServiceClient.saveComic(event)` then iterate `SavedChapterInfo` list and publish `ChapterPendingEvent × N` |
| `CrawlServiceImpl.retryFailedChapters()` | **Implement** | Call `coreServiceClient.getPendingChapters(slug)`, publish `ChapterPendingEvent` for each |
| `CrawlRestResource` | **Add** endpoint | `POST /api/v1/crawl/nettruyen/comic/{slug}/chapters/retry` → calls `retryFailedChapters(slug)` |
| `ComicExtractedProducer` | **Delete** | No longer needed |
| `application.properties` | **Remove** | `comic-extracted-out` outgoing channel |
| `application.properties` | **Add** | `chapter-pending-out` outgoing channel (exchange: `chapter.pending.exchange`, type: direct, durable: true, routing-key: `pending`) |

---

## Data Flow Detail

### First Crawl (`crawlNettruyenComic`)

1. Pre-flight: `coreServiceClient.comicExists(slugNId)` → 409 guard (unchanged)
2. Extract: `nettruyenExtractor.extractComicDetail(slugNId)`
3. MinIO: rename thumb from temp → permanent path
4. Build `ComicExtractedEvent` (same structure, now used as REST body)
5. REST: `POST /internal/comics` → receive `List<SavedChapterInfo>`
6. For each `SavedChapterInfo`: publish `ChapterPendingEvent(chapterId, comicSlug, chapterNumber, originUrl, originPathParams)`
7. Log success

### Manual Retry (`retryFailedChapters`)

1. REST: `GET /internal/comics/{slug}/chapters?status=INIT,STARTED` → `List<SavedChapterInfo>`
2. If empty → log "nothing to retry", return
3. For each: publish `ChapterPendingEvent`
4. `ChapterPendingConsumer` skips `FINISHED` chapters naturally

---

## Error Handling

| Scenario | Behavior |
|---|---|
| `core-service` down during `POST /internal/comics` | REST throws → crawl-svc surfaces 503 to caller. No partial DB state. MinIO thumb cleaned by `FileTempClearer`. |
| Duplicate comic (`POST /internal/comics` returns 409) | crawl-svc throws `ConflictException` — same as existing behaviour |
| `ChapterPendingEvent` publish fails after REST success | Comic+chapters in DB with `CrawlStatus.INIT`. Operator calls retry endpoint. |
| Retry on already-FINISHED comic | `GET /internal/comics/{slug}/chapters?status=INIT,STARTED` returns empty list → no-op |
| core-service down during retry | REST throws → crawl-svc returns 503 |

---

## Out of Scope

- `publishStatus` / `DRAFT` → `PUBLISHED` lifecycle (separate design: `feature-crawl-rest-publish-state.md`)
- Bulk comic crawl
- Discord/Telegram notification on crawl complete

---

## Config Summary

### `crawl-service/application.properties` delta

Remove:
```properties
mp.messaging.outgoing.comic-extracted-out.*
```

Add:
```properties
mp.messaging.outgoing.chapter-pending-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.chapter-pending-out.exchange.name=chapter.pending.exchange
mp.messaging.outgoing.chapter-pending-out.exchange.type=direct
mp.messaging.outgoing.chapter-pending-out.exchange.durable=true
mp.messaging.outgoing.chapter-pending-out.default-routing-key=pending
mp.messaging.outgoing.chapter-pending-out.content-type=application/json
```

### `core-service/application.properties` delta

Remove:
```properties
mp.messaging.incoming.comic-extracted-in.*
mp.messaging.outgoing.chapter-pending-out.*
```
