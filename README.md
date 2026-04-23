# warada_v2

Warada v2 is a Quarkus-based backend for crawling manga/comic metadata and chapter images from NetTruyen, storing metadata in PostgreSQL, and serving images from MinIO.

## Architecture

The project is decomposed into three independent Quarkus microservices that communicate via RabbitMQ, plus a shared Java library:

```
warada_v2/
├── src/
│   ├── apps/
│   │   ├── core-service/     # PostgreSQL persistence (port 8082)
│   │   ├── crawl-service/    # NetTruyen scraping + MinIO (port 8081)
│   │   └── display-service/  # GraphQL read API (port 8080)
│   └── libs/
│       └── shared-lib/       # JPA entities, events, enums, exceptions
├── legacy/
│   └── src/                  # Original monolith (reference only)
└── docker-compose.yml
```

### Event Flow

```
crawl-service  →[ComicExtractedEvent]→        core-service
core-service   →[ChapterPendingEvent]→        crawl-service
crawl-service  →[ChapterImagesExtractedEvent]→ core-service
display-service reads PostgreSQL directly (read-only)
```

## Main Features

- Crawl comic metadata, tags, thumbnail, and chapters from NetTruyen
- Event-driven chapter image crawling through RabbitMQ
- Stream live crawl progress to clients with Server-Sent Events (SSE)
- Browse comics and chapters through GraphQL
- Trigger crawl operations through REST endpoints

## Tech Stack

- Java 21 · Quarkus 3
- PostgreSQL 16 · Hibernate ORM Panache · Liquibase
- MinIO (S3-compatible image storage)
- RabbitMQ · SmallRye Reactive Messaging
- GraphQL (SmallRye) · REST (JAX-RS) · SSE

## Local Development

### Infrastructure

Start PostgreSQL, RabbitMQ, and MinIO:

```bash
docker compose up -d postgres rabbitmq minio
# or via devcontainer:
docker compose -f .devcontainer/docker-compose.yml up -d postgres rabbitmq minio
```

### Run individual services (dev mode with live reload)

```bash
./gradlew :core-service:quarkusDev     # http://localhost:8082
./gradlew :crawl-service:quarkusDev   # http://localhost:8081
./gradlew :display-service:quarkusDev # http://localhost:8080
```

### Useful endpoints (display-service / crawl-service)

| URL | Description |
|-----|-------------|
| `http://localhost:8080/graphql` | GraphQL API |
| `http://localhost:8080/q/graphql-ui` | GraphQL UI (dev only) |
| `http://localhost:8081/api/v1/crawl/nettruyen/comic` | Trigger comic crawl (POST) |
| `http://localhost:8081/q/health` | Crawl-service health |
| `http://localhost:15672` | RabbitMQ management UI |
| `http://localhost:9001` | MinIO console |

### Preferred workflow: devcontainer

1. Install Docker Desktop or Docker Engine.
2. Open in VS Code → **Dev Containers: Reopen in Container**, or open in IntelliJ with Dev Container support.
3. Inside the container, run any service with `./gradlew :<service>:quarkusDev`.

## Build

```bash
# Build all modules
./gradlew build -x test

# Build a specific service
./gradlew :crawl-service:build -x test

# Build native image (requires Docker)
./gradlew :crawl-service:build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

## Test

```bash
./gradlew test
```

## Async Crawl Flow

### 1. Trigger a comic crawl

```http
POST /api/v1/crawl/nettruyen/comic
Content-Type: application/json

{ "slugNId": "one-piece-12345" }
```

This publishes a `ComicExtractedEvent` to RabbitMQ. `core-service` persists the comic and publishes `ChapterPendingEvent` for each chapter. `crawl-service` downloads images and publishes `ChapterImagesExtractedEvent`. `core-service` finalises each chapter as `FINISHED`.

### 2. Subscribe to live crawl progress (SSE)

```http
GET /sse/comic/{comicSlug}
Accept: text/event-stream
```

Supported event types: `processing` · `completed` · `failed` · `skipped`

### 3. Retry pending chapters

```http
POST /api/v1/crawl/nettruyen/comic/{slug}/retry
```

## Notes

- SSE progress is in-memory per crawl-service instance
- Liquibase migrations run only in `core-service`; `display-service` connects read-only
- Chapter crawl concurrency is configurable via RabbitMQ consumer settings in `crawl-service/src/main/resources/application.properties`
