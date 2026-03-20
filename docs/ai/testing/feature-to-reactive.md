# Feature: REST Blocking to Reactive — Testing

## Phase 1 Test Strategy

Because the REST resources now return `Uni<Response>`, unit tests must await the `Uni` to obtain the underlying `Response` value. Integration tests (`@QuarkusTest`) are unaffected since they call endpoints via HTTP (REST-Assured) and never directly see the `Uni` wrapper.

---

## Unit Test Changes (Phase 1)

### `CrawlRestResourceTest`

Plain JUnit 5 + Mockito test (no `@QuarkusTest`, no Docker required).

**Key pattern:**
```java
Uni<Response> uni = crawlRestResource.crawlIndividualComic(request);
Response response = uni.await().indefinitely();
assertEquals(200, response.getStatus());
```

- `Uni.await().indefinitely()` subscribes and blocks the calling test thread until the result is available.
- The `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` inside the resource will dispatch to a worker thread, which is safe in unit-test context.

### `HealthCheckResourceTest`

Plain JUnit 5 test.

```java
String result = resource.ping().await().indefinitely();
assertEquals("pong", result);
```

`Uni.createFrom().item("pong")` resolves immediately without scheduling; `await().indefinitely()` returns synchronously.

---

## Unchanged Tests (Phase 1)

| Test Class | Reason unchanged |
|-----------|-----------------|
| `CrawlServiceImplTest` | Tests `CrawlServiceImpl` directly; service methods remain `void`, no Uni |
| `ComicServiceImplTest` | Tests `ComicServiceImpl` directly; service methods remain synchronous |
| `ChapterServiceImplTest` | Tests `ChapterServiceImpl` directly; service methods remain synchronous |
| `ComicGqlResourceTest` | GraphQL resource unchanged |
| `ChapterGqlResourceTest` | GraphQL resource unchanged |
| `TagGqlResourceTest` | GraphQL resource unchanged |

---

## Phase 1 Validation Checklist

- [x] `./gradlew compileJava` → `BUILD SUCCESSFUL`
- [x] `./gradlew compileTestJava` → `BUILD SUCCESSFUL`
- [x] `CrawlRestResourceTest` — all three test methods pass with Uni awaiting
- [x] `HealthCheckResourceTest#testPing` — passes with Uni result unwrap
- [ ] `@QuarkusTest` integration tests — require Docker + running PostgreSQL + MinIO (pre-existing infra requirement, not introduced by this change)

---

## Phase 2 Test Strategy (Hibernate Reactive)

Hibernate Reactive services return `Uni<T>` — tests must mock `Uni`-returning methods and await results.

### Repository unit tests (Phase 2)

```java
// Mock repository returning Uni
when(comicRepository.findBySlugAndComicId("slug-123"))
    .thenReturn(Uni.createFrom().item(Optional.of(mockComic)));

// Assert via await
Uni<Void> result = crawlService.crawlNettruyenComic("slug-123");
result.await().indefinitely();   // asserts no exception
verify(comicRepository).persistAndFlush(any(Comic.class));
```

### Service unit tests (Phase 2) — `UniAssertSubscriber` pattern

```java
crawlService.crawlNettruyenComic("slug-123")
    .subscribe().withSubscriber(UniAssertSubscriber.create())
    .awaitItem()
    .assertCompleted();
```

### Integration tests (Phase 2) — `@QuarkusTest` with reactive datasource

```java
@QuarkusTest
class CrawlServiceIntegrationTest {
    @Inject CrawlService crawlService;

    @Test
    void testCrawlComic() {
        crawlService.crawlNettruyenComic("test-slug-123")
            .await().indefinitely();
        // assert via repository
    }
}
```

`@QuarkusTest` auto-configures Dev Services (Testcontainers) for PostgreSQL when `quarkus-reactive-pg-client` is present — no manual Docker setup needed for CI.

---

## Known Limitations

- VS Code's built-in test runner may show `NoClassDefFoundError` due to stale cached `.class` files. Run `./gradlew test` for authoritative results.
- Phase 1 `@QuarkusTest` tests that hit the database require `docker compose up -d` first.
- Phase 2 `@QuarkusTest` tests will use Testcontainers Dev Services for PostgreSQL automatically.
