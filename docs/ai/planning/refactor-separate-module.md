---
phase: planning
title: "Refactor: Separate Module Architecture — Migration Plan"
description: Phased plan for decomposing warada_v2 monolith into crawl-service, core-service, display-service, and shared-lib
---

# Planning: Refactor — Separate Module Architecture

## Milestones

- [ ] Milestone 1: **shared-lib** — Extract common entities, events, enums, exceptions into a standalone library
- [ ] Milestone 2: **core-service** — Move persistence logic; consume RabbitMQ events; expose internal REST
- [ ] Milestone 3: **crawl-service** — Move scraping, MinIO upload, SSE; publish/consume events
- [ ] Milestone 4: **display-service** — Move GraphQL API; read-only DB
- [ ] Milestone 5: **Integration & Verification** — All three services run together and pass end-to-end smoke tests
- [ ] Milestone 6: **Cleanup** — Remove old monolith source; update Docker Compose; update CI/CD

---

## Task Breakdown

### Phase 0: Preparation
- [ ] Task 0.1: Create `settings.gradle` with `include 'shared-lib', 'crawl-service', 'core-service', 'display-service'`
- [ ] Task 0.2: Move current monolith source into a `legacy/` directory (do not delete yet — reference during migration)
- [ ] Task 0.3: Create root `build.gradle` with shared Java/Lombok conventions

### Phase 1: shared-lib
- [ ] Task 1.1: Create `shared-lib/build.gradle` (java-library, no Quarkus plugin)
- [ ] Task 1.2: Move JPA entities (`Comic`, `Chapter`, `Image`, `Tag`) → `shared-lib`
- [ ] Task 1.3: Move enums (`CrawlStatus`, `ImageType`) and JPA converters → `shared-lib`
- [ ] Task 1.4: Move `PageRequest`, `PageResponse`, `ApiError`, `ApiResponse` → `shared-lib`
- [ ] Task 1.5: Move `BusinessException`, `ConflictException`, `ResourceNotFoundException` → `shared-lib`
- [ ] Task 1.6: Move `Utils.java` and `Constant.java` → `shared-lib`
- [ ] Task 1.7: Define new event records: `ComicExtractedEvent`, `ChapterPendingEvent`, `ChapterImagesExtractedEvent`
- [ ] Task 1.8: Verify shared-lib compiles independently (`./gradlew :shared-lib:build`)

### Phase 2: core-service
- [ ] Task 2.1: Create `core-service/build.gradle` with Quarkus, Panache, Liquibase, RabbitMQ, shared-lib deps
- [ ] Task 2.2: Copy Liquibase changelog files from monolith → `core-service/src/main/resources/db/`
- [ ] Task 2.3: Create `ComicExtractedConsumer` — consumes event, persists Comic + Tags + stub Chapters
- [ ] Task 2.4: Create `ChapterPendingProducer` — after comic saved, publish one `ChapterPendingEvent` per new chapter
- [ ] Task 2.5: Create `ChapterImagesExtractedConsumer` — persists Images, updates Chapter to `FINISHED`
- [ ] Task 2.6: Create `InternalResource` — `GET /internal/comics/exists?slug=` and `GET /internal/chapters/{id}`
- [ ] Task 2.7: Configure `application.yaml` for core-service (DB, RabbitMQ queues/exchanges)
- [ ] Task 2.8: Write unit tests for consumers (mock Panache repositories)
- [ ] Task 2.9: Verify `./gradlew :core-service:build`

### Phase 3: crawl-service
- [ ] Task 3.1: Create `crawl-service/build.gradle` with Quarkus, REST, MinIO, RabbitMQ, REST-client, shared-lib deps
- [ ] Task 3.2: Move `NettruyenExtractorImpl` + REST client `NettruyenRestService` → crawl-service
- [ ] Task 3.3: Move `CrawlServiceImpl` → crawl-service; replace direct repository calls with event publishing
- [ ] Task 3.4: Create `ComicCrawlProducer` — publishes `ComicExtractedEvent`
- [ ] Task 3.5: Create `ChapterPendingConsumer` — consumes `ChapterPendingEvent`, triggers image download
- [ ] Task 3.6: Create `ChapterImagesProducer` — publishes `ChapterImagesExtractedEvent`
- [ ] Task 3.7: Create `CoreServiceClient` (MicroProfile REST client) — calls core-service `/internal/comics/exists`
- [ ] Task 3.8: Move `CrawlRestResource` and SSE endpoint → crawl-service resources
- [ ] Task 3.9: Move `FileTempClearer` scheduler → crawl-service
- [ ] Task 3.10: Move `MinioBucketInitializer` → crawl-service
- [ ] Task 3.11: Configure `application.yaml` for crawl-service (MinIO, RabbitMQ, core-service REST client URL)
- [ ] Task 3.12: Write unit tests for extractor and crawl service (mock MinIO, mock producer)
- [ ] Task 3.13: Verify `./gradlew :crawl-service:build`

