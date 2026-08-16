# AGENTS.md — warada_v2

This file provides context and conventions for AI agents working on this repository.

## What This Project Does

**warada_v2** is a Quarkus-based backend that crawls manga/comic content from NetTruyen (a Vietnamese comic site), stores metadata in PostgreSQL, and serves images via MinIO (S3-compatible object storage). It exposes a **GraphQL API** for browsing comics/chapters (read-only) and a **REST API** for triggering crawl operations.

---

## Repository Layout

```
warada_v2/
├── src/
│   ├── apps/
│   │   ├── core-service/     # PostgreSQL persistence + Liquibase (port 8082)
│   │   ├── crawl-service/    # NetTruyen scraping + MinIO + RabbitMQ (port 8081)
│   │   └── display-service/  # GraphQL read API (port 8080)
│   └── libs/
│       └── shared-lib/       # Shared JPA entities, events, enums, exceptions, DTOs
├── scripts/                  # Per-service build-native.sh and build-and-push.sh scripts
├── .devcontainer/            # VS Code / JetBrains devcontainer (docker-compose + env vars)
├── .github/
│   ├── copilot-instructions.md
│   ├── workflows/build-native.yml  # CI: builds and pushes native images to GHCR on main
│   └── prompts/              # Workflow prompt templates
├── .agents/skills/           # Reusable agent skill definitions
├── build.gradle              # Root Gradle build (defines buildProdNative task)
├── settings.gradle           # Multi-project Gradle settings
└── docker-compose.yml        # Local infrastructure only (PostgreSQL, RabbitMQ, MinIO)
```

### Gradle subprojects

| Subproject | Path | Description |
|---|---|---|
| `:shared-lib` | `src/libs/shared-lib` | Common entities, events, enums, exceptions, DTOs |
| `:crawl-service` | `src/apps/crawl-service` | Scraping, MinIO upload, RabbitMQ producer/consumer |
| `:core-service` | `src/apps/core-service` | Persistence, Liquibase migrations, RabbitMQ consumer/producer |
| `:display-service` | `src/apps/display-service` | GraphQL read API, Panache repositories |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Quarkus 3.28.1 |
| Build | Gradle (`./gradlew`) |
| Database | PostgreSQL 16 + Hibernate ORM Panache + Liquibase |
| Object storage | MinIO (S3-compatible, bucket: `warada-images`) |
| Messaging | RabbitMQ 4 + SmallRye Reactive Messaging |
| APIs | SmallRye GraphQL (read-only) · JAX-RS REST · SSE |
| Code generation | Lombok (compile-only on shared-lib; entities use manual getters/setters + Builder) |
| Observability | OpenTelemetry |

---

## Build & Run Commands

```bash
# Start infrastructure (PostgreSQL, RabbitMQ, MinIO)
docker compose up -d postgres rabbitmq minio

# Dev mode with live reload — run each service separately
./gradlew :core-service:quarkusDev     # http://localhost:8082
./gradlew :crawl-service:quarkusDev   # http://localhost:8081
./gradlew :display-service:quarkusDev # http://localhost:8080

# Run all tests
./gradlew test

# Run tests for a specific subproject
./gradlew :crawl-service:test

# Build all modules (skip tests)
./gradlew build -x test

# Build a specific service
./gradlew :crawl-service:build -x test

# Build native images for all services (production, requires Docker)
./gradlew buildProdNative -Dquarkus.profile=prod

# Build and push a single service native image to GHCR
./scripts/crawl-service/build-and-push.sh <version>

# Build and push all services
./scripts/build-all-native.sh <version>
```

### Dev endpoints

| URL | Service | Description |
|---|---|---|
| `http://localhost:8080/graphql` | display-service | GraphQL API |
| `http://localhost:8080/q/graphql-ui` | display-service | GraphQL UI (dev only) |
| `http://localhost:8081/api/v1/crawl/nettruyen/comic` | crawl-service | Trigger comic crawl (POST) |
| `http://localhost:8081/api/v1/crawl/nettruyen/comic/{slug}/retry` | crawl-service | Retry pending chapters (POST) |
| `http://localhost:8081/api/v1/crawl/progress/{slug}` | crawl-service | SSE crawl progress (GET) |
| `http://localhost:8081/q/health` | crawl-service | Health check |
| `http://localhost:8082/q/health` | core-service | Health check |
| `http://localhost:15672` | RabbitMQ | Management UI (guest/guest) |
| `http://localhost:9001` | MinIO | Console (admin/admin123123123123) |

---

## Architecture & Event Flow

