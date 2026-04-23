# Feature Design: Crawl Comic via REST + Comic Publish State

**Date:** 2026-04-23  
**Status:** Approved  
**Scope:** Replace `ComicExtractedEvent` RabbitMQ queue with synchronous REST call; add `publishStatus` lifecycle field to `Comic`.

---

## Problem Statement

Current comic crawl pipeline uses RabbitMQ for all service-to-service communication, including the initial comic metadata step which is fast (scrapes metadata, not images). This adds broker overhead and async complexity for a short-lived, low-volume, request/response operation. Additionally, newly crawled comics have no concept of "not yet visible to users" — they could potentially surface in GraphQL queries before an admin has reviewed them.

---

## Goals

1. Replace `ComicExtractedEvent` → RabbitMQ publish with a synchronous REST call from `crawl-service` to `core-service`.
2. Move `ChapterPendingEvent` publishing responsibility from `core-service` to `crawl-service` (crawl pipeline owns crawl sequencing).
3. Introduce `publishStatus` on `Comic` entity to gate user visibility (`DRAFT` → `PUBLISHED`).
4. Keep chapter image pipeline (`ChapterPendingEvent`, `ChapterImagesExtractedEvent`) on RabbitMQ unchanged.

---

## Architecture

### Before

```
crawl-svc scrapes metadata
  → publish ComicExtractedEvent [RabbitMQ: comic-extracted]
    → core-svc consumes → persists Comic + Chapters
    → core-svc publishes N × ChapterPendingEvent [RabbitMQ: chapter-pending]
      → crawl-svc consumes → scrapes images
      → crawl-svc publishes ChapterImagesExtractedEvent [RabbitMQ: chapter-images-extracted]
        → core-svc consumes → persists images
```

### After

```
crawl-svc scrapes metadata
  → POST /internal/comics (REST, sync)
    → core-svc persists Comic (publishStatus=DRAFT) + stub Chapters
    → returns List<SavedChapterInfo> [{chapterId, chapterName, originPathParams, originUrl}]
  ← crawl-svc receives chapter list
  → crawl-svc publishes N × ChapterPendingEvent [RabbitMQ: chapter-pending]
    → crawl-svc consumes → scrapes images
    → crawl-svc publishes ChapterImagesExtractedEvent [RabbitMQ: chapter-images-extracted]
      → core-svc consumes → persists images (unchanged)

Admin:
  → PATCH /internal/comics/{slug}/publish
    → core-svc sets publishStatus = PUBLISHED
    → comic now visible in GraphQL queries
```

**RabbitMQ queues remaining:** `chapter-pending`, `chapter-images-extracted` only.

---

## Comic Publish State

### Enum: `PublishStatus`

| Value | Meaning |
|---|---|
| `DRAFT` | Freshly crawled. Not visible to users. |
| `PUBLISHED` | Admin reviewed and published. Visible in GraphQL. |

### Lifecycle

```
Comic crawled → DRAFT
  (all chapters crawled → still DRAFT, future: notify admin via Discord/Telegram)
Admin reviews → PATCH /internal/comics/{slug}/publish → PUBLISHED
Users see comic in queries
```

### DB Migration

New Liquibase changeset: add `publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` to `comics` table.

**Existing rows:** All existing comics become `DRAFT` on migration and are invisible to display-service until published. Include a second Liquibase changeset immediately after the column addition to backfill all existing rows to `PUBLISHED` (they were already crawled and working before this feature). New comics created after this migration start as `DRAFT`.

---

## Component Changes

### `core-service`

| Item | Change |
|---|---|
| `InternalResource` | Add `POST /internal/comics` — accepts `ComicExtractedEvent` body, delegates to a new `ComicPersistService.persistComic()` method, returns `201 Created` with `List<SavedChapterInfo>`. |
| `ComicPersistService` (new) | Extracts persist logic from `ComicExtractedConsumer` into a dedicated service. Method `persistComic(ComicExtractedEvent)` annotated `@Transactional` (per project convention: transactions on service methods, not resources). Calls `TagResolverService.resolveOrCreateTags()` and `incrementComicCount()`. Returns `List<SavedChapterInfo>`. |
| `InternalResource` | Add `PATCH /internal/comics/{slug}/publish` — sets `publishStatus = PUBLISHED`, returns `204 No Content`. Returns `404` if slug not found. Internal-network trust (same as all `/internal` endpoints — no auth token required). |
| `ComicExtractedConsumer` | **Delete** |
| `ChapterPendingProducer` | **Delete** |
| Liquibase | Two changesets: (1) add `publish_status` column with `DEFAULT 'DRAFT'`; (2) backfill all existing rows to `PUBLISHED` |
| `application.yaml` | Remove `comic-extracted-in` and `chapter-pending-out` channel bindings |

