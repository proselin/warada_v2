# Current State Review

> Last reviewed: 2026-08-28  
> Branch: `main` (HEAD `04d8dc2`)  
> Reviewed by: Developer onboarding audit

---

## Summary

**warada_v2** is a single Quarkus monolith that crawls manga/comic data from NetTruyen, stores images in MinIO, persists metadata in PostgreSQL, and exposes the data via a GraphQL API and REST endpoints. The service is functionally complete for its core use case.

The codebase previously went through a period of experimentation with a three-service architecture over RabbitMQ. That direction has been **officially abandoned**. RabbitMQ is no longer part of the application — no dependency, no consumer, no producer. The service is one, and it stays one.

---

## Architecture: One Service

The application is a **single Quarkus monolith** running on port `8080`. There are no submodules, no subprojects in Gradle, and no inter-service communication. Everything lives in one process.

```
warada_v2 (single Quarkus app, port 8080)
│
├── Crawl layer      → scrapes NetTruyen, downloads images to MinIO
├── Persistence layer → PostgreSQL via Hibernate ORM Panache
└── API layer        → GraphQL (/graphql) + REST (/api/v1/crawl/*)
```

**Infrastructure dependencies (external processes, not application modules):**
- PostgreSQL 16 — primary data store
- MinIO — image object storage

That is all. Nothing else.

---

## What Works (Feature-Complete)

### Crawl Pipeline
- `POST /api/v1/crawl/nettruyen/comic` — full comic crawl: scrapes NetTruyen HTML, extracts JSON-LD metadata, downloads thumbnail, persists `Comic` + stub `Chapter` entities, then crawls every chapter's images
- `POST /api/v1/crawl/nettruyen/comic/{slug}/retry` — re-crawls all chapters still in `INIT` status for a given comic slug
- `GET /api/v1/crawl/progress/{comicSlug}` — SSE stream delivering live chapter-by-chapter progress events
- Duplicate detection — returns `409 Conflict` if comic's `originPathParams` already exists

### GraphQL API (`/graphql`)
- `comics(pageRequest)` — paginated comic list
- `comic(id)` — single comic by id
- `chapters(comicId, pageRequest)` — paginated chapters for a comic
- `detailChapterById(id)` — full chapter detail with images
- `detailChapterBySlug(slug)` — chapter detail by path params
- `tags` — all tags sorted by usage count descending
- `tag(id)` — single tag by id
- `ping` — liveness probe

### Data Model
- **Comics**: slug, title, author, description, status, chapter count, crawl status, thumbnail
- **Chapters**: chapter number, position, crawl status, origin URL
- **Images**: file name, file path (MinIO), position, type (thumb or chapter page)
- **Tags**: normalized name, comic count (denormalized)
- Many-to-many `comic_tags` junction table

### Infrastructure Integration
- **MinIO**: bucket auto-created on startup via `MinioBucketInitializer`. Two-phase write: temp path → permanent path. `FileTempClearer` scheduler purges stale temp objects at 00:00, 07:00, 14:00, 19:00.
- **Liquibase**: 3 migrations run automatically on startup. Schema is stable.
- **OpenTelemetry**: enabled in prod profile, disabled in dev.
- **SmallRye Health**: `/q/health` endpoint active.
- **OpenAPI**: `/q/openapi` available.

---

## What Is Dead Code / Cleanup Needed

### 1. Event classes with no callers

The following three classes in `shared/models/events/` are **unused**. They were designed for a RabbitMQ-based microservice split that was cancelled. Nothing imports or references them outside their own package.

| Class | Status |
|---|---|
| `ComicExtractedEvent` | Dead — no producer, no consumer |
| `ChapterPendingEvent` | Dead — no producer, no consumer |
| `ChapterImagesExtractedEvent` | Dead — no producer, no consumer |

**Recommendation:** Delete all three. They add noise and mislead future developers about what the system does.

### 2. `ChapterCrawlConsumerTest` is a zombie test file

`src/test/java/.../services/ChapterCrawlConsumerTest.java` exists but every `@Test` method and the `@InjectMocks` target are commented out. The class it was testing (`ChapterCrawlConsumer`) never existed in the active codebase.

