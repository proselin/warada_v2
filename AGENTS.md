# AGENTS.md — warada_v2

This file provides context and conventions for AI agents working on this repository.

## What This Project Does

**warada_v2** is a Quarkus-based backend that crawls manga/comic content from NetTruyen (a Vietnamese comic site), stores metadata in PostgreSQL, and serves images via MinIO (S3-compatible object storage). It exposes a **GraphQL API** for browsing comics/chapters and a **REST API** for triggering crawl operations.

---

## Repository Layout

```
warada_v2/
├── src/
│   ├── apps/
│   │   ├── core-service/     # PostgreSQL persistence + Liquibase (port 8082)
│   │   ├── crawl-service/    # NetTruyen scraping + MinIO + RabbitMQ producers (port 8081)
│   │   └── display-service/  # GraphQL read API (port 8080)
│   └── libs/
│       └── shared-lib/       # Shared JPA entities, events, enums, exceptions, DTOs
├── legacy/src/               # Original monolith (reference only — do not modify)
├── docs/                     # Architecture docs, AI planning docs, API reference
├── .agents/skills/           # Reusable agent skill definitions
├── .github/
│   ├── copilot-instructions.md
│   └── prompts/              # Workflow prompt templates
├── build.gradle              # Root Gradle build (legacy monolith build — mostly unused)
├── settings.gradle           # Multi-project Gradle settings
└── docker-compose.yml        # Local infrastructure (PostgreSQL, RabbitMQ, MinIO)
```

### Gradle subprojects

| Subproject | Path | Description |
|---|---|---|
| `:shared-lib` | `src/libs/shared-lib` | Common entities, events, enums, exceptions, DTOs |
| `:crawl-service` | `src/apps/crawl-service` | Scraping, MinIO upload, RabbitMQ producer |
| `:core-service` | `src/apps/core-service` | Persistence, Liquibase migrations, RabbitMQ consumer |
| `:display-service` | `src/apps/display-service` | GraphQL read API, Panache repositories |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Quarkus 3 |
| Build | Gradle (`./gradlew`) |
| Database | PostgreSQL 16 + Hibernate ORM Panache + Liquibase |
| Object storage | MinIO (S3-compatible, bucket: `warada-images`) |
| Messaging | RabbitMQ + SmallRye Reactive Messaging |
| APIs | SmallRye GraphQL · JAX-RS REST · SSE |
| Code generation | Lombok (used selectively; JPA entities use manual getters/setters + Builder) |
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

# Build native image (requires Docker)
./gradlew :crawl-service:build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true

# Build all native images for production
./gradlew buildProdNative -Dquarkus.profile=prod
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
  │                                              │ scrapes NetTruyen HTML
  │                                              │ uploads thumbnail → MinIO
  │                                              └─[ComicExtractedEvent]──► core-service
  │                                                                              │ persists Comic + Chapters (INIT)
  │                                                                              └─[ChapterPendingEvent]──► crawl-service
  │                                                                                                              │ downloads images → MinIO
  │                                                                                                              └─[ChapterImagesExtractedEvent]──► core-service
  │                                                                                                                                                     └ marks Chapter FINISHED
  │
  ├─ GET  /graphql  ──────────────────────► display-service ──► PostgreSQL (read-only)
  │
  └─ GET  /api/v1/crawl/progress/{slug}  ──► crawl-service (in-memory broadcast)
