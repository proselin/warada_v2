# warada_v2

Warada v2 is a Quarkus-based backend for crawling manga/comic metadata and chapter images from NetTruyen, storing metadata in PostgreSQL, and serving images from MinIO.

## Architecture

The project is three independent Quarkus microservices that communicate via RabbitMQ, plus a shared Java library:

```
warada_v2/
├── src/
│   ├── apps/
│   │   ├── core-service/     # PostgreSQL persistence + Liquibase (port 8082)
│   │   ├── crawl-service/    # NetTruyen scraping + MinIO + RabbitMQ producers (port 8081)
│   │   └── display-service/  # GraphQL read API (port 8080)
│   └── libs/
│       └── shared-lib/       # JPA entities, RabbitMQ events, enums, exceptions, DTOs
├── scripts/                  # Per-service build-and-push shell scripts
├── .devcontainer/            # VS Code / JetBrains devcontainer setup
└── docker-compose.yml        # Local infrastructure (PostgreSQL, RabbitMQ, MinIO)
```

### Event Flow

```
crawl-service  →[ComicExtractedEvent]→         core-service
core-service   →[ChapterPendingEvent]→         crawl-service
crawl-service  →[ChapterImagesExtractedEvent]→ core-service
display-service reads PostgreSQL directly (read-only)
```

## Main Features

- Crawl comic metadata, tags, thumbnail, and chapters from NetTruyen
- Event-driven chapter image crawling through RabbitMQ
- Stream live crawl progress to clients with Server-Sent Events (SSE)
- Browse comics and chapters through GraphQL (read-only queries)
- Trigger crawl operations through REST endpoints

## Tech Stack

- Java 21 · Quarkus 3.28.1
- PostgreSQL 16 · Hibernate ORM Panache · Liquibase
- MinIO (S3-compatible image storage, bucket: `warada-images`)
- RabbitMQ 4 · SmallRye Reactive Messaging
- SmallRye GraphQL · JAX-RS REST · SSE
- OpenTelemetry (traces, metrics, logs)

## Local Development

### Infrastructure

Start PostgreSQL, RabbitMQ, and MinIO:

```bash
docker compose up -d postgres rabbitmq minio
```

MinIO first-time setup: open `http://localhost:9001`, login with `admin` / `admin123123123123`, create a bucket named `warada-images`.

### Run individual services (dev mode with live reload)

```bash
./gradlew :core-service:quarkusDev     # http://localhost:8082
./gradlew :crawl-service:quarkusDev   # http://localhost:8081
./gradlew :display-service:quarkusDev # http://localhost:8080
```

### Useful endpoints

| URL | Description |
|-----|-------------|
| `http://localhost:8080/graphql` | GraphQL API |
| `http://localhost:8080/q/graphql-ui` | GraphQL UI (dev only) |
| `http://localhost:8081/api/v1/crawl/nettruyen/comic` | Trigger comic crawl (POST) |
| `http://localhost:8081/api/v1/crawl/nettruyen/comic/{slug}/retry` | Retry pending chapters (POST) |
| `http://localhost:8081/api/v1/crawl/progress/{slug}` | SSE crawl progress (GET) |
| `http://localhost:8081/q/health` | Crawl-service health |
| `http://localhost:8082/q/health` | Core-service health |
| `http://localhost:15672` | RabbitMQ management UI (guest/guest) |
| `http://localhost:9001` | MinIO console (admin/admin123123123123) |

### Preferred workflow: devcontainer

1. Install Docker Desktop or Docker Engine.
2. Open in VS Code → **Dev Containers: Reopen in Container**, or open in IntelliJ with Dev Container support.
3. All infrastructure env vars (`DEV_DB_URL`, `DEV_RABBITMQ_HOST`, `DEV_MINIO_HOST`, etc.) are pre-injected by `.devcontainer/devcontainer.json`.
4. Inside the container, run any service with `./gradlew :<service>:quarkusDev`.

## Build

```bash
# Build all modules (skip tests)
./gradlew build -x test

# Build a specific service
./gradlew :crawl-service:build -x test

# Build native images for all services (production, requires Docker)
./gradlew buildProdNative -Dquarkus.profile=prod

# Build and push a single service native image
./scripts/crawl-service/build-and-push.sh <version>

# Build and push all services
./scripts/build-all-native.sh <version>
```

Native images are published to `ghcr.io/proselin/warada_v2/<service>:<version>-native-micro`. The CI workflow (`.github/workflows/build-native.yml`) builds and pushes all three services automatically on every push to `main`.

## Test

```bash
./gradlew test
```

## Async Crawl Flow

### 1. Trigger a comic crawl

```http
POST /api/v1/crawl/nettruyen/comic
Content-Type: application/json

{ "slug-and-id": "one-piece-12345" }
```

`crawl-service` scrapes NetTruyen, uploads the thumbnail to MinIO, then publishes a `ComicExtractedEvent` to RabbitMQ. `core-service` persists the comic and publishes a `ChapterPendingEvent` for every chapter. `crawl-service` consumes each event, downloads chapter images to MinIO, and publishes a `ChapterImagesExtractedEvent`. `core-service` marks each chapter `FINISHED`.

### 2. Subscribe to live crawl progress (SSE)

```http
GET /api/v1/crawl/progress/{comicSlug}
Accept: text/event-stream
```

Each SSE payload is a `CrawlProgressEvent` JSON object:

```json
{ "type": "completed", "message": "Chapter crawled successfully", "data": { "chapterId": 10, "chapterNumber": "10", "status": null, "comicSlug": "one-piece-12345" } }
```

Supported event `type` values: `processing` · `completed` · `failed` · `skipped`

### 3. Retry pending chapters

```http
POST /api/v1/crawl/nettruyen/comic/{slug}/retry
```

## Notes

- SSE progress is in-memory per `crawl-service` instance
- Liquibase migrations run only in `core-service`; `display-service` connects read-only
- Chapter crawl concurrency is configurable via `mp.messaging.incoming.chapter-pending-in.concurrency` in `crawl-service/src/main/resources/application.properties`
- The GraphQL API is currently **read-only** (queries only; all `Mutation` blocks are empty placeholders)