**Recommendation:** Delete this file.

### 3. RabbitMQ in `docker-compose.yml`

RabbitMQ is still defined as a live service in `docker-compose.yml` (ports 5672/15672, with health check). Since it is completely removed from the application, running `docker compose up` still spins it up unnecessarily.

**Recommendation:** Remove the `rabbitmq` service block and the `rabbitmq-data` volume from `docker-compose.yml`.

### 4. `ComicRepository.existedBySlugAndComicId` bug

This method queries a Panache field named `slugNComicId` which does not exist on the `Comic` entity. It would throw a `HibernateException` at runtime. It is currently unused in any code path — `CrawlServiceImpl` uses `existsByOriginPathParams` instead.

**Recommendation:** Delete this method.

### 5. README describes a multi-service architecture that no longer applies

The `README.md` shows Gradle subproject commands like `./gradlew :crawl-service:quarkusDev` and describes a three-service layout with RabbitMQ event flows. None of this reflects reality.

**Recommendation:** Rewrite `README.md` to describe the actual single-service setup.

---

## Known Limitations

| Area | Limitation |
|---|---|
| Security | No authentication or authorization on any endpoint. All REST and GraphQL routes are public. |
| Crawl blocking | `POST /api/v1/crawl/nettruyen/comic` is synchronous — it blocks the HTTP thread for the entire crawl duration. Large comics with 200+ chapters can hold the connection open for a long time. |
| SSE in-memory | `CrawlProgressBroadcasterImpl` stores emitters in a `ConcurrentHashMap` in-process. Not compatible with multi-instance deployments. |
| No concurrency cap | Chapter image downloads use raw `parallelStream()` with no thread pool limit or back-pressure. |
| Scraper coupling | `NettruyenExtractorImpl` is tightly coupled to the HTML structure of `nettruyenar.com`. Any site redesign will break crawling. |
| `ChapterService` null returns | `ChapterServiceImpl.getChapterById` and `getChapterBySlug` return `null` instead of throwing `ResourceNotFoundException`, inconsistent with `ComicService`. |

---

## Dependency Snapshot (as of main HEAD)

| Component | Version |
|---|---|
| Java | 21 |
| Quarkus | 3.38.2 |
| PostgreSQL | 16 |
| MinIO client | quarkiverse-minio 3.9.1 |
| Gradle | Wrapper |
| Lombok | **Removed** (see Unreleased in RELEASE_NOTE.md) |

No RabbitMQ client dependency. No SmallRye Reactive Messaging dependency. No reactive PostgreSQL client. The stack is intentionally synchronous/imperative.

---

## Local Dev Quick Reference

```bash
# Start required infrastructure only (postgres + minio)
docker compose up -d postgres minio

# Run the app with live reload
./gradlew quarkusDev

# Run tests with coverage
./gradlew test
# → report: build/reports/jacoco/test/html/index.html

# Build native image (requires Docker)
./gradlew build -Dquarkus.profile=prod -x test
```

Key URLs when running locally:
| URL | Purpose |
|---|---|
| `http://localhost:8080/q/graphql-ui` | GraphQL Playground |
| `http://localhost:8080/api/v1/crawl/nettruyen/comic` | Crawl trigger (POST) |
| `http://localhost:8080/api/v1/crawl/progress/{slug}` | SSE progress stream |
| `http://localhost:8080/q/health` | Health check |
| `http://localhost:9001` | MinIO console |

---

## Recommended Next Steps

1. **Delete dead code** — remove the three event classes, `ChapterCrawlConsumerTest`, and the broken `existedBySlugAndComicId` method.
2. **Clean up docker-compose** — remove the `rabbitmq` service and volume.
3. **Rewrite README** — align it with the actual single-service reality.
4. **Add authentication** — at minimum, an API key or JWT on the crawl-trigger endpoints.
5. **Make crawl async** — fire-and-forget the crawl job so the POST returns immediately; clients already use SSE for progress.
6. **Fix `ChapterService` null returns** — throw `ResourceNotFoundException` consistently.
