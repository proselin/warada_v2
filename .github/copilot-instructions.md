# Copilot Instructions for warada_v2

## What This Project Does

**warada_v2** is a Quarkus-based backend that crawls manga/comic content from NetTruyen (an external Vietnamese comic site), stores metadata in PostgreSQL, and serves images via MinIO (S3-compatible object storage). It exposes a **GraphQL API** for reading comics/chapters and a **REST API** for triggering crawl operations.

## Technology Stack

- **Framework**: Quarkus 3.39.3, Java 25
- **Build Tool**: Maven (use `./mvnw`)
- **Database**: PostgreSQL 16 + Hibernate ORM Panache + Liquibase migrations
- **Object Storage**: MinIO (S3-compatible, bucket: `warada-images`)
- **APIs**: SmallRye GraphQL (queries) + JAX-RS REST (crawl triggers) + SSE (streaming)
- **Observability**: OpenTelemetry (traces, metrics, logs)
- **Code generation**: Lombok

## Build & Run Commands

```bash
# Start infrastructure (PostgreSQL + MinIO)
docker compose up -d

# Dev mode with live reload (port 8080)
./mvnw quarkus:dev

# Run all tests
./mvnw test

# Run a single test
./mvnw -Dtest="com.github.warada_v2.ExampleResourceTest#testPingPong" test

# Build JVM JAR
./mvnw package

# Build native image (requires Docker)
./mvnw -Pnative -Dquarkus.native.container-build=true package

# Build native image for production
./mvnw -Pnative -Dquarkus.native.container-build=true package
```

**Dev endpoints** (after `quarkusDev`):
- App: `http://localhost:8080`
- GraphQL UI: `http://localhost:8080/q/graphql-ui`
- Dev UI: `http://localhost:8080/q/dev/`
- OpenAPI: `http://localhost:8080/q/openapi`
- Health: `http://localhost:8080/q/health`

**MinIO setup** (first-time): Open `http://localhost:9001`, login with `admin`/`admin123123123123`, create a bucket named `warada-images`.

## Architecture

```
REST /api/v1/crawl  ──┐
GraphQL /graphql    ──┤──► Services ──► Repositories ──► PostgreSQL
SSE /sse/comic      ──┘        │
                               └──► NettruyenExtractor ──► NetTruyen (external)
                               └──► MinIO (image storage)
```

- **`resources/`** — API layer: `*GqlResource` (GraphQL, read-only), `CrawlRestResource` (REST, crawl triggers)
- **`services/`** — Business logic: `CrawlServiceImpl` orchestrates the full crawl pipeline; `NettruyenExtractor` scrapes HTML
- **`repositories/`** — Data access via Panache (`ComicRepository`, `ChapterRepository`, `ImageRepository`, `TagRepository`)
- **`db_entities/`** — JPA entities: `Comic`, `Chapter`, `Image`, `Tag`
- **`models/`** — DTOs as Java records (`PageRequest`, `PageResponse`, `ExtractedComicDetail`, etc.)
- **`configurations/`** — Type-safe config via `@ConfigMapping` (`MinioConfig`, `WaradaConfig`)
- **`scheduler/`** — `FileTempClearer`: cron job that purges stale MinIO temp files
- **`rest/client/`** — MicroProfile REST Client: `NettruyenRestService` (external HTTP calls)

## Key Conventions

### Naming
- Service/Repository pairs: interface + `Impl` suffix (`ComicService` / `ComicServiceImpl`)
- GraphQL resources: `*GqlResource`; REST resources: `*RestResource`
- Entities: singular PascalCase (`Comic`); tables: plural lowercase (`comics`)
- DTOs: Java `record` types preferred for immutability

### Logging
Use `@JBossLog` (Lombok) on every class that logs. **Never use `System.out` or `e.printStackTrace()`.**
```java
@JBossLog
public class MyService {
    // use: log.infof(), log.debugf(), log.warnf(), log.errorf()
}
```

### Dependency Injection
Use Jakarta CDI annotations (`@Inject`, `@ApplicationScoped`, `@RequestScoped`). All services and repositories are CDI beans.

### GraphQL Resources
- Annotate with `@GraphQLApi`, `@Query`, `@Mutation`
- Add `@Blocking` on methods that perform JPA operations (they must not run on the event loop)
```java
@GraphQLApi
public class ComicGqlResource {
    @Query
    @Blocking
    public PageResponse<ComicGql> comics(PageRequest request) { ... }
}
```

### Transactions
Put `@Transactional` on service methods that write to the database, not on repository methods.

### Exception Handling
Use custom exceptions from `exceptions/`:
- `ResourceNotFoundException` → 404
- `ConflictException` → 409
- `BusinessException` → 400

`GlobalExceptionMapper` catches all unhandled exceptions and returns structured JSON (`ApiError`).

### Configuration
Add new config keys to `application.yaml` under the appropriate profile. Access config via `@ConfigMapping` interfaces in `configurations/`, not `@ConfigProperty` directly.

### Database Migrations
All schema changes go through Liquibase changesets in `src/main/resources/db/migration/`. Register new changesets in `db-changelog-master.yaml`.

## Crawl Workflow (Core Domain Logic)

1. **Comic crawl** (`CrawlServiceImpl.crawlNettruyenComic`):
   - Checks for duplicate by slug → throws `ConflictException` if exists
   - Calls `NettruyenExtractor` to scrape HTML (JSON-LD + regex)
   - Downloads thumbnail to MinIO temp path, then moves to permanent path
   - Persists `Comic` + thumbnail `Image` + stub `Chapter` entities (all `CrawlStatus.INIT`)

2. **Chapter crawl** (`CrawlServiceImpl.crawlChapter`):
   - Only processes chapters with `CrawlStatus.INIT`
   - Sets status to `STARTED` before processing (optimistic lock)
   - Extracts image URLs from HTML (`data-sv1`/`data-sv2` attributes)
   - Downloads images **in parallel** (`parallelStream`) → MinIO temp → permanent path
   - Sets `CrawlStatus.FINISHED` on success

3. **SSE streaming** (`CrawlServiceImpl.crawlNettruyenSSEChapter`):
   - Returns a Mutiny `Multi` stream; each chapter emits an SSE event (`ADD` or `IGNORE`)

## MinIO Image Storage Pattern

Images follow a two-phase write: temp path → permanent path.
- Temp path format: `temp/<filename>`
- Permanent path format: `<comicSlug>/chapters/<chapterNum>/<filename>` (chapter images) or `<comicSlug>/thumb/<filename>` (thumbnails)
- `FileTempClearer` cleans up temp objects older than `warada.clean-up.min-age-minutes` on a cron schedule.

## Quarkus Profiles

| Profile | Purpose |
|---------|---------|
| `dev` (default) | Local development, `quarkusDev` |
| `prod` | Production native image build |
| `test-native` | Native integration testing |

Config is in `src/main/resources/application.yaml` using `%dev`, `%prod`, `%test-native` profile prefixes.


Using skills is in `.agents/skills/*`