```

---

## Code Conventions

### Naming

- Service/Repository pairs: interface + `Impl` suffix (`CrawlService` / `CrawlServiceImpl`)
- GraphQL resources: `*GqlResource`; REST resources: `*RestResource`
- JPA entities: singular PascalCase (`Comic`, `Chapter`); DB tables: plural lowercase (`comics`, `chapters`)
- DTOs: Java `record` types preferred for immutability
- Message event classes: `*Event` (e.g., `ComicExtractedEvent`, `ChapterPendingEvent`)

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
- Current GraphQL API is **read-only** (queries only — no mutations implemented)

```java
@GraphQLApi
public class ComicGqlResource {
    @Query("comics")
    @Blocking
    public List<Comic> comics(PageRequest pageRequest) { ... }
}
```

### Transactions

Place `@Transactional` on **service** methods that write to the database, not on repository methods.

### Exception Handling

Use custom exceptions from `shared-lib/exceptions/`:

| Exception | HTTP status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `ConflictException` | 409 |
| `BusinessException` | 400 |

`GlobalExceptionMapper` in display-service catches all unhandled exceptions and returns structured `ApiError` JSON.

### Configuration

- All services use `application.properties` (not `application.yaml`). Add new config keys under the appropriate Quarkus profile prefix (`%dev`, `%prod`).
- Access config via `@ConfigMapping` interfaces in `configurations/` (see `crawl-service/configurations/WaradaConfig.java`), not `@ConfigProperty` directly.

### Database Migrations

All schema changes go through Liquibase changesets in `core-service/src/main/resources/db/migration/`. Register new changesets in `db-changelog-master.yaml`. Only `core-service` runs migrations; `display-service` connects read-only.

---

## MinIO Image Storage Pattern

Images are written in two phases:

1. **Temp path** → `temp/<filename>`
2. **Permanent path** → `<comicSlug>/chapters/<chapterNum>/<filename>` (chapter images) or `<comicSlug>/thumb/<filename>` (thumbnails)

`FileTempClearer` (scheduler in crawl-service) purges temp objects older than `warada.clean-up.min-age-minutes` on a cron schedule.

---

## RabbitMQ Event Types

| Event | Publisher | Consumer | Purpose |
|---|---|---|---|
| `ComicExtractedEvent` | crawl-service | core-service | Comic scraped; persist and fan out chapters |
| `ChapterPendingEvent` | core-service | crawl-service | Chapter ready to be image-crawled |
| `ChapterImagesExtractedEvent` | crawl-service | core-service | Images downloaded; mark chapter FINISHED |

---

## Key Domain Classes

| Class | Module | Description |
|---|---|---|
| `Comic`, `Chapter`, `Image`, `Tag` | shared-lib | JPA entities |
| `CrawlStatus` | shared-lib | Enum: `INIT`, `STARTED`, `FINISHED` |
| `CrawlServiceImpl` | crawl-service | Orchestrates comic + chapter crawl pipeline |
| `NettruyenExtractorImpl` | crawl-service | Scrapes HTML from NetTruyen |
| `CoreServiceClient` | crawl-service | REST client to core-service internal API |
| `ComicExtractedConsumer` | core-service | Handles `ComicExtractedEvent` from RabbitMQ |
| `ChapterImagesExtractedConsumer` | core-service | Handles `ChapterImagesExtractedEvent` |
| `ChapterPendingProducer` | core-service | Publishes `ChapterPendingEvent` to RabbitMQ |
| `ComicGqlResource` | display-service | GraphQL queries for comics |
| `ChapterGqlResource` | display-service | GraphQL queries for chapters |
| `TagGqlResource` | display-service | GraphQL queries for tags |
| `CrawlRestResource` | crawl-service | REST endpoints for triggering crawls |
| `CrawlProgressBroadcaster` | crawl-service | In-memory SSE broadcast per comic slug |

---

## Where to Make Changes

| Task | Where |
|---|---|
| Add a new GraphQL query/mutation | `display-service/.../resources/` + service + repository |
| Add a new REST crawl endpoint | `crawl-service/.../resources/CrawlRestResource.java` |
| Add a new DB column | Liquibase changeset in `core-service/.../db/migration/` + update shared-lib entity |
| Add a new RabbitMQ event | shared-lib `models/events/` + producer in crawl/core + consumer in the other service |
| Add shared DTOs / exceptions | `shared-lib/` only |
| Change scraping logic | `crawl-service/.../services/NettruyenExtractorImpl.java` |
| Change MinIO storage structure | `crawl-service/.../services/CrawlServiceImpl.java` |
| Change crawl progress streaming | `crawl-service/.../services/CrawlProgressBroadcaster.java` |

---

## Testing

- Tests live in `src/test/java/` within each subproject.
- Quarkus `@QuarkusTest` is used for integration tests (spins up the full app with Testcontainers for PostgreSQL / RabbitMQ).
- Use `io.quarkus:quarkus-junit5-mockito` for mocking CDI beans in unit tests.
- Run tests: `./gradlew test` (root) or `./gradlew :display-service:test` (per service).

---

## Skills

Reusable agent skill definitions live in `.agents/skills/`. Use them for specialized tasks:

- **`java-quarkus-development`** — Quarkus/Java development guidance
- **`graphql-schema`** — GraphQL schema design
- **`graphql-operations`** — Writing GraphQL queries and mutations
- **`database-schema-design`** — DB schema and Liquibase migration design
- **`brainstorming`** — Architecture and feature brainstorming

See `.agents/skills/<skill-name>/SKILL.md` for each skill's instructions.
