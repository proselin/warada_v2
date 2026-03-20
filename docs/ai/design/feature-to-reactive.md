# Feature: REST Blocking to Reactive — Design

## Phase 1 Architecture (Current — Partial Reactive)

```
HTTP Request
     │
     ▼
RESTEasy Reactive (event loop thread)
     │  returns Uni<Response>
     ▼
Uni.createFrom().item(...)
     │  subscribeOn
     ▼
Worker Thread Pool (Infrastructure.getDefaultWorkerPool())  ← BLOCKING THREAD HELD HERE
     │  blocking call, thread occupied for full DB duration
     ▼
Service (@Transactional, synchronous)
     │
     ▼
Hibernate ORM Panache (JDBC) → PostgreSQL   ← BLOCKING I/O
```

**Problem**: Only the HTTP layer is reactive. Every DB request still occupies a worker thread.
Default pool = 20 threads → 20 concurrent crawl requests saturate it.
SSE endpoint additionally blocks the **event loop** directly (see [Requirements](../requirements/feature-to-reactive.md)).

---

## Phase 2 Target Architecture (Full Reactive Stack)

```
HTTP Request
     │
     ▼
RESTEasy Reactive (event loop thread)
     │  returns Uni<Response> / Multi<T>
     ▼
Service (@WithTransaction / @ReactiveTransactional)
     │  returns Uni<T> — non-blocking
     ▼
Hibernate Reactive Panache (Mutiny API)
     │  non-blocking I/O (Vert.x PostgreSQL driver)
     ▼
PostgreSQL     (reactive-pg-client / Vert.x SQL driver)
```

No worker threads held for DB operations. The event loop handles everything end-to-end.
Worker thread pool is preserved for CPU-intensive work (image processing, MinIO uploads).

---

## Phase 2 Reactive Pattern — Services

```java
// Repository (Hibernate Reactive PanacheRepository)
@ApplicationScoped
public class ComicRepository implements PanacheRepository<Comic> {
    public Uni<Optional<Comic>> findBySlugAndComicId(String slugNId) {
        return find("slug = ?1", slugNId).firstResultOptional();
    }
}

// Service — all return Uni<T>, use @WithTransaction
@ApplicationScoped
public class CrawlServiceImpl implements CrawlService {

    @WithTransaction
    public Uni<Void> crawlNettruyenComic(String slugNId) {
        return comicRepository.existedBySlugAndComicId(slugNId)
            .flatMap(exists -> {
                if (exists) throw new ConflictException("...");
                return Uni.createFrom().item(() -> nettruyenExtractor.extractComicDetail(slugNId))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()) // HTTP scraping still blocking
                    .flatMap(extracted -> comicRepository.persistAndFlush(buildComic(extracted)));
            });
    }
}
```

---

## Phase 2 Pattern — REST Resources

```java
// No runSubscriptionOn needed — service returns Uni already
@POST
@Path("/nettruyen/comic")
public Uni<Response> crawlIndividualComic(CrawlNettruyenComicRequest request) {
    return crawlService.crawlNettruyenComic(request.slugNId())
        .replaceWith(Response.ok("Success").build());
}
```

---

## SSE Endpoint (Phase 2)

```java
@Override
public Multi<OutboundSseEvent> crawlNettruyenSSEChapter(String slugNId) {
    return comicRepository.findBySlugAndComicId(slugNId)  // returns Uni
        .onItem().transformToMulti(comic ->
            Multi.createFrom().iterable(comic.getChapters())
                .onItem().transformToUniAndConcatenate(chapter ->
                    crawlChapterReactive(chapter)   // returns Uni<OutboundSseEvent>
                )
        );
}
```

No synchronous code on the event loop; full pipeline is non-blocking.

---

## SSE Endpoint Short-Term Fix (until Phase 2)

Add `@Blocking` to the SSE REST resource method:

```java
@GET
@Path("/nettruyen/sse/comic")
@Blocking                   // ← forces execution onto a worker thread
@RestStreamElementType("application/json")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<OutboundSseEvent> crawlChapterByComic(@QueryParam("slug-n-id") String slugNId) {
    return crawlService.crawlNettruyenSSEChapter(slugNId);
}
```

This fixes the dev-mode `BlockedThreadChecker` error but does not fix pool exhaustion.

---

## Error Handling

In reactive pipelines, thrown exceptions inside `Uni`/`Multi` operators are converted to failure events.
The existing `GlobalExceptionMapper` (`ExceptionMapper<Throwable>`) catches all Uni failures — no change needed.

---

## Class Diagram (changed classes)

```
CrawlRestResource ──uses──► CrawlService (interface, no throws Exception)
                                  └── CrawlServiceImpl (@Transactional, synchronous)

ComicRestResource ──uses──► ComicService (interface, unchanged)
                                  └── ComicServiceImpl (@Transactional, synchronous)

ChapterRestResource ──uses──► ChapterService (interface, unchanged)
                                    └── ChapterServiceImpl (@Transactional, synchronous)

HealthCheckResource (Uni<String>, no deps)
```
