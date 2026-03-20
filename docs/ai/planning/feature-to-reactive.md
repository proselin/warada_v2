# Feature: REST Blocking to Reactive — Planning

## Scope

Full end-to-end reactive stack for warada_v2 REST endpoints.  
**Phase 1** (completed): HTTP transport layer only.  
**Phase 2** (planned): Persistence layer — Hibernate Reactive.

---

## Approach Decision

### Option A — Resource-layer wrapping (Phase 1, DONE)
Wrap every service call in `Uni.createFrom().item(...).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`.

**Pros:** `@Transactional` works naturally; minimal code change.  
**Cons (identified in Phase 1 retrospective):**
- Worker pool (default 20 threads) exhausted under concurrent crawl load.
- SSE endpoint: service blocks event loop before returning `Multi` → `BlockedThreadChecker` error in dev.

### Option B — Full Hibernate Reactive (Phase 2, PLANNED)
Replace `quarkus-hibernate-orm-panache` + `quarkus-jdbc-postgresql` with `quarkus-hibernate-reactive-panache` + `quarkus-reactive-pg-client`. Service methods return `Uni<T>`. Resources call service directly without `runSubscriptionOn`.

**Pros:** True non-blocking I/O; no thread pool exhaustion; SSE event loop safe.  
**Cons:** Major change — all entities, repositories, and service methods must be migrated.

### Option C — SSE short-term fix only (interim)
Add `@Blocking` to the SSE REST resource method to stop dev-mode errors.  
Does not fix POST endpoint pool exhaustion. Use only as a temporary patch.

---

## Phase 1 Execution Plan (Completed)

### Phase 1.1 — Service interfaces
- [x] Remove `throws Exception` from `CrawlService` method signatures

### Phase 1.2 — Service impl cleanup
- [x] Remove `throws Exception` from `CrawlServiceImpl`
- [x] Wrap `catch (Exception e)` correctly

### Phase 1.3 — REST resources
- [x] `CrawlRestResource`: POST endpoints → `Uni<Response>`, SSE removes `@Blocking`
- [x] `ComicRestResource`: both endpoints → `Uni<Response>`
- [x] `ChapterRestResource`: both endpoints → `Uni<Response>`
- [x] `HealthCheckResource`: `ping()` → `Uni<String>`

### Phase 1.4 — Test updates
- [x] `CrawlRestResourceTest`: `uni.await().indefinitely()` pattern
- [x] `HealthCheckResourceTest`: `Uni.await()` pattern

---

## Phase 2 Execution Plan (Hibernate Reactive)

### Phase 2.0 — Interim SSE fix
- [ ] Add `@Blocking` to `CrawlRestResource#crawlChapterByComic` — stops dev-mode errors immediately

### Phase 2.1 — Dependency swap (`build.gradle`)
- [ ] Remove `quarkus-hibernate-orm-panache`
- [ ] Remove `quarkus-jdbc-postgresql`
- [ ] Add `quarkus-hibernate-reactive-panache`
- [ ] Add `quarkus-reactive-pg-client`
- [ ] Verify Liquibase still works (uses JDBC; keep `quarkus-jdbc-postgresql` for Liquibase only if needed)

### Phase 2.2 — Configuration (`application.yaml`)
- [ ] Replace `quarkus.datasource.jdbc.url` with `quarkus.datasource.reactive.url`
- [ ] Remove Agroal pool settings (replaced by reactive pool)

### Phase 2.3 — Entities
- [ ] Remove `import io.quarkus.hibernate.orm.panache.PanacheEntity`
- [ ] Add `import io.quarkus.hibernate.reactive.panache.PanacheEntity`
- [ ] No other entity changes required

### Phase 2.4 — Repositories
- [ ] All repositories: change `PanacheRepository<T>` import to Hibernate Reactive variant
- [ ] All custom query methods: return `Uni<T>` / `Uni<List<T>>` instead of `T` / `List<T>`

### Phase 2.5 — Services
- [ ] All service methods return `Uni<T>` / `Multi<T>`
- [ ] Replace `@Transactional` with `@ReactiveTransactional` (or `@WithTransaction` per-call)
- [ ] Chain DB calls with `.flatMap()` / `.chain()` using Mutiny API
- [ ] HTTP scraping (`NettruyenExtractor`) still blocks → keep `runSubscriptionOn(workerPool)` for that step only

### Phase 2.6 — REST resources
- [ ] Remove all `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` wrappers
- [ ] Resources call service directly: `return crawlService.crawlNettruyenComic(...)` → already `Uni<Response>`

### Phase 2.7 — Tests
- [ ] Unit tests for repositories: use `UniAssertSubscriber` or `Mutiny.await()`
- [ ] Unit tests for services: mock `Uni`-returning repository methods
- [ ] Integration tests: `@QuarkusTest` with reactive datasource config

---

## Risks

| Risk | Mitigation |
|------|-----------|
| Worker pool exhaustion (Phase 1 regression) | Phase 2 eliminates this |
| SSE `BlockedThreadChecker` error (Phase 1 regression) | Phase 2.0 `@Blocking` interim fix |
| Liquibase incompatible with reactive driver | Keep `quarkus-jdbc-postgresql` only for Liquibase profile; use reactive driver for app |
| `@Transactional` transaction scope lost in reactive chain | Use `@ReactiveTransactional`; verify with integration tests |
| Lazy-loaded Hibernate collections in reactive context | Eager-load all associations or use `fetch join` queries |

## Timeline

- Phase 2.0 (interim `@Blocking` fix): immediate, single-line change
- Phase 2.1–2.7 (full Hibernate Reactive): multi-day refactor, separate PR