### Phase 4: display-service
- [ ] Task 4.1: Create `display-service/build.gradle` with Quarkus, SmallRye GraphQL, Panache, shared-lib deps
- [ ] Task 4.2: Move `ComicGqlResource`, `ChapterGqlResource`, `TagGqlResource`, `ImageGqlResource` → display-service
- [ ] Task 4.3: Move `ComicService`, `ChapterService`, `TagService` and their impls → display-service
- [ ] Task 4.4: Move `ComicRepository`, `ChapterRepository`, `ImageRepository`, `TagRepository` → display-service
- [ ] Task 4.5: Copy GraphQL schema files (`.graphqls`) → display-service resources
- [ ] Task 4.6: Move `GlobalExceptionMapper`, `ConflictExceptionMapper`, `ResourceNotFoundExceptionMapper` → display-service
- [ ] Task 4.7: Move `HealthCheckResource` → display-service
- [ ] Task 4.8: Configure `application.yaml` for display-service (DB read-only user, GraphQL settings)
- [ ] Task 4.9: Write unit tests for GQL resources (mock services)
- [ ] Task 4.10: Verify `./gradlew :display-service:build`

### Phase 5: Integration & Verification
- [ ] Task 5.1: Update `docker-compose.yml` to run all three services + RabbitMQ + PostgreSQL + MinIO
- [ ] Task 5.2: Add `healthcheck` to each service in Docker Compose
- [ ] Task 5.3: Run end-to-end smoke test: trigger comic crawl → verify GraphQL returns comic data
- [ ] Task 5.4: Verify SSE stream works across crawl-service
- [ ] Task 5.5: Verify DLQ receives events on intentional failure (integration test)

### Phase 6: Cleanup
- [ ] Task 6.1: Remove `legacy/` monolith source directory
- [ ] Task 6.2: Update `README.md` with new multi-module structure and run instructions
- [ ] Task 6.3: Update CI/CD pipeline to build and publish images for each service
- [ ] Task 6.4: Update `docs/ai/design/README.md` to point to new architecture

---

## Dependencies (Task Order)

```
Phase 0 (Prep)
    └── Phase 1 (shared-lib)
            ├── Phase 2 (core-service)
            ├── Phase 3 (crawl-service)
            └── Phase 4 (display-service)
                        └── Phase 5 (Integration)
                                    └── Phase 6 (Cleanup)
```

Phases 2, 3, and 4 can be worked on **in parallel** once `shared-lib` is stable.

---

## Migration Strategy: Keep Monolith Running During Migration

To avoid a big-bang cutover:

1. **Keep the current monolith in `src/` untouched** while developing new modules in parallel directories.
2. Build and test each service independently before attempting integration.
3. Once all three services pass integration tests, flip the Docker Compose to use new services.
4. Only then remove the old monolith source.

---

## Risks & Mitigation

| Risk | Impact | Mitigation |
|---|---|---|
| Entity scanning fails in shared-lib JAR | core/display services can't find JPA entities | Set `quarkus.hibernate-orm.packages=com.github.warada_v2.shared.entities` in each service config |
| SSE connection lost between crawl steps | User loses progress | Store progress in in-memory map keyed by request ID; reconnect support |
| core-service internal REST becomes bottleneck | Crawl-service blocked on dedup check | Add short-circuit cache in crawl-service (TTL: 5 min) for known slugs |
| RabbitMQ message ordering for chapter events | Chapters saved out of order | Use `position` field on Chapter entity; ordering is cosmetic only |
| Gradle multi-module build times increase | Slower dev loop | Use `./gradlew :crawl-service:quarkusDev` to run only the target service in dev mode |
| Liquibase migrations run in core-service only | display-service starts before schema is ready | Add readiness probe; display-service retries DB connection on startup |

---

## Docker Compose (Target State)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_PASSWORD: postgres

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"

  core-service:
    image: ghcr.io/proselin/warada_v2/core-service:latest
    ports:
      - "8082:8082"
    depends_on: [postgres, rabbitmq]

  crawl-service:
    image: ghcr.io/proselin/warada_v2/crawl-service:latest
    ports:
      - "8081:8081"
    depends_on: [minio, rabbitmq, core-service]

  display-service:
    image: ghcr.io/proselin/warada_v2/display-service:latest
    ports:
      - "8080:8080"
    depends_on: [postgres]
```
