# Feature: REST Blocking to Reactive — Implementation

## Changed Files

### Service Interface

**`src/main/java/.../services/CrawlService.java`**

Removed `throws Exception` from all three method declarations. All runtime exceptions are `RuntimeException` subclasses; the checked declaration was unnecessary.

```java
// Before
void crawlNettruyenComic(String slugNId) throws Exception;
void crawlNettruyenChapter(Long chapterId) throws Exception;
Multi<OutboundSseEvent> crawlNettruyenSSEChapter(String slugNId) throws Exception;

// After
void crawlNettruyenComic(String slugNId);
void crawlNettruyenChapter(Long chapterId);
Multi<OutboundSseEvent> crawlNettruyenSSEChapter(String slugNId);
```

---

### Service Implementation

**`src/main/java/.../services/CrawlServiceImpl.java`**

- Removed `throws Exception` from `crawlNettruyenComic` method signature.
- Split `catch (Exception e) { throw e; }` into a proper two-branch catch to avoid unchecked throw of a checked `Exception` type:

```java
} catch (RuntimeException e) {
    log.error("ERROR Crawling Nettruyen Comic from " + slugNId);
    throw e;
} catch (Exception e) {
    log.error("ERROR Crawling Nettruyen Comic from " + slugNId);
    throw new RuntimeException(e);
}
```

---

### REST Resources

**`CrawlRestResource`**

| Method | Before | After |
|--------|--------|-------|
| `crawlIndividualComic` | `Response` + `throws Exception` | `Uni<Response>` + `runSubscriptionOn(workerPool)` |
| `crawlChapter` | `Response` + `throws Exception` | `Uni<Response>` + `runSubscriptionOn(workerPool)` |
| `crawlChapterByComic` | `Multi` + `@Blocking` + `throws Exception` | `Multi` (no `@Blocking`, no throws) |

Removed import: `io.smallrye.common.annotation.Blocking`  
Added imports: `io.smallrye.mutiny.Uni`, `io.smallrye.mutiny.infrastructure.Infrastructure`

**`ComicRestResource`**

| Method | Before | After |
|--------|--------|-------|
| `createComic` | `Response` | `Uni<Response>` + `runSubscriptionOn(workerPool)` |
| `deleteComic` | `Response` | `Uni<Response>` + `runSubscriptionOn(workerPool)` |

**`ChapterRestResource`**

| Method | Before | After |
|--------|--------|-------|
| `createChapter` | `Response` | `Uni<Response>` + `runSubscriptionOn(workerPool)` |
| `deleteChapter` | `Response` | `Uni<Response>` + `runSubscriptionOn(workerPool)` |

**`HealthCheckResource`**

| Method | Before | After |
|--------|--------|-------|
| `ping` | `String` | `Uni<String>` (no worker pool needed — non-blocking) |

---

### Tests

**`CrawlRestResourceTest`**

Updated two test methods to await the `Uni<Response>` result:
```java
// Before
Response response = crawlRestResource.crawlIndividualComic(request);

// After
Uni<Response> uni = crawlRestResource.crawlIndividualComic(request);
Response response = uni.await().indefinitely();
```
Removed `throws Exception` from test method signatures (no longer declared on service interface).

**`HealthCheckResourceTest`**

```java
// Before
assertEquals("pong", resource.ping());

// After
String result = resource.ping().await().indefinitely();
assertEquals("pong", result);
```

---

## Key Libraries

| Library | Purpose |
|---------|---------|
| `io.smallrye.mutiny.Uni` | Reactive single-value type (replaces synchronous `T`) |
| `io.smallrye.mutiny.infrastructure.Infrastructure` | Provides `getDefaultWorkerPool()` executor for blocking offload |
| `io.smallrye.mutiny.Multi` | Reactive multi-value stream (SSE, already in use) |

---

## Phase 1 Known Issues (Post-Implementation Review)

### Issue 1 — SSE endpoint blocks event loop (dev-breaking)

**Location**: `CrawlRestResource#crawlChapterByComic` → `CrawlServiceImpl#crawlNettruyenSSEChapter`

