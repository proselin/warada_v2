# warada_v2 — Developer Learning Notes

> Written as a new-developer deep dive into the codebase. Covers everything from architecture to individual method-level details, configuration, data model, and known quirks.

---

## Table of Contents

1. [What Is This Project?](#1-what-is-this-project)
2. [Architecture Overview](#2-architecture-overview)
3. [Technology Stack](#3-technology-stack)
4. [Project Structure](#4-project-structure)
5. [Build & Configuration](#5-build--configuration)
6. [Database Schema & Migrations](#6-database-schema--migrations)
7. [Domain Model (JPA Entities)](#7-domain-model-jpa-entities)
8. [Enums & JPA Converters](#8-enums--jpa-converters)
9. [Repository Layer](#9-repository-layer)
10. [Service Layer](#10-service-layer)
11. [REST API Endpoints](#11-rest-api-endpoints)
12. [GraphQL API](#12-graphql-api)
13. [Crawl Pipeline (End-to-End)](#13-crawl-pipeline-end-to-end)
14. [Infrastructure: MinIO Object Storage](#14-infrastructure-minio-object-storage)
15. [Infrastructure: SSE (Server-Sent Events)](#15-infrastructure-sse-server-sent-events)
16. [Infrastructure: Scheduler](#16-infrastructure-scheduler)
17. [Infrastructure: RabbitMQ (Planned)](#17-infrastructure-rabbitmq-planned)
18. [Shared Models & Utilities](#18-shared-models--utilities)
19. [Exception Handling](#19-exception-handling)
20. [Testing](#20-testing)
21. [CI/CD Pipeline](#21-cicd-pipeline)
22. [Local Development Setup](#22-local-development-setup)
23. [Known Issues & Gotchas](#23-known-issues--gotchas)

---

## 1. What Is This Project?

**warada_v2** is a Quarkus-based Java backend for:
- **Crawling** manga/comic metadata and chapter images from [NetTruyen](https://nettruyenar.com)
- **Storing** images in MinIO (S3-compatible object storage)
- **Persisting** metadata (comics, chapters, images, tags) in PostgreSQL
- **Exposing** data via a **GraphQL API** for reading and **REST endpoints** for crawl triggers
- **Streaming** live crawl progress to clients via Server-Sent Events (SSE)

The README describes a planned three-microservice decomposition, but the **current `src/` is a single Quarkus monolith** combining all concerns. The event classes for inter-service communication exist but are unused while the RabbitMQ integration is in-progress.

---

## 2. Architecture Overview

### Current State (Monolith)
```
Client
  │
  ├─ REST  /api/v1/crawl/*  ──► CrawlRestResource
  │                                  │
  │                                  ▼
  │                           CrawlServiceImpl
  │                            ├── NettruyenExtractorImpl (HTML scraping)
  │                            ├── MinioClient (image storage)
  │                            ├── ComicRepository / ChapterRepository / ImageRepository
  │                            ├── TagService
  │                            └── CrawlProgressBroadcaster (SSE fan-out)
  │
  ├─ SSE   /api/v1/crawl/progress/{slug}  ──► CrawlRestResource (SSE stream)
  │
  └─ GraphQL /graphql  ──► ComicGqlResource
                       ──► ChapterGqlResource
                       ──► TagGqlResource
```

### Planned Microservice State (from README)
```
crawl-service  →[ComicExtractedEvent]→         core-service
core-service   →[ChapterPendingEvent]→         crawl-service
crawl-service  →[ChapterImagesExtractedEvent]→ core-service
display-service reads PostgreSQL directly (read-only)
```
The `shared/models/events/` package has the three event classes ready for this decomposition.

---

## 3. Technology Stack

| Category | Technology | Version / Notes |
|---|---|---|
| Language | Java | 21 |
| Framework | Quarkus | 3.38.2 |
| Build Tool | Gradle (Groovy DSL) | Single-module, no subprojects |
| REST | Quarkus REST (JAX-RS) + Jackson | `quarkus-rest`, `quarkus-rest-jackson` |
| GraphQL | SmallRye GraphQL | `quarkus-smallrye-graphql`, `quarkus-smallrye-graphql-client` |
| ORM | Hibernate ORM Panache | `quarkus-hibernate-orm-panache` |
| Database | PostgreSQL | 16, via `quarkus-jdbc-postgresql` |
| Migrations | Liquibase | `quarkus-liquibase`, auto-runs on start |
| Object Storage | MinIO | quarkiverse-minio 3.9.1 |
| HTTP Client | MicroProfile Rest Client | `quarkus-rest-client`, `quarkus-rest-client-jackson` |
| Scheduler | Quarkus Scheduler | `quarkus-scheduler` |
| SSE Streaming | SmallRye Mutiny `Multi` | Built into Quarkus REST |
| Observability | OpenTelemetry | `quarkus-opentelemetry`, enabled in prod only |
| Health | SmallRye Health | `quarkus-smallrye-health` |
| API Docs | SmallRye OpenAPI | `quarkus-smallrye-openapi` |
| DI Container | Quarkus ARC | CDI-based, compile-time |
| Testing | JUnit 5 + Mockito | `quarkus-junit`, `quarkus-junit-mockito` |
| Test HTTP | REST Assured | |
| Code Coverage | JaCoCo | HTML reports |
| Containerization | Docker (native micro-image) | Mandrel/GraalVM for native builds |
| CI/CD | GitHub Actions | |
| Messaging (planned) | RabbitMQ | infra defined in docker-compose, not wired in code |

---

## 4. Project Structure

```
warada_v2/
├── build.gradle                         # Build config + dependencies
├── gradle.properties                    # Quarkus version, Docker image settings
├── settings.gradle                      # Root project name (warada_v2)
├── docker-compose.yml                   # Local infra: postgres, rabbitmq, minio
├── .devcontainer/                       # VSCode/IntelliJ devcontainer definition
├── .github/
│   ├── workflows/build-native.yml       # CI/CD: build + push native Docker image
│   └── prompts/                         # AI-assist prompts (not runtime)
├── docs/
│   └── ai/                              # AI-generated design & implementation docs
└── src/
    ├── main/
    │   ├── java/com/github/warada_v2/
    │   │   ├── api/                     # Display / read API layer
    │   │   │   ├── models/
    │   │   │   │   └── ChapterDetailGql.java
    │   │   │   └── resources/
    │   │   │       ├── graphql/
    │   │   │       │   ├── ComicGqlResource.java
    │   │   │       │   ├── ChapterGqlResource.java
    │   │   │       │   └── TagGqlResource.java
    │   │   │       └── rest/
    │   │   │           └── HealthCheckResource.java
    │   │   ├── crawl/                   # Crawl domain
    │   │   │   ├── client/
    │   │   │   │   └── NettruyenRestService.java  # MicroProfile REST client
    │   │   │   ├── models/              # Crawl-specific DTOs
    │   │   │   │   ├── CrawlNettruyenComicRequest.java
    │   │   │   │   ├── CrawlProgressEvent.java
    │   │   │   │   ├── ExtractedChapterDetail.java
    │   │   │   │   ├── ExtractedChapters.java
    │   │   │   │   ├── ExtractedImage.java
    │   │   │   │   ├── ExtractedNettruyenComic.java
    │   │   │   │   └── ExtractedThumbImage.java
    │   │   │   ├── resources/rest/
    │   │   │   │   └── CrawlRestResource.java
    │   │   │   ├── scheduler/
    │   │   │   │   └── FileTempClearer.java
    │   │   │   └── services/
    │   │   │       ├── CrawlProgressBroadcaster.java     (interface)
    │   │   │       ├── CrawlService.java                 (interface)
    │   │   │       ├── NettruyenExtractor.java           (interface)
    │   │   │       └── impls/
    │   │   │           ├── CrawlProgressBroadcasterImpl.java
    │   │   │           ├── CrawlServiceImpl.java
    │   │   │           └── NettruyenExtractorImpl.java
    │   │   └── shared/                  # Cross-cutting library
    │   │       ├── configurations/
    │   │       │   ├── MinioBucketInitializer.java
    │   │       │   └── WaradaConfig.java
    │   │       ├── converter/
    │   │       │   ├── CrawlStatusConverter.java
    │   │       │   └── ImageTypeConverter.java
    │   │       ├── crawl/
    │   │       │   └── ExtractedComicDetail.java
    │   │       ├── entities/
    │   │       │   ├── Comic.java
    │   │       │   ├── Chapter.java
    │   │       │   ├── Image.java
    │   │       │   └── Tag.java
    │   │       ├── enums/
    │   │       │   ├── CrawlStatus.java
    │   │       │   └── ImageType.java
    │   │       ├── exception_mappers/
    │   │       │   ├── ConflictExceptionMapper.java
    │   │       │   ├── GlobalExceptionMapper.java
    │   │       │   └── ResourceNotFoundExceptionMapper.java
    │   │       ├── exceptions/
    │   │       │   ├── BusinessException.java
    │   │       │   ├── ConflictException.java
    │   │       │   └── ResourceNotFoundException.java
    │   │       ├── ez/
    │   │       │   └── Utils.java
    │   │       ├── models/
    │   │       │   ├── ApiError.java
    │   │       │   ├── ApiResponse.java
    │   │       │   ├── PageRequest.java
    │   │       │   ├── PageResponse.java
    │   │       │   └── events/
    │   │       │       ├── ChapterImagesExtractedEvent.java
    │   │       │       ├── ChapterPendingEvent.java
    │   │       │       └── ComicExtractedEvent.java
    │   │       ├── repositories/
    │   │       │   ├── ChapterRepository.java
    │   │       │   ├── ComicRepository.java
    │   │       │   ├── ImageRepository.java
    │   │       │   └── TagRepository.java
    │   │       └── services/
    │   │           ├── ChapterService.java             (interface)
    │   │           ├── ComicService.java               (interface)
    │   │           ├── TagService.java                 (interface)
    │   │           └── impls/
    │   │               ├── ChapterServiceImpl.java
    │   │               ├── ComicServiceImpl.java
    │   │               └── TagServiceImpl.java
    │   └── resources/
    │       ├── application.properties
    │       ├── schema.graphql               # Root GQL schema
    │       ├── graphql/                     # Per-type GQL schema fragments
    │       │   ├── chapter.graphql
    │       │   ├── comic.graphql
    │       │   ├── image.graphql
    │       │   └── tag.graphql
    │       └── db/migration/
    │           ├── db-changelog-master.yaml
    │           ├── initialize-270925.sql
    │           ├── remove-unique-260226.sql
    │           └── add-tags-table-260304.sql
    └── test/java/com/github/warada_v2/
        ├── commons/         UtilsTest.java
        ├── converter/       ConvertersTest.java
        ├── exceptions/      ExceptionsTest.java
        ├── mapper/          GlobalExceptionMapperTest.java
        ├── models/          ApiErrorTest.java, ModelsTest.java, PageRequestTest.java, PageResponseTest.java
        ├── resources/       CrawlRestResourceTest.java, ComicGqlResourceTest.java,
        │                    ChapterGqlResourceTest.java, TagGqlResourceTest.java, HealthCheckResourceTest.java
        └── services/        CrawlServiceImplTest.java, ComicServiceImplTest.java,
                             ChapterServiceImplTest.java, TagServiceImplTest.java,
                             NettruyenExtractorImplTest.java, ChapterCrawlConsumerTest.java
```

---

## 5. Build & Configuration

### `build.gradle`
Single-module Gradle build. Key settings:
- `group = 'com.github.warada_v2'`, `version = '1.0.0'`
- `sourceCompatibility = JavaVersion.VERSION_21`
- Uses Quarkus Gradle plugin (`id 'io.quarkus'`)
- JaCoCo coverage: HTML report generated after each `test` run
- Compiler flag `-parameters` retained for Quarkus reflection

### `application.properties`

The app uses Quarkus **profile-based configuration** (`%dev.*`, `%prod.*`).

#### Base Config (all profiles)
| Property | Value | Meaning |
|---|---|---|
| `quarkus.application.name` | `warada_v2` | App name |
| `quarkus.http.port` | `8080` | HTTP server port |
| `quarkus.http.host` | `0.0.0.0` | Listen on all interfaces |
| `quarkus.hibernate-orm.packages` | `shared.entities`, `shared.converter` | Hibernate entity scan paths |
| `quarkus.liquibase.migrate-at-start` | `true` | Auto-run migrations on startup |
| `quarkus.liquibase.change-log` | `db/migration/db-changelog-master.yaml` | Migration master file |
| `quarkus.smallrye-graphql.ui.enabled` | `true` | GQL Playground (overridden to `false` in prod) |
| `warada.nettruyen.url` | `https://nettruyenar.com` | Source website base URL |
| `warada.image-bucket` | `warada-images` | MinIO bucket name |
| `warada.image-bucket-temp-path` | `temp` | MinIO temp folder prefix |
| `warada.image-bucket-images-path` | `images` | MinIO permanent folder prefix |
| `warada.clean-up.enabled` | `true` | Enable temp file cleanup scheduler |
| `warada.clean-up.min-age-minutes` | `5` | Delete temp files older than N minutes |

#### Dev Profile (`%dev.*`)
| Property | Default Value | Env Override |
|---|---|---|
| `datasource.db-kind` | `postgresql` | — |
| `datasource.username` | `devuser` | `DEV_DB_USERNAME` |
| `datasource.password` | `devpassword` | `DEV_DB_PASSWD` |
| `datasource.jdbc.url` | `jdbc:postgresql://localhost:5432/postgres` | `DEV_DB_URL` |
| `hibernate-orm.log.sql` | `false` | — |
| `otel.enabled` | `false` | — |
| `minio.host` | `localhost` | `DEV_MINIO_HOST` |
| `minio.port` | `9000` | `DEV_MINIO_PORT` |
| `minio.access-key` | `admin` | `DEV_MINIO_ACCESS_KEY` |
| `minio.secret-key` | `admin123123123123` | `DEV_MINIO_SECRET_KEY` |
| `minio.secure` | `false` | `DEV_MINIO_SECURE` |
| `package.jar.enabled` | `true` | — |

#### Prod Profile (`%prod.*`)
- All DB credentials come from environment variables: `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`
- OpenTelemetry enabled; endpoint from `OTLP_ENDPOINT`
- MinIO from env vars: `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_SECURE`
- Native binary build: `quarkus.native.enabled=true`, uses `ubi-quarkus-mandrel-builder-image:jdk-21`
- JAR packaging disabled (`quarkus.package.jar.enabled=false`)

### `WaradaConfig.java`
A type-safe config mapping interface (using SmallRye Config `@ConfigMapping`):
```java
@ConfigMapping(prefix = "warada")
public interface WaradaConfig {
    String imageBucket();
    String imageBucketTempPath();
    String imageBucketImagesPath();
    NettruyenConfig nettruyen();      // nested: .url()
    CleanUpTempImages cleanUp();      // nested: .enabled(), .minAgeMinutes()
}
```
Injected anywhere via `@Inject WaradaConfig waradaConfig`.

### `docker-compose.yml`
Three infrastructure services for local dev:

| Service | Image | Ports | Purpose |
|---|---|---|---|
| `postgres` | `postgres:16` | `5432:5432` | Primary database |
| `rabbitmq` | `rabbitmq:4.1-management` | `5672:5672`, `15672:15672` | Message broker (planned) |
| `minio` | `minio/minio:RELEASE.2025-04-22...` | `9000:9000`, `9001:9001` | Object storage + console |

Application service definitions are commented out in docker-compose — run the app with Quarkus dev mode directly.

---

## 6. Database Schema & Migrations

Managed by **Liquibase** with a master YAML changelog that includes SQL files in order.

### Migration 1: `initialize-270925.sql`
Creates the core tables:

**`comics`** — one row per manga series
| Column | Type | Notes |
|---|---|---|
| `id` | bigint PK | from `comic_id_seq` |
| `slug` | varchar(200) UNIQUE | URL-friendly identifier |
| `title` | varchar(255) | Display title |
| `author` | varchar(255) | Author name |
| `description` | text | Summary |
| `status` | varchar(255) NOT NULL | e.g. `OnGoing`, `Completed` |
| `chapter_count` | integer | Denormalized count |
| `crawling_status` | varchar(255) NOT NULL | `0000`/`0001`/`9999` codes |
| `origin_id` | varchar(255) UNIQUE | Source website comic ID |
| `origin_url` | varchar(500) UNIQUE | Source URL |
| `origin_path_params` | varchar(500) | URL path segment for matching |
| `thumb_image_id` | bigint UNIQUE | FK → `images.id` |
| `created_at`, `updated_at` | timestamp(6) | Audit timestamps |

**`chapters`** — one row per chapter
| Column | Type | Notes |
|---|---|---|
| `id` | bigint PK | from `chapter_id_seq` |
| `comic_id` | bigint | FK → `comics.id` CASCADE DELETE |
| `chapter_num` | varchar(255) | e.g. `"Chapter 10"` |
| `position` | integer | Order within the comic |
| `crawling_status` | varchar(255) | Code string |
| `type` | varchar(255) | `main`, `extra`, etc. |
| `origin_url` | varchar(500) UNIQUE | |
| `origin_path_params` | varchar(500) UNIQUE | |
| `created_at`, `updated_at` | timestamp(6) | |

**`images`** — one row per page image or thumbnail
| Column | Type | Notes |
|---|---|---|
| `id` | bigint PK | from `image_id_seq` |
| `chapter_id` | bigint | FK → `chapters.id` CASCADE DELETE |
| `position` | integer | Page order |
| `file_name` | varchar(1000) | Stored filename |
| `file_path` | varchar(1000) | MinIO object path |
| `origin_url` | varchar(500) UNIQUE | Source image URL |
| `type` | varchar(255) | `THUMB` or `CHAPTER_IMAGE` |
| `created_at`, `updated_at` | timestamp(6) | |

FK constraints are `DEFERRABLE INITIALLY DEFERRED` — this allows inserting related records in one transaction without worrying about insert order.

### Migration 2: `remove-unique-260226.sql`
Drops the `UNIQUE` constraints on `images.file_name` and `images.file_path`. This was needed because the same image filename can appear across different comics.

### Migration 3: `add-tags-table-260304.sql`
Replaces the old comma-separated `tags` column in `comics` with a normalized many-to-many structure:

**`tags`** — one row per genre/tag
| Column | Type | Notes |
|---|---|---|
| `id` | bigint PK | from `tag_id_seq` |
| `name` | varchar(255) | Original casing |
| `normalized_name` | varchar(255) UNIQUE | Lowercase for dedup matching |
| `comic_count` | integer DEFAULT 0 | Denormalized count |
| `created_at`, `updated_at` | timestamp(6) | |

**`comic_tags`** — junction table (composite PK)
| Column | Notes |
|---|---|
| `comic_id` | FK → `comics.id` CASCADE DELETE |
| `tag_id` | FK → `tags.id` CASCADE DELETE |

Indexes on `tags(normalized_name)`, `comic_tags(comic_id)`, `comic_tags(tag_id)`.

---

## 7. Domain Model (JPA Entities)

All entities:
- Extend `PanacheEntityBase` (no built-in id field — explicitly declared)
- Use `@SequenceGenerator` + `@GeneratedValue(strategy = SEQUENCE)` for ID generation
- Use Lombok `@Builder`, `@Getter`, `@Setter` (or manual builder)
- Auto-apply JPA attribute converters for `CrawlStatus` and `ImageType`

### `Comic.java`
```
@Entity @Table("comics")
Fields:
  Long id                       — PK, comic_id_seq
  String title
  String slug                   — @Column(unique=true, length=200)
  String originId               — unique
  String originPathParams
  String originUrl              — unique
  String author
  String description
  String status
  Integer chapterCount
  CrawlStatus crawlingStatus    — stored as code string via CrawlStatusConverter
  Image thumbImage              — @OneToOne, FK thumb_image_id
  List<Tag> tags                — @ManyToMany, junction comic_tags
  List<Chapter> chapters        — @OneToMany, cascade=ALL, orphanRemoval=true, @JsonIgnore
  LocalDateTime createdAt, updatedAt
```

### `Chapter.java`
```
@Entity @Table("chapters")
Fields:
  Long id                       — PK, chapter_id_seq
  String chapterNumber          — maps to column chapter_num
  Integer position
  String originUrl              — unique
  String originPathParams       — unique
  String type
  CrawlStatus crawlingStatus    — stored as code string
  Comic comic                   — @ManyToOne(fetch=LAZY), @JsonIgnore
  List<Image> images            — @OneToMany(fetch=LAZY), @JsonIgnore
  LocalDateTime createdAt, updatedAt
```

### `Image.java`
```
@Entity @Table("images")
Fields:
  Long id                       — PK, image_id_seq
  String fileName
  String filePath
  Integer position
  ImageType type                — stored as value string via ImageTypeConverter
  String originUrl              — unique
  Comic comic                   — @OneToOne(mappedBy="thumbImage"), @JsonIgnore
  Chapter chapter               — @ManyToOne(fetch=LAZY), @JsonIgnore
  LocalDateTime createdAt, updatedAt
```

### `Tag.java`
```
@Entity @Table("tags")
Fields:
  Long id                       — PK, tag_id_seq
  String name                   — original casing
  String normalizedName         — unique, lowercase
  Integer comicCount            — default 0
  List<Comic> comics            — @ManyToMany(mappedBy="tags"), @JsonIgnore
  LocalDateTime createdAt, updatedAt
```

---

## 8. Enums & JPA Converters

### `CrawlStatus` enum
| Constant | Code Value | Meaning |
|---|---|---|
| `INIT` | `"0000"` | Not yet crawled |
| `STARTED` | `"0001"` | Crawl in progress |
| `FINISHED` | `"9999"` | Successfully crawled |

Stored as the code string (`"0000"`, etc.) in the database via `CrawlStatusConverter`.

### `ImageType` enum
| Constant | Value | Meaning |
|---|---|---|
| `THUMB_IMAGE` | `"THUMB"` | Cover/thumbnail image |
| `CHAPTER_IMAGE` | `"CHAPTER_IMAGE"` | Page image inside a chapter |

Stored as the value string via `ImageTypeConverter`.

### JPA Converters
Both converters are annotated `@Converter(autoApply = true)`, meaning Hibernate applies them automatically to any entity field of the matching type without needing `@Convert` annotations on each field.

```java
// CrawlStatusConverter: CrawlStatus ↔ String (the .code field)
// ImageTypeConverter:   ImageType   ↔ String (the .value field)
```

---

## 9. Repository Layer

All repositories implement `PanacheRepository<Entity>` and are `@ApplicationScoped`. Panache provides all standard CRUD operations for free.

### `ComicRepository`
| Method | Signature | Notes |
|---|---|---|
| `existsByOriginPathParams` | `(String) → boolean` | Count query, used for duplicate check before crawl |
| `findByOriginPathParams` | `(String) → Optional<Comic>` | |
| `findBySlug` | `(String) → Optional<Comic>` | |
| `findPageableComics` | `(Sort, Page) → List<Comic>` | `findAll(sort).page(page).list()` |
| `existedBySlugAndComicId` | `(String) → boolean` | ⚠️ **Bug**: queries non-existent field `slugNComicId`; would throw at runtime but is currently unused |

### `ChapterRepository`
| Method | Signature | Notes |
|---|---|---|
| `findByIdWithComic` | `(Long) → Optional<Chapter>` | JPQL with `LEFT JOIN FETCH c.comic` to avoid N+1 |
| `findByOriginPathParams` | `(String) → Optional<Chapter>` | |
| `findBySlug` | `(String slug) → Optional<Chapter>` | ⚠️ Despite the name, queries `originPathParams`, not a `slug` field |
| `findByComicId` | `(Long, Sort, Page) → List<Chapter>` | Paginated |
| `persistOrUpdate` | `(Chapter) → void` | If `id == null`, calls `persist`; else calls `getEntityManager().merge()` |

### `TagRepository`
| Method | Signature | Notes |
|---|---|---|
| `findByNormalizedName` | `(String) → Optional<Tag>` | |
| `findByNormalizedNameIn` | `(List<String>) → List<Tag>` | Batch lookup for resolveOrCreate |
| `findAllOrderByComicCountDesc` | `() → List<Tag>` | `list("ORDER BY comicCount DESC")` |

### `ImageRepository`
Extends `PanacheRepository<Image>` with no custom query methods — only standard Panache CRUD.

---

## 10. Service Layer

### `ComicService` / `ComicServiceImpl`
`@ApplicationScoped`

| Method | Signature | Behavior |
|---|---|---|
| `getPageableComics` | `(PageRequest) → List<Comic>` | Null-safe: defaults to page=0, size=20 if `pageRequest` is null or has no `sortBy`. Calls `comicRepository.findPageableComics`. |
| `findById` | `(Long id) → Comic` | Throws `ResourceNotFoundException` if not found. |

### `ChapterService` / `ChapterServiceImpl`
`@ApplicationScoped`

| Method | Signature | Behavior |
|---|---|---|
| `getChapterById` | `(Long id) → Chapter` | Returns `null` if not found (note: no exception — different behavior from `ComicService`). |
| `pageableChapterByComicId` | `(Long comicId, PageRequest) → List<Chapter>` | Delegates to `ChapterRepository.findByComicId`. |
| `getChapterBySlug` | `(String slug) → Chapter` | Returns `null` if not found. Calls `ChapterRepository.findBySlug`, which actually queries `originPathParams`. |

### `TagService` / `TagServiceImpl`
`@ApplicationScoped`

| Method | Signature | Behavior |
|---|---|---|
| `getAllTags` | `() → List<Tag>` | Returns all tags ordered by `comicCount DESC`. |
| `findById` | `(Long id) → Tag` | Throws `jakarta.ws.rs.NotFoundException` if missing. |
| `resolveOrCreateTags` | `(List<String> rawTagNames) → List<Tag>` | Normalizes (trim+lowercase+dedupe), batch-fetches existing, creates missing, persists new tags. Returns full list. |
| `incrementComicCount` | `(List<Tag> tags) → void` | Adds 1 to `comicCount` on each tag, persists each. Called after a comic is successfully persisted. |

### `CrawlService` / `CrawlServiceImpl`
`@ApplicationScoped` — the core orchestrator. See §13 for the full flow.

| Method | Signature | Behavior |
|---|---|---|
| `crawlNettruyenComic` | `(String slugNId) → void` | Full comic crawl: validate, extract, persist, crawl all chapters. |
| `crawlChapterById` | `(Long chapterId) → void` | Re-crawl a specific chapter by DB ID. Looks up chapter with comic join. |
| `retryFailedChapters` | `(String comicSlug) → void` | Finds all chapters for `comicSlug` where `crawlingStatus = INIT`, re-runs `crawlChapterInternal` on each. |

Private helpers:
- `persistComic(...)` — runs in `QuarkusTransaction.requiringNew()`. Creates Comic+Chapter+Image entities and persists everything. Returns a list of `ChapterStub` records.
- `crawlChapterInternal(ChapterStub)` — calls extractor, moves images to permanent MinIO paths (parallel), then persists image records in a new transaction.
- `persistChapterImages(Long chapterId, List<ImageInfo>)` — in a `QuarkusTransaction.requiringNew()`, persists Image entities for each page, sets chapter status to `FINISHED`.
- `renameObject(bucket, src, dest)` — MinIO copy-then-delete (no atomic rename).

Internal records (private to `CrawlServiceImpl`):
- `ChapterStub(chapterId, comicSlug, chapterNumber, originPathParams)`
- `ComicChapterInfo(chapterName, originPathParams, originUrl, chapterNum)`
- `ImageInfo(fileName, filePath, originUrl, position)`

### `NettruyenExtractor` / `NettruyenExtractorImpl`
`@ApplicationScoped` — HTML scraper and image downloader for NetTruyen.

| Method | Signature | Behavior |
|---|---|---|
| `extractComicDetail` | `(String slug) → ExtractedComicDetail` | Fetches HTML, extracts comic metadata via JSON-LD, fetches chapter list, downloads thumbnail. |
| `extractChapterDetail` | `(String slug) → ExtractedChapterDetail` | Fetches chapter HTML, regex-extracts image URLs (`data-sv1`, `data-sv2` attributes), downloads all images to MinIO `temp/` in parallel. |
| `extractComicHtmlContent` | `(String html) → ExtractedNettruyenComic` | Public method. Parses `<script type="application/ld+json">` blocks, finds `ComicSeries` node, maps to DTO. Also extracts `gOpts.comicSlug` and `gOpts.comicId` via regex. |
| `getChapterList` | `(Long comicId, String slug) → ExtractedChapters` | Calls the NetTruyen ASMX chapter-list API, generates `originPathParams` and `originUrl` for each chapter. |

Private helpers:
- `downloadNSaveImage(imageUrl, uniqueName)` — Downloads image with custom Referer/Origin headers (to bypass hotlink protection), uploads to MinIO `temp/{timestamp}_{uniqueName}{ext}`, returns `{fileName, objectTemp}` map.
- `pullImage(imageUrl)` — Creates a JAX-RS `Client`, sets proper CORS/Referer headers, GETs image bytes.
- `getChapterContent(slug)` — GETs chapter HTML using JAX-RS client.
- `crawlThumbImage(imageUrl)` — Calls `downloadNSaveImage` and wraps result in `ExtractedThumbImage`.
- `generateChapterPathParams(comicSlug, chapterSlug, chapterOriginId)` → `"{comicSlug}/{chapterSlug}/{chapterOriginId}"`
- `generateChapterOriginUrl(chapterSlug)` → `"{nettruyenUrl}/truyen-tranh/{chapterSlug}"`
- `flattenJsonLd(JsonNode)` — Handles `@graph` arrays in JSON-LD.

Regex patterns used:
- `LD_JSON_PATTERN` — finds `<script type="application/ld+json">` blocks
- `SLUG_P` — extracts `gOpts.comicSlug = 'value'`
- `ID_P` — extracts `gOpts.comicId = 12345`
- `IMG_SERVER_P` — extracts `data-sv1="url1" data-sv2="url2"` pairs (image servers)

### `CrawlProgressBroadcaster` / `CrawlProgressBroadcasterImpl`
`@ApplicationScoped` — In-memory SSE fan-out manager.

| Method | Signature | Behavior |
|---|---|---|
| `subscribe` | `(String comicSlug) → Multi<CrawlProgressEvent>` | Creates a Mutiny `Multi` backed by a new `MultiEmitter`. Registers the emitter in `ConcurrentHashMap<String, CopyOnWriteArrayList<MultiEmitter>>`. Auto-removes on termination. |
| `emit` | `(String comicSlug, CrawlProgressEvent) → void` | Fans out the event to all emitters registered for `comicSlug`. |
| `markActive` | `(String comicSlug) → void` | Adds slug to active-crawls set. |
| `isActive` | `(String comicSlug) → boolean` | Checks if crawl is active. |
| `markInactive` | `(String comicSlug) → void` | Removes from active-crawls set. |

State is purely in-memory — not shared across JVM instances.

---

## 11. REST API Endpoints

### Crawl REST: `CrawlRestResource` — base path `/api/v1/crawl`

#### `POST /api/v1/crawl/nettruyen/comic`
Triggers a full comic crawl.

Request body:
```json
{ "slug-and-id": "truyen-tranh-slug-12345" }
```
Mapped to `CrawlNettruyenComicRequest` (a record with field `slugNId` annotated `@JsonProperty("slug-and-id")`).

Response: `200 OK` with body `"Success"`.

Errors:
- `409 Conflict` — comic already exists (`ConflictException`)
- `500` — any other failure

**This call is blocking** — it runs the entire crawl (all chapters) before returning.

#### `POST /api/v1/crawl/nettruyen/comic/{slug}/retry`
Re-crawls all `INIT`-status chapters for the given comic slug.

Response: `200 OK` with body `"Retry queued"`.

#### `GET /api/v1/crawl/progress/{comicSlug}`
Opens an SSE stream for live crawl progress.

Response: `text/event-stream` (content negotiation), each event is a JSON-serialized `CrawlProgressEvent`.

Event structure:
```json
{
  "type": "processing | completed | failed | skipped",
  "message": "Human-readable description",
  "data": {
    "chapterId": 42,
    "chapterNumber": "Chapter 5",
    "status": null,
    "comicSlug": "one-piece-12345"
  }
}
```

### Health REST: `HealthCheckResource` — path `/health`

#### `GET /health`
Manual health check endpoint (separate from Quarkus built-in `/q/health`).

Response: `200 OK` with body `{"status": "UP"}`.

---

## 12. GraphQL API

Endpoint: `POST /graphql`
Dev UI: `http://localhost:8080/q/graphql-ui`

Schema defined in `src/main/resources/schema.graphql` and type fragments in `src/main/resources/graphql/`.

### GraphQL Types

**`Comic`**: `id`, `createdAt`, `updatedAt`, `title`, `chapterCount`, `originId`, `status`, `description`, `originUrl`, `author`, `slug`, `crawlingStatus`, `tags: [Tag]`, `thumbImage: Image`, `chapters: [Chapter]`

**`Chapter`**: `id`, `createdAt`, `updatedAt`, `chapterNumber`, `sourceUrl` (renamed from `originUrl`), `type`, `position`, `crawlingStatus`, `comic: Comic`, `images: [Image]`

**`Image`**: `id`, `createdAt`, `updatedAt`, `fileName`, `position`, `type`, `originUrl`, `comic: Comic`, `chapter: Chapter`

**`Tag`**: `id`, `name`, `normalizedName`, `comicCount`, `updatedAt`, `createdAt`

**`PageRequest` (input)**: `page: Int`, `size: Int`, `sortBy: String`, `sortOrder: String`

### Queries

| Query | Signature | Resolver | Notes |
|---|---|---|---|
| `ping` | `() → String` | `ComicGqlResource` | Returns `"Pong"` — debug probe |
| `comics` | `(pageRequest: PageRequest) → [Comic!]!` | `ComicGqlResource` | Paginated list; defaults page=0, size=20 |
| `comic` | `(id: ID!) → Comic` | `ComicGqlResource` | Single comic by DB id |
| `chapters` | `(comicId: ID!, pageRequest: PageRequest) → [ChapterDetailGql]` | `ChapterGqlResource` | Paginated chapters for a comic |
| `detailChapterById` | `(id: ID!) → ChapterDetailGql` | `ChapterGqlResource` | Chapter + images by DB id |
| `detailChapterBySlug` | `(slug: String) → ChapterDetailGql` | `ChapterGqlResource` | Chapter by path params |
| `tags` | `() → [Tag]` | `TagGqlResource` | All tags, sorted by `comicCount DESC` |
| `tag` | `(id: ID!) → Tag` | `TagGqlResource` | Single tag by DB id |

**`ChapterDetailGql`** — a DTO class (not the `Chapter` entity directly) used as the GQL return type for chapter queries. Maps entity fields and renames `originUrl` → `sourceUrl`.

---

## 13. Crawl Pipeline (End-to-End)

```
1. Client: POST /api/v1/crawl/nettruyen/comic
           body: {"slug-and-id": "manga-slug-12345"}
                  ↓
2. CrawlRestResource.crawlIndividualComic()
   → calls CrawlServiceImpl.crawlNettruyenComic("manga-slug-12345")
                  ↓
3. Duplicate check
   comicRepository.existsByOriginPathParams("manga-slug-12345")
   → throws ConflictException (409) if already exists
                  ↓
4. NettruyenExtractorImpl.extractComicDetail("manga-slug-12345")
   a. GET https://nettruyenar.com/truyen-tranh/manga-slug-12345  (HTML)
   b. Parse JSON-LD <script> blocks → find @type="ComicSeries" node
   c. Regex extract gOpts.comicSlug, gOpts.comicId from HTML
   d. GET /Comic/Services/ComicService.asmx/ChapterList?slug=...&comicId=...
      → deserialize ExtractedChapters, build originPathParams + originUrl per chapter
   e. Download thumbnail image → MinIO temp/{timestamp}_thumbnail{ext}
   Returns ExtractedComicDetail(comic, chapters, thumbImage)
                  ↓
5. MinIO: copy thumbnail from temp/ → {slug}/thumb/{fileName}, delete temp object
                  ↓
6. QuarkusTransaction.requiringNew() → persistComic()
   a. tagService.resolveOrCreateTags(genres)
      - normalize: trim + lowercase + deduplicate raw genre strings
      - batch fetch existing tags from DB
      - create+persist any new tags
   b. Build Comic entity: slug, title, author, description, originId, originUrl,
      originPathParams, status="OnGoing", crawlingStatus=FINISHED, chapterCount
   c. Build Image (thumb): fileName, filePath, originUrl, position=0, type=THUMB_IMAGE
   d. Build Chapter list (all: crawlingStatus=INIT)
   e. comicRepository.persistAndFlush(comic)  ← saves comic + thumb + chapters in one TX
   f. tagService.incrementComicCount(tags)    ← updates tag.comicCount
   Returns List<ChapterStub>
                  ↓
7. For each ChapterStub:
   a. broadcaster.emit(slug, CrawlProgressEvent{PROCESSING, "Processing chapter N"})
   b. crawlChapterInternal(stub):
      i.  NettruyenExtractorImpl.extractChapterDetail(originPathParams)
          - GET chapter HTML page
          - Regex find data-sv1/data-sv2 image server URLs
          - For each image URL (parallel):
            * Download image bytes with Referer/Origin headers
            * Upload to MinIO temp/{timestamp}_{position}{ext}
          Returns ExtractedChapterDetail(List<ExtractedImage>)
      ii. For each extracted image (parallel):
          - MinIO copy: temp/... → {slug}/chapters/{chapterNum}/{fileName}
          - MinIO delete: temp/...
      iii. QuarkusTransaction.requiringNew() → persistChapterImages()
           - persist Image entities (type=CHAPTER_IMAGE) for each page
           - set chapter.crawlingStatus = FINISHED
           - chapterRepository.persistOrUpdate(chapter)
   c. broadcaster.emit(slug, CrawlProgressEvent{COMPLETED or FAILED, message})
                  ↓
8. Response: 200 "Success"
```

---

## 14. Infrastructure: MinIO Object Storage

### Initialization
`MinioBucketInitializer` is `@ApplicationScoped` with a `@PostConstruct` method that:
1. Checks if the `warada-images` bucket exists via `MinioClient.bucketExists()`
2. Creates it via `MinioClient.makeBucket()` if it doesn't exist

### Object Naming Convention
| Object Type | MinIO Path Pattern |
|---|---|
| Temp (during crawl) | `temp/{yyyyMMdd_HHmmss}_{uniqueName}{ext}` |
| Thumbnail (permanent) | `{comicSlug}/thumb/{fileName}` |
| Chapter image (permanent) | `{comicSlug}/chapters/{chapterNumber}/{fileName}` |

### "Rename" Operation
MinIO has no native rename/move. `renameObject(bucket, src, dest)` does:
```java
minioClient.copyObject(CopyObjectArgs... src → dest)
minioClient.removeObject(RemoveObjectArgs... src)
```
If the delete fails after a successful copy, you'll have a duplicate object in temp.

---

## 15. Infrastructure: SSE (Server-Sent Events)

`CrawlProgressBroadcasterImpl` manages in-memory SSE fan-out:

```
subscribe("manga-slug-12345")
  → new MultiEmitter<CrawlProgressEvent> registered in Map
  → returns Multi<CrawlProgressEvent> (streamed to HTTP client)

emit("manga-slug-12345", event)
  → iterates CopyOnWriteArrayList of emitters for that slug
  → calls emitter.emit(event) on each

Emitter cleanup:
  → auto-removed from list on termination (complete/fail/cancel)
```

State is per-JVM. Not suitable for multi-instance deployments without a shared pub/sub mechanism.

---

## 16. Infrastructure: Scheduler

`FileTempClearer` — deletes old temp MinIO objects on a schedule.

| Property | Value |
|---|---|
| Class | `crawl.scheduler.FileTempClearer` |
| Schedule | `@Scheduled(cron = "0 0 0,7,14,19 * * ?")` — runs at midnight, 7am, 2pm, 7pm |
| Concurrency | `@Scheduled(concurrentExecution = SKIP)` — skips if previous run still going |
| Toggle | Only runs if `warada.clean-up.enabled = true` |

Logic:
1. Lists all objects under the `temp/` prefix in the `warada-images` bucket
2. For each object older than `warada.clean-up.min-age-minutes` (default: 5 minutes), calls `MinioClient.removeObject()`
3. Logs count of deleted files

---

## 17. Infrastructure: RabbitMQ (Planned)

RabbitMQ 4.1 is defined in `docker-compose.yml` (ports 5672/15672) but **not wired into the application code**. The SmallRye Reactive Messaging dependency is not in `build.gradle`.

The three event classes in `shared/models/events/` represent the planned message contracts:

| Event Class | Direction | Purpose |
|---|---|---|
| `ComicExtractedEvent` | crawl-service → core-service | Comic metadata + chapter list + thumbnail after scraping |
| `ChapterPendingEvent` | core-service → crawl-service | Request to crawl images for a specific chapter |
| `ChapterImagesExtractedEvent` | crawl-service → core-service | Result of a chapter image crawl, with file paths |

The `ChapterCrawlConsumerTest` file exists but all tests are commented out, referencing a `ChapterCrawlConsumer` that is not yet implemented.

---

## 18. Shared Models & Utilities

### `PageRequest.java`
Plain Java class for pagination input:
| Field | Default | Notes |
|---|---|---|
| `page` | `0` | Zero-based page number |
| `size` | `20` | Items per page |
| `sortBy` | `null` | Field name to sort by |
| `sortOrder` | `"asc"` | `"asc"` or `"desc"` |

Methods:
- `toPage()` → `Page.of(page, size)` (Panache `Page`)
- `toSort()` → `Sort.ascending(sortBy)` or `Sort.descending(sortBy)`, returns `null` if `sortBy` is blank
- `getOffSet()` → `page * size`

### `PageResponse.java`
Generic response wrapper:
```java
public record PageResponse<T>(List<T> data, int page, int size, long total) {}
```

### `ApiError.java`
Standard error response body:
```java
public class ApiError {
    String processId;   // UUID to correlate logs
    int status;         // HTTP status code
    String message;     // Error message
    String timestamp;   // ISO timestamp
}
```

### `ApiResponse.java`
Generic success wrapper (rarely used directly in current code — most endpoints return entity directly or a plain String).

### `ExtractedComicDetail.java` (in `shared/crawl/`)
A record tying together the results of a comic extraction:
```java
public record ExtractedComicDetail(
    ExtractedNettruyenComic comic,
    ExtractedChapters chapters,
    ExtractedThumbImage thumbImage
) {}
```

### `Utils.java` (in `shared/ez/`)
Utility class with static helpers. Contains at minimum:
- `getExtensionFromUrl(String url)` — extracts file extension from a URL string (e.g., `.jpg`, `.webp`). Used when naming downloaded images.

### Crawl Models (DTOs, all in `crawl/models/`)

| Class | Type | Description |
|---|---|---|
| `CrawlNettruyenComicRequest` | Record | REST request body, `@JsonProperty("slug-and-id")` → `slugNId` |
| `CrawlProgressEvent` | Record | SSE event: `type`, `message`, `data: ChapterProgress` |
| `CrawlProgressEvent.EventType` | Enum | `PROCESSING`, `COMPLETED`, `FAILED`, `SKIPPED` (serialized lowercase via `@JsonValue`) |
| `CrawlProgressEvent.ChapterProgress` | Record | `chapterId`, `chapterNumber`, `status`, `comicSlug` |
| `ExtractedNettruyenComic` | Record | JSON-LD `ComicSeries` data: `name`, `url`, `author`, `genre`, `image`, `description`, plus `comicId`, `slug` added post-parsing |
| `ExtractedChapters` | Record | Container for `List<ExtractedChapter>` — the chapter list API response |
| `ExtractedImage` | Record | One scraped image: `imageUrls`, `fileName`, `objectTemp`, `position`, `imageUrlPulled`. Has `withFileNameWithObjectWithImageUrlPulled(...)` builder-style wither. |
| `ExtractedThumbImage` | Record | Thumbnail: `originUrl`, `fileName`, `objectTemp` |
| `ExtractedChapterDetail` | Record | Wraps `List<ExtractedImage>` returned from chapter scraping |

---

## 19. Exception Handling

### Custom Exceptions

| Exception | Extends | Notes |
|---|---|---|
| `BusinessException` | `RuntimeException` | Base business exception; carries `int statusCode` (default 400) |
| `ConflictException` | `RuntimeException` | Used when a resource already exists (e.g., duplicate comic crawl) |
| `ResourceNotFoundException` | `RuntimeException` | Two constructors: `(String message)` or `(String resource, String field, Object value)` |

### Exception Mappers (JAX-RS `@Provider`)

| Mapper | Catches | HTTP Status | Response Body |
|---|---|---|---|
| `ConflictExceptionMapper` | `ConflictException` | `409 Conflict` | Plain string: the exception message |
| `ResourceNotFoundExceptionMapper` | `ResourceNotFoundException` | `404 Not Found` | Plain string: the exception message |
| `GlobalExceptionMapper` | `Throwable` (catch-all) | `500 Internal Server Error` | JSON `ApiError` with `processId`, `status`, `message`, `timestamp` |

The `GlobalExceptionMapper` generates a `processId` (UUID) for each error, which allows correlating frontend error reports with server logs.

---

## 20. Testing

Tests are pure unit tests — no `@QuarkusTest` annotations, no database, no MinIO. All dependencies are mocked with **Mockito** via `@Mock` + `MockitoAnnotations.openMocks(this)` in `@BeforeEach`.

### Test Files

| Test File | What It Tests |
|---|---|
| `CrawlServiceImplTest` | `crawlNettruyenComic` (happy path, conflict), `retryFailedChapters`, chapter crawl logic — mocks `NettruyenExtractor`, `MinioClient`, repositories, `TagService`, `broadcaster` |
| `ComicServiceImplTest` | `getPageableComics` with null/valid `PageRequest`, `findById` found/not-found |
| `ChapterServiceImplTest` | Chapter retrieval by id and slug |
| `TagServiceImplTest` | `resolveOrCreateTags` (all exist, none exist, mixed), `incrementComicCount` |
| `NettruyenExtractorImplTest` | HTML parsing (`extractComicHtmlContent`), `extractChapterDetail` |
| `ChapterCrawlConsumerTest` | All tests **commented out** — `ChapterCrawlConsumer` not implemented yet |
| `CrawlRestResourceTest` | REST endpoint behavior (crawl trigger, retry, SSE subscribe) |
| `ComicGqlResourceTest` | GraphQL resolver unit tests |
| `ChapterGqlResourceTest` | GraphQL resolver unit tests |
| `TagGqlResourceTest` | GraphQL resolver unit tests |
| `HealthCheckResourceTest` | `/health` endpoint returns 200 with `{"status":"UP"}` |
| `FileTempClearerTest` | Scheduler: enabled/disabled toggle, age-based deletion logic |
| `ConvertersTest` | `CrawlStatusConverter`, `ImageTypeConverter` round-trips |
| `ExceptionsTest` | Custom exception constructors and fields |
| `GlobalExceptionMapperTest` | `ApiError` structure, `processId` is UUID |
| `UtilsTest` | `getExtensionFromUrl` edge cases |
| `PageRequestTest` | `toPage()`, `toSort()`, `getOffSet()` |
| `PageResponseTest` | Record construction |

Coverage reports: run `./gradlew test` → `build/reports/jacoco/test/html/index.html`.

---

## 21. CI/CD Pipeline

File: `.github/workflows/build-native.yml`

**Triggers:**
- Push to `main` branch
- Manual `workflow_dispatch` with optional `version` input

**Steps:**

1. **Checkout** — `actions/checkout@v4`
2. **Java 21** — `actions/setup-java@v4` (Temurin), with Gradle dependency cache
3. **Cache Gradle** — caches `~/.gradle/caches` and `~/.gradle/wrapper`
4. **Resolve version** — uses input `version` or falls back to `git rev-parse --short HEAD`
5. **GHCR Login** — `docker/login-action@v3`, credentials: `${{ github.actor }}` / `${{ secrets.GITHUB_TOKEN }}`
6. **Native Build** — `./gradlew build -Dquarkus.profile=prod -x test`
   - Activates prod profile: native compilation via Mandrel builder image (Docker-in-Docker)
   - Skips tests in CI
7. **Docker Metadata** — `docker/metadata-action@v5`
   - Image: `ghcr.io/proselin/warada_v2`
   - Tags: `{version}-native-micro`, `latest` (only on `main`)
8. **Build + Push** — `docker/build-push-action@v6`
   - Dockerfile: `src/main/docker/Dockerfile.native-micro`
   - Platform-specific native binary from `build/` directory

**Native Docker Image:**
- Base: `quay.io/quarkus/quarkus-micro-image:2.0` (minimal runtime, ~100MB)
- Copies `*-runner` binary
- Exposes port `8081`
- Runs as user `1001` (non-root)

---

## 22. Local Development Setup

### Prerequisites
- Docker Desktop or Docker Engine
- Java 21
- Gradle (or use `./gradlew` wrapper)

### Start Infrastructure
```bash
docker compose up -d postgres rabbitmq minio
```

### Start the App (Quarkus Dev Mode with live reload)
```bash
./gradlew quarkusDev
```
App runs at `http://localhost:8080`.

### Useful URLs
| URL | Description |
|---|---|
| `http://localhost:8080/q/graphql-ui` | GraphQL Playground (dev only) |
| `http://localhost:8080/graphql` | GraphQL API endpoint |
| `http://localhost:8080/api/v1/crawl/nettruyen/comic` | Crawl trigger (POST) |
| `http://localhost:8080/health` | App health check |
| `http://localhost:8080/q/health` | Quarkus health (SmallRye) |
| `http://localhost:9001` | MinIO Console |
| `http://localhost:15672` | RabbitMQ Management UI (guest/guest) |

### Trigger a Comic Crawl
```bash
curl -X POST http://localhost:8080/api/v1/crawl/nettruyen/comic \
  -H "Content-Type: application/json" \
  -d '{"slug-and-id": "truyen-tranh-slug-12345"}'
```

### Subscribe to Live Progress
```bash
curl -N http://localhost:8080/api/v1/crawl/progress/truyen-tranh-slug-12345 \
  -H "Accept: text/event-stream"
```

### Run Tests
```bash
./gradlew test
# Coverage report at: build/reports/jacoco/test/html/index.html
```

### Build Native Image (requires Docker)
```bash
./gradlew build -Dquarkus.profile=prod -x test
```

### DevContainer (Recommended)
1. Install Docker Desktop + VS Code with Dev Containers extension (or IntelliJ with DevContainer support)
2. Open project → "Reopen in Container"
3. Infrastructure starts automatically
4. Run `./gradlew quarkusDev` inside container

---

## 23. Known Issues & Gotchas

### 1. `existedBySlugAndComicId` has a runtime bug
`ComicRepository.existedBySlugAndComicId(String)` issues a Panache query against a field called `slugNComicId` which does not exist on the `Comic` entity. This would throw a `HibernateException` at runtime. However, the method is not called anywhere in the production flow — `CrawlServiceImpl` correctly uses `existsByOriginPathParams` instead. Safe for now but should be fixed or deleted.

### 2. `ChapterRepository.findBySlug` is misnamed
The method `findBySlug` actually queries `originPathParams`, not a `slug` field (chapters don't have a slug field). It works correctly but the naming is misleading.

### 3. `ChapterService.getChapterById` returns null instead of throwing
Unlike `ComicService.findById` which throws `ResourceNotFoundException`, `ChapterServiceImpl.getChapterById` returns `null` when not found. The GQL resolver (`ChapterGqlResource`) must null-check the result. Be consistent when adding new services.

### 4. MinIO "rename" can leave orphans
`renameObject` = copy + delete. If the delete call fails after a successful copy, you end up with both the temp and permanent objects. The `FileTempClearer` scheduler mitigates this by cleaning up old temp objects every few hours.

### 5. SSE is in-memory only
`CrawlProgressBroadcasterImpl` stores emitters in a `ConcurrentHashMap` local to the JVM. If the app runs as multiple instances (e.g., horizontal scaling), SSE events from one instance won't reach clients connected to another. This matches the single-instance nature of the current deployment.

### 6. Crawl endpoint is synchronous/blocking
`POST /api/v1/crawl/nettruyen/comic` blocks until all chapters are crawled. For a large comic with 200+ chapters, this HTTP connection stays open for a very long time. The SSE endpoint is the correct way for clients to monitor progress. Consider making the crawl async (fire-and-forget) in the future.

### 7. RabbitMQ is infrastructure-ready but code-incomplete
The docker-compose spins up RabbitMQ, and the event model classes exist, but no consumers/producers are implemented. The `ChapterCrawlConsumerTest` file has all tests commented out. Don't spend time debugging RabbitMQ connectivity — it's not used yet.

### 8. No authentication or authorization
All REST and GraphQL endpoints are publicly accessible. No Quarkus Security extension is in use. Any authorization must be added before production exposure.

### 9. Parallel image operations use `synchronized` list
In `NettruyenExtractorImpl.extractChapterDetail`, the parallel stream uses a `synchronized(imageInfos)` block to accumulate results into an `ArrayList`. This is a known workaround for parallel streams with mutable shared state. A cleaner approach would be using `Collectors.toList()` with a thread-safe approach or the stream's collect terminal operation.

### 10. README describes future architecture, not current code
The README talks about `:core-service:quarkusDev`, `:crawl-service:quarkusDev`, etc. — these subproject gradle targets don't exist. The monolith runs as `./gradlew quarkusDev` from root. The README is aspirational documentation for the planned refactor.