```
Client
  │
  ├─ POST /api/v1/crawl/nettruyen/comic  ──► crawl-service
  │                                              │ 1. scrapes NetTruyen HTML (JSON-LD + regex)
  │                                              │ 2. downloads thumbnail → MinIO temp → permanent
  │                                              └─[ComicExtractedEvent]──► core-service
  │                                                                              │ persists Comic + Chapters (INIT)
  │                                                                              └─[ChapterPendingEvent × N]──► crawl-service
  │                                                                                                                  │ downloads chapter images → MinIO
  │                                                                                                                  └─[ChapterImagesExtractedEvent]──► core-service
  │                                                                                                                                                         └ persists Images, marks Chapter FINISHED
  │
  ├─ GET  /graphql  ────────────────────────► display-service ──► PostgreSQL (read-only)
  │
  └─ GET  /api/v1/crawl/progress/{slug}  ──► crawl-service (in-memory SSE broadcast)
```

---

## Code Conventions

### Naming

- Service/Repository pairs: interface + `Impl` suffix (`CrawlService` / `CrawlServiceImpl`)
- GraphQL resources: `*GqlResource`; REST resources: `*RestResource`
- JPA entities: singular PascalCase (`Comic`, `Chapter`); DB tables: plural lowercase (`comics`, `chapters`)
- DTOs and event classes: Java `record` types
- RabbitMQ event classes: `*Event` suffix (e.g., `ComicExtractedEvent`, `ChapterPendingEvent`)

### Logging

Use `private static final Logger log = Logger.getLogger(ClassName.class)` (JBoss Logger) in every class that logs. **Never use `System.out.println()` or `e.printStackTrace()`.**

```java
import org.jboss.logging.Logger;

public class MyService {
    private static final Logger log = Logger.getLogger(MyService.class);
    // log.infof(), log.debugf(), log.warnf(), log.errorf()
}
```

### Dependency Injection

Use Jakarta CDI annotations (`@Inject`, `@ApplicationScoped`, `@RequestScoped`). All services, repositories, and resources are CDI beans.

### GraphQL Resources

- Annotate with `@GraphQLApi` and `@Query`
- Add `@Blocking` on methods that perform JPA operations (they must not run on the Vert.x event loop)
- The GraphQL API is **read-only** (queries only — all `Mutation` blocks are empty placeholders)

```java
@GraphQLApi
public class ComicGqlResource {
    @Query("comics")
    @Blocking
    public List<Comic> comics(PageRequest pageRequest) { ... }
}
```

### Transactions

Place `@Transactional` on **service** methods that write to the database, not on repository methods. For cross-method transaction boundaries (e.g., in consumers), use `QuarkusTransaction.requiringNew().call(...)`.

### Exception Handling

Use custom exceptions from `shared-lib/exceptions/`:

| Exception | HTTP status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `ConflictException` | 409 |
| `BusinessException` | 400 |

`GlobalExceptionMapper` in display-service catches all unhandled `Throwable` and returns structured `ApiError` JSON (HTTP 500).

### Configuration

- All services use `application.properties` (not `application.yaml`). Add new config keys under the appropriate Quarkus profile prefix (`%dev`, `%prod`).
- Access config via `@ConfigMapping` interfaces in `configurations/` (see `crawl-service/configurations/WaradaConfig.java`), not `@ConfigProperty` directly.

### Database Migrations

All schema changes go through Liquibase changesets in `core-service/src/main/resources/db/migration/`. Register new changesets in `db-changelog-master.yaml`. Only `core-service` runs migrations; `display-service` connects read-only (no Liquibase extension).

---

## MinIO Image Storage Pattern

Images are written in two phases:

1. **Temp path** → `temp/<filename>`
2. **Permanent path** → `<comicSlug>/chapters/<chapterNum>/<filename>` (chapter images) or `<comicSlug>/thumb/<filename>` (thumbnails)

The move is implemented as a MinIO copy + delete in `CrawlServiceImpl.renameObject()`. `FileTempClearer` (scheduler in crawl-service) purges temp objects older than `warada.clean-up.min-age-minutes` on a cron schedule (`0 0 0,7,14,19 * * ?`).

---

## RabbitMQ Event Types

| Event | Exchange | Publisher | Consumer | Purpose |
|---|---|---|---|---|
| `ComicExtractedEvent` | `comic.extracted.exchange` | crawl-service | core-service | Comic scraped; persist and fan out chapters |
| `ChapterPendingEvent` | `chapter.pending.exchange` | core-service | crawl-service | Chapter ready to be image-crawled |
| `ChapterImagesExtractedEvent` | `chapter.images.extracted.exchange` | crawl-service | core-service | Images downloaded; mark chapter FINISHED |

All exchanges are `direct` type, durable, with dead-letter queues auto-bound on failure.

