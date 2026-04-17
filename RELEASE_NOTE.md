# Release Notes

---

## Unreleased

### Improvements
- Added feature lomnox-170426 to remove Lombok from the codebase in phased refactors.
- Replaced Lombok-based logging annotations with explicit JBoss Logger usage in Quarkus resource, service, messaging, and scheduler classes.
- Removed Lombok from low-risk enums, exception handling, helper models, and pageable DTO classes.
- Converted the remaining ORM entities to explicit Java while preserving JPA behavior and existing builder-style call paths.
- Removed the Lombok dependency from the Gradle build configuration.

---

## v1.0.0 — 2026-03-10

### Overview

First stable release of **warada_v2** — a Quarkus-based manga/comic crawler and management backend. This release establishes the core crawl pipeline, GraphQL/REST API surface, image storage integration, and observability foundation.

---

### Features

#### Crawl Pipeline
- **Comic crawl** (`POST /api/v1/crawl/nettruyen/comic`): Scrapes comic metadata from NetTruyen by `slug-n-id`, downloads the thumbnail to MinIO, and persists `Comic`, `Image`, and stub `Chapter` entities with `CrawlStatus.INIT`.
- **Chapter crawl** (`POST /api/v1/crawl/nettruyen/chapter`): Processes a single chapter by ID, downloads all page images in parallel to MinIO, and advances its status to `CrawlStatus.FINISHED`.
- **SSE streaming crawl** (`GET /api/v1/crawl/nettruyen/sse/comic`): Streams chapter crawl progress in real time as Server-Sent Events, emitting `ADD` or `IGNORE` events per chapter.
- Duplicate detection via comic slug — returns `409 Conflict` if comic already exists.
- Two-phase MinIO image write: temp path → permanent path (`<slug>/chapters/<num>/<file>` or `<slug>/thumb/<file>`).

#### GraphQL API (`/graphql`)
- `comics(pageRequest)` — paginated list of comics.
- `comic(id)` — fetch single comic by ID.
- `chapters(comicId, pageRequest)` — paginated chapters for a comic.
- `detailChapterById(id)` / `detailChapterBySlug(slug)` — full chapter detail with image list.
- `tags` / `tag` — list and fetch tags.
- GraphQL UI available at `/q/graphql-ui` in dev mode.

#### REST API (`/api/v1`)
- `POST /api/v1/crawl/nettruyen/comic` — trigger individual comic crawl.
- `POST /api/v1/crawl/nettruyen/chapter` — trigger individual chapter crawl.
- `GET /api/v1/crawl/nettruyen/sse/comic?slug-n-id=<slug>` — SSE stream for full comic chapter crawl.
- OpenAPI docs available at `/q/openapi`.

#### Database
- PostgreSQL 16 with Liquibase-managed migrations applied at startup.
- Schema: `comics`, `chapters`, `images`, `tags` tables with proper relationships.
- Changesets: initial schema, unique-constraint removal, tags table addition.

#### Image Storage
- MinIO (S3-compatible) integration via `quarkus-minio-native:3.8.6`.
- Bucket: `warada-images`.
- `FileTempClearer` scheduler: purges stale temp objects older than the configured `warada.clean-up.min-age-minutes` interval.

#### Observability
- OpenTelemetry traces and metrics enabled.
- SmallRye Health endpoints at `/q/health`.
- Prometheus/Micrometer metrics exposed at `/q/metrics`.
- GraphQL request/variable logging available for debugging.

#### Infrastructure
- Docker Compose setup for local development (PostgreSQL + MinIO).
- Quarkus native image build support via `./gradlew build -Dquarkus.native.enabled=true`.
- Dedicated build scripts for native and native-micro images (`scripts/`).

---

### Tech Stack

| Component | Version |
|---|---|
| Quarkus | 3.28.1 |
| Java | 21 |
| PostgreSQL | 16 |
| MinIO client | quarkus-minio-native 3.8.6 |
| Lombok | 1.18.42 |
| Gradle | (wrapper) |

---

### Known Limitations

- This is a hobby/side project. Not production-hardened.
- NetTruyen scraper is tightly coupled to the current HTML structure of `nettruyenar.com`.
- No authentication or authorization on any API endpoint.
- Chapter image downloads use `parallelStream` — no concurrency cap or back-pressure control.

---

### Migration / Setup Notes

1. Start infrastructure: `docker compose up -d`
2. Create MinIO bucket `warada-images` via the MinIO console at `http://localhost:9001` (credentials: `admin` / `admin123123123123`).
3. Run the application: `./gradlew quarkusDev`
4. Liquibase migrations run automatically on startup.