### `crawl-service`

| Item | Change |
|---|---|
| `CoreServiceClient` | Add `saveComic(ComicExtractedEvent)` returning `List<SavedChapterInfo>` (expects HTTP 201) |
| `CrawlServiceImpl.crawlNettruyenComic()` | Replace `comicExtractedProducer.send(event)` with `coreServiceClient.saveComic(event)`, then publish `ChapterPendingEvent` × N. Keep the pre-flight `coreServiceClient.comicExists()` check — it prevents wasted scraping + MinIO download before the 409 is raised. The `POST /internal/comics` 409 acts as a secondary safety net. |
| `ComicExtractedProducer` | **Delete** |
| `ChapterPendingProducer` | **Add** (moved from core-service) |
| `application.yaml` | Remove `comic-extracted-out` channel; add `chapter-pending-out` outgoing channel (moved from core-service): exchange `chapter.pending.exchange`, type `direct`, durable `true`, routing-key `pending`, content-type `application/json` |

### `display-service`

| Item | Change |
|---|---|
| `ComicRepository` | `findPageableComics()`: replace `findAll(sort)` with `find("publishStatus", PublishStatus.PUBLISHED, sort)`. `findBySlug()`: add `AND publishStatus = PUBLISHED` filter — `DRAFT` comics return empty `Optional` → 404. These are the only two query methods in this repository; no other methods need changes. Admins use the `/internal` endpoints to inspect DRAFT comics. |
| `ComicGqlResource` | No interface change — filtering is transparent to GraphQL callers |

### `shared-lib`

| Item | Change |
|---|---|
| `ComicExtractedEvent` | No change — reused as REST request body |
| `PublishStatus` enum | **Add** (`DRAFT`, `PUBLISHED`) |
| `PublishStatusConverter` | **Add** — JPA `AttributeConverter<PublishStatus, String>` mapping enum to string column value (pattern: `CrawlStatusConverter`) |
| `SavedChapterInfo` record | **Add** — response DTO returned by `POST /internal/comics`: `{Long chapterId, String chapterName, String originPathParams, String originUrl}`. `chapterName` maps to `ChapterPendingEvent.chapterNum` (both are the display label, e.g. `"Chapter 1"`). `comicSlug` for `ChapterPendingEvent` is sourced from `extractedComic.slug()` already in crawl-svc scope — not needed in the response. |
| `Comic` entity | Add `publishStatus` field (`PublishStatus`, default `DRAFT`). Add to builder, constructor, getter, setter. Note: entity uses manual builder (no Lombok `@Builder`) — all four must be updated. |

---

## Error Handling

| Scenario | Behavior |
|---|---|
| `core-service` down during `POST /internal/comics` | REST call throws → crawl-svc surfaces error to caller. No partial Comic state in DB. MinIO thumb already written — cleaned by `FileTempClearer` on retry timeout. |
| Duplicate comic | `core-svc` returns 409 → crawl-svc throws `ConflictException` (existing handling unchanged) |
| Transaction fails in core-svc | 500 returned → crawl-svc propagates. Comic not persisted. |
| `ChapterPendingEvent` publish fails (crawl-svc after REST success) | Comic and chapters persisted in DB with `CrawlStatus.INIT`. Manual re-trigger via `retryFailedChapters` endpoint when implemented. Currently a no-op — operator must delete comic and re-crawl as workaround. |
| `ChapterImagesExtractedEvent` consumer fails | Chapters remain `STARTED`; retry logic handles (unchanged). |
| `PATCH /internal/comics/{slug}/publish` — already PUBLISHED | Idempotent — returns `204 No Content` again. No error. |
| `PATCH /internal/comics/{slug}/publish` — slug not found | `core-svc` returns 404. Caller receives error. |

---

## Out of Scope (Future)

- Discord/Telegram admin notification when all chapters finish crawling.
- Bulk publish endpoint.
- User-facing "coming soon" state between `DRAFT` and `PUBLISHED`.