---

## Key Domain Classes

| Class | Module | Description |
|---|---|---|
| `Comic`, `Chapter`, `Image`, `Tag` | shared-lib | JPA entities (manual getters/setters + Builder) |
| `CrawlStatus` | shared-lib | Enum: `INIT`, `STARTED`, `FINISHED` |
| `ImageType` | shared-lib | Enum: `THUMB_IMAGE`, `CHAPTER_IMAGE` |
| `ComicExtractedEvent`, `ChapterPendingEvent`, `ChapterImagesExtractedEvent` | shared-lib | RabbitMQ message records |
| `CrawlServiceImpl` | crawl-service | Orchestrates comic crawl; `crawlChapterFromEvent` handles image download |
| `NettruyenExtractorImpl` | crawl-service | Scrapes HTML from NetTruyen (JSON-LD + regex for images) |
| `ChapterPendingConsumer` | crawl-service | Consumes `ChapterPendingEvent`; calls `crawlChapterFromEvent` |
| `ComicExtractedProducer`, `ChapterImagesExtractedProducer` | crawl-service | Publish events to RabbitMQ |
| `CoreServiceClient` | crawl-service | MicroProfile REST Client to `core-service /internal` |
| `ComicExtractedConsumer` | core-service | Persists Comic + Chapters, publishes `ChapterPendingEvent` per chapter |
| `ChapterImagesExtractedConsumer` | core-service | Persists Images, marks Chapter `FINISHED` |
| `ChapterPendingProducer` | core-service | Publishes `ChapterPendingEvent` to RabbitMQ |
| `InternalResource` | core-service | Internal REST endpoints (comic exists check, chapter lookup) |
| `CrawlProgressBroadcaster` | crawl-service | In-memory SSE emitter map keyed by comic slug |
| `FileTempClearer` | crawl-service | Cron scheduler to purge stale MinIO temp objects |
| `ComicGqlResource`, `ChapterGqlResource`, `TagGqlResource` | display-service | GraphQL query endpoints |
| `CrawlRestResource` | crawl-service | REST endpoints: comic crawl, retry, SSE progress |
| `WaradaConfig` | crawl-service | `@ConfigMapping` for `warada.*` config properties |

---

## Where to Make Changes

| Task | Where |
|---|---|
| Add a new GraphQL query | `display-service/.../resources/*GqlResource.java` + service + repository + GraphQL schema file in `graphql/` |
| Add a new REST crawl endpoint | `crawl-service/.../resources/CrawlRestResource.java` |
| Add a new DB column | Liquibase changeset in `core-service/.../db/migration/` + update shared-lib entity |
| Add a new RabbitMQ event | `shared-lib/models/events/` + producer in publisher service + consumer in receiver service + `application.properties` channel config |
| Add shared DTOs / exceptions | `shared-lib/` only |
| Change scraping logic | `crawl-service/.../services/NettruyenExtractorImpl.java` |
| Change MinIO storage paths | `crawl-service/.../services/CrawlServiceImpl.java` (`renameObject` calls) |
| Change crawl progress streaming | `crawl-service/.../services/CrawlProgressBroadcaster.java` |
| Change DB schema | New Liquibase changeset in `core-service/src/main/resources/db/migration/` |

---

## Testing

- Tests live in `src/test/java/` within each subproject.
- Quarkus `@QuarkusTest` is used for integration tests (spins up the full app with Testcontainers for PostgreSQL / RabbitMQ).
- Use `io.quarkus:quarkus-junit5-mockito` for mocking CDI beans in unit tests.
- Run tests: `./gradlew test` (all) or `./gradlew :display-service:test` (per service).

---

## CI / Deployment

- **CI workflow**: `.github/workflows/build-native.yml` — on every push to `main` (or manual dispatch), builds native images for all three services using the Mandrel builder and pushes to `ghcr.io/proselin/warada_v2/<service>:<sha>-native-micro`.
- **Manual builds**: `./scripts/build-all-native.sh <version>` or per-service `./scripts/<service>/build-and-push.sh <version>`.
- **Docker image registry**: `ghcr.io/proselin/warada_v2/{core-service,crawl-service,display-service}`.

---

## Skills

Reusable agent skill definitions live in `.agents/skills/`. Use them for specialized tasks:

- **`java-quarkus-development`** — Quarkus/Java development guidance
- **`graphql-schema`** — GraphQL schema design
- **`graphql-operations`** — Writing GraphQL queries and mutations
- **`database-schema-design`** — DB schema and Liquibase migration design
- **`brainstorming`** — Architecture and feature brainstorming

See `.agents/skills/<skill-name>/SKILL.md` for each skill's instructions.