**Problem**: The service method performs two synchronous Hibernate calls before returning the `Multi`.
The REST resource is reactive and has no `@Blocking` / `runSubscriptionOn` guard on the method itself.
These blocking calls run on the Vert.x event loop thread → `BlockedThreadChecker` fires in dev mode.

```java
// PROBLEM in CrawlServiceImpl:
public Multi<OutboundSseEvent> crawlNettruyenSSEChapter(String slugNId) {
    // These two lines run on the event loop thread when called from reactive REST resource:
    Comic comic = comicRepository.findBySlugAndComicId(slugNId).orElse(null);  // ← blocking!
    ...
    return Multi.createFrom().iterable(comic.getChapters())  // ← blocking lazy-load!
        ...
        .runSubscriptionOn(Infrastructure.getDefaultExecutor());  // too late — sync init already done
}
```

**Immediate fix**: Add `@Blocking` to the SSE endpoint in `CrawlRestResource`.

```java
// FIX:
@GET
@Path("/nettruyen/sse/comic")
@Blocking                   // ← add this
@RestStreamElementType("application/json")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<OutboundSseEvent> crawlChapterByComic(@QueryParam("slug-n-id") String slugNId) {
    return crawlService.crawlNettruyenSSEChapter(slugNId);
}
```

**Permanent fix**: Phase 2 — Hibernate Reactive makes all DB calls return `Uni`, no sync blocking.

---

### Issue 2 — Worker thread pool exhaustion under concurrent crawl load

**Location**: All POST endpoints in `CrawlRestResource`, `ComicRestResource`, `ChapterRestResource`

**Problem**: `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` holds a Vert.x worker thread for the full duration of each crawl request (seconds). Default pool = 20 threads.

```
20 concurrent crawl requests → pool saturated → new requests queue → apparent hang
```

Quarkus exposes this symptom as very high `http.server.request.duration` under load, and eventual request timeouts.

**Tuning (temporary)**: Increase worker pool in `application.yaml`:
```yaml
quarkus:
  vertx:
    worker-pool-size: 40   # increase from default 20
```

**Permanent fix**: Phase 2 — Hibernate Reactive; no worker threads held for DB operations.

---

## Phase 2 Implementation Notes (Planned)

### `build.gradle` changes

```groovy
// REMOVE:
implementation 'io.quarkus:quarkus-hibernate-orm-panache'
implementation 'io.quarkus:quarkus-jdbc-postgresql'

// ADD:
implementation 'io.quarkus:quarkus-hibernate-reactive-panache'
implementation 'io.quarkus:quarkus-reactive-pg-client'

// KEEP for Liquibase migrations only:
implementation 'io.quarkus:quarkus-jdbc-postgresql'
```

### Repository pattern change

```java
// Phase 1 (Hibernate ORM):
@ApplicationScoped
public class ComicRepository implements PanacheRepository<Comic> {
    public Optional<Comic> findBySlugAndComicId(String slugNId) { ... }
}

// Phase 2 (Hibernate Reactive):
@ApplicationScoped
public class ComicRepository implements io.quarkus.hibernate.reactive.panache.PanacheRepository<Comic> {
    public Uni<Optional<Comic>> findBySlugAndComicId(String slugNId) {
        return find("slug = ?1", slugNId).firstResultOptional();
    }
}
```

### Service pattern change

```java
// Phase 1:
@Transactional
public void crawlNettruyenComic(String slugNId) { ... }

// Phase 2:
@ReactiveTransactional
public Uni<Void> crawlNettruyenComic(String slugNId) {
    return comicRepository.existedBySlugAndComicId(slugNId)
        .flatMap(exists -> { ... });
}
```

### REST resource change

```java
// Phase 1 (still needs runSubscriptionOn):
public Uni<Response> crawlIndividualComic(...) {
    return Uni.createFrom().item(() -> {
        crawlService.crawlNettruyenComic(request.slugNId());
        return Response.ok("Success").build();
    }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
}

// Phase 2 (service already returns Uni — no wrapping needed):
public Uni<Response> crawlIndividualComic(...) {
    return crawlService.crawlNettruyenComic(request.slugNId())
        .replaceWith(Response.ok("Success").build());
}
```
