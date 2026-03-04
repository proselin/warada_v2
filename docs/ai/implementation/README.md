---
phase: implementation
title: Implementation Guide
description: Technical implementation notes, patterns, and code guidelines
---

# Implementation Guide

## Development Setup
**How do we get started?**

### Prerequisites
- **Java 21** (GraalVM recommended for native builds)
- **Gradle** (wrapper included — use `./gradlew`)
- **Docker** & **Docker Compose** (for PostgreSQL, MinIO, and OTel Collector)
- **IDE:** IntelliJ IDEA recommended (with Lombok plugin)

### Environment Setup

1. **Start infrastructure services:**
   ```bash
   docker compose up -d
   ```
   This starts:
   - PostgreSQL 16 on port `5432` (user: `postgres`, password: `postgres`)
   - MinIO on port `9000` (API) and `9001` (Console)

2. **Create the MinIO bucket:**
   - Open MinIO Console at `http://localhost:9001`
   - Login with credentials from `compose.yaml` (default: `admin` / `admin123123123123`)
   - Create bucket named `warada-images`

3. **Run in dev mode:**
   ```bash
   ./gradlew quarkusDev
   ```
   The application starts at `http://localhost:8080` with:
   - Dev UI: `http://localhost:8080/q/dev/`
   - GraphQL UI: `http://localhost:8080/q/graphql-ui`
   - OpenAPI: `http://localhost:8080/q/openapi`
   - Health: `http://localhost:8080/q/health`

### Configuration
Key configuration in `src/main/resources/application.yaml`:

| Config Key | Description | Dev Default |
|---|---|---|
| `quarkus.datasource.jdbc.url` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/postgres` |
| `minio.endpoint` | MinIO API endpoint | `http://localhost:9000` |
| `minio.access-key` / `minio.secret-key` | MinIO credentials | Set in application.yaml |
| `warada.image-bucket` | MinIO bucket name | `warada-images` |
| `warada.nettruyen.url` | NetTruyen base URL | `https://nettruyenar.com` |
| `warada.clean-up.enabled` | Enable temp file cleanup | `true` |
| `warada.clean-up.min-age-minutes` | Min age before cleanup | `5` |

## Code Structure
**How is the code organized?**

### Directory Layout
```
src/main/
├── docker/                          # Dockerfiles (JVM, native, native-micro)
├── java/com/github/warada_v2/
│   ├── Application.java             # @QuarkusMain entry point
│   ├── commons/                     # Shared utilities, constants, enums
│   ├── configurations/              # CDI config mappings (@ConfigMapping)
│   ├── converter/                   # JPA AttributeConverters for enums
│   ├── db_entities/                 # JPA @Entity classes (Comic, Chapter, Image)
│   ├── exceptions/                  # Custom exceptions & JAX-RS mappers
│   ├── mapper/                      # Global exception mapper
│   ├── models/                      # DTOs, request/response records
│   ├── repositories/                # Panache repository interfaces + implementations
│   ├── resources/                   # API endpoints (GraphQL + REST)
│   ├── rest/client/                 # MicroProfile REST Client interfaces
│   ├── scheduler/                   # Scheduled jobs (temp file cleanup)
│   └── services/                    # Business logic layer
└── resources/
    ├── application.yaml             # Multi-profile Quarkus configuration
    ├── schema.graphql               # Root GraphQL schema
    ├── graphql/                     # Type-specific GraphQL schemas
    │   ├── comic.graphql
    │   ├── chapter.graphql
    │   └── image.graphql
    └── db/migration/                # Liquibase changesets
        ├── db-changelog-master.yaml
        ├── initialize-270925.sql
        └── remove-unique-260226.sql
```

### Naming Conventions
- **Entities:** Singular noun in PascalCase (`Comic`, `Chapter`, `Image`)
- **Tables:** Plural lowercase (`comics`, `chapters`, `images`)
- **Services:** Interface + `Impl` suffix (`ComicService` / `ComicServiceImpl`)
- **Repositories:** Interface + `Impl` suffix (`ComicRepository` / `ComicRepositoryImpl`)
- **Resources:** Suffix with `GqlResource` (GraphQL) or `RestResource` (REST)
- **Models/DTOs:** Descriptive name, Java records preferred (`ChapterDetailGql`, `PageRequest`)

## Implementation Notes
**Key technical details to remember:**

### Core Features

#### 1. Comic Crawling (`CrawlServiceImpl.crawlNettruyenComic`)
- Checks for duplicate comics by slug (throws `ConflictException` if exists)
- Calls `NettruyenExtractor` to scrape HTML and extract structured data via JSON-LD + regex
- Downloads thumbnail image to MinIO temp path, then moves to permanent path
- Creates `Comic`, `Image` (thumbnail), and `Chapter` (INIT status) entities in a single transaction

#### 2. Chapter Crawling (`CrawlServiceImpl.crawlChapter`)
- Only processes chapters with `CrawlStatus.INIT`
- Sets status to `STARTED` (lock) before processing
- Extracts image URLs from chapter HTML (`data-sv1`, `data-sv2` attributes)
- Downloads images in parallel using `parallelStream`
- Images stored in MinIO temp path first, then moved to permanent path
- Sets status to `FINISHED` on success

#### 3. SSE Chapter Streaming (`CrawlServiceImpl.crawlNettruyenSSEChapter`)
- Uses Mutiny `Multi` to stream chapter crawl events
- Each chapter emits an SSE event with type `ADD` (crawled) or `IGNORE` (already done)
- Runs on default executor via `Infrastructure.getDefaultExecutor()`

#### 4. GraphQL Queries
- Comics: paginated list with sorting, single comic by ID
- Chapters: by ID, by slug, paginated by comic ID
- Images: GraphQL resource exists but no queries implemented yet

#### 5. Temporary File Cleanup (`FileTempClearer`)
- Cron schedule: `0 0 0,7,14,19 * * ?` (midnight, 7am, 2pm, 7pm)
- Lists objects in MinIO temp prefix
- Deletes objects older than configured `minAgeMinutes`
- Skips execution if `cleanUp.enabled` is `false`

### Patterns & Best Practices
- **CDI Dependency Injection:** All services, repositories, and configs are CDI beans
- **ConfigMapping:** Type-safe configuration via SmallRye `@ConfigMapping`
- **Repository Pattern:** Panache repositories with custom query methods
- **Record Types:** DTOs modeled as Java `record` for immutability
- **Transactional Boundaries:** `@Transactional` on service methods that modify data
- **Blocking Annotation:** `@Blocking` on GraphQL queries to allow JPA operations on non-event-loop thread
- **Global Exception Handling:** `GlobalExceptionMapper` catches all unhandled exceptions, returns structured JSON error

## Integration Points
**How do pieces connect?**

### NetTruyen Integration
- **REST Client:** `NettruyenRestService` using MicroProfile REST Client (`@RegisterRestClient`)
  - `getContent(slugNId)` — fetches comic HTML page
  - `getComicChapterList(slug, comicId)` — fetches chapter list JSON
- **HTML Scraping:** Regex patterns extract:
  - `application/ld+json` script blocks (ComicSeries type)
  - `gOpts.comicSlug` and `gOpts.comicId` JavaScript variables
  - `data-sv1` / `data-sv2` image URLs from chapter pages
- **Image Download:** Direct HTTP GET with required headers (Referer, Origin) to bypass hotlink protection

### MinIO Integration
- **MinioClient** produced via CDI (`MinioClientProducer`)
- **Image workflow:** Download → save to `temp/` prefix → move to `images/` prefix
- **Temp cleanup:** Scheduled job scans and deletes stale temp objects

### PostgreSQL Integration
- **Hibernate ORM Panache** for entity management
- **Liquibase** migrations run at startup (`migrate-at-start: true`)
- **Sequences:** Separate sequences for each entity (`comic_id_seq`, `chapter_id_seq`, `image_id_seq`)
- **Deferred constraints:** Foreign keys are `DEFERRABLE INITIALLY DEFERRED` to handle circular references (comic ↔ image)

## Error Handling
**How do we handle failures?**

### Exception Strategy
| Exception | HTTP Status | Use Case |
|---|---|---|
| `ConflictException` | 409 | Duplicate comic crawl attempt |
| `ResourceNotFoundException` | 404 | Entity not found |
| `NotFoundException` (JAX-RS) | 404 | Comic/chapter not found |
| `BusinessException` | 500 | Generic business error |
| `Throwable` (global mapper) | 500 | Unhandled exceptions |

### Error Response Format
```json
{
  "processId": "uuid-string",
  "status": 500,
  "message": "Internal server error"
}
```

### Logging
- Uses JBoss Logging via Lombok `@JBossLog`
- Structured logging in production: `traceId`, `parentId`, `spanId`, `sampled` in log format
- Log levels: `INFO` default, `DEBUG` for detailed operations

## Performance Considerations
**How do we keep it fast?**

- **Native Image:** GraalVM native compilation for sub-second startup and < 128MB memory
- **Parallel Image Downloads:** Chapter images downloaded in parallel using `parallelStream()`
- **Lazy Loading:** Chapter images and comic chapters use `FetchType.LAZY`
- **Pagination:** All list queries support pagination via `PageRequest`
- **Sequence-based IDs:** Database sequences with `allocationSize=1` for efficient ID generation

## Security Notes
**What security measures are in place?**

- **No authentication/authorization currently implemented** — APIs are open
- **Secrets in production:** Database credentials, MinIO keys, and OTLP endpoint are injected via environment variables
- **Dev credentials:** Hardcoded in `application.yaml` for development convenience (non-sensitive)
- **Input validation:** Jakarta Validation API dependency included but minimal validation in place
- **Global error handling:** Stack traces not exposed in API responses (only generic error message)
