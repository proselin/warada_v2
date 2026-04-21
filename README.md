# warada_v2

Warada v2 is a Quarkus-based backend for crawling manga/comic metadata and chapter images from NetTruyen, storing metadata in PostgreSQL, and serving images from MinIO.

## Main Features

- Crawl comic metadata, tags, thumbnail, and chapters from NetTruyen
- Queue chapter image crawling through RabbitMQ
- Stream live crawl progress to frontend clients with Server-Sent Events
- Browse comics and chapters through GraphQL
- Manage comics and chapters through REST endpoints

## Tech Stack

- Java 21
- Quarkus 3
- PostgreSQL 16
- Hibernate ORM Panache
- Liquibase
- MinIO
- RabbitMQ
- GraphQL + REST + SSE

## Local Development

### Preferred workflow: devcontainer + Docker Compose

This repository now prefers a shared devcontainer setup for daily development instead of auto-starting Quarkus Dev Services.
The same setup can be used from both VS Code and IntelliJ IDEA.

#### Option 1: VS Code

1. Install Docker Desktop or Docker Engine with Compose support.
2. Open the repository in VS Code.
3. Choose **Dev Containers: Reopen in Container**.
4. Inside the container, start the app:

```bash
./gradlew quarkusDev
```

#### Option 2: IntelliJ IDEA

1. Use IntelliJ IDEA with Dev Container support enabled.
2. Open the repository and select the existing devcontainer configuration from the project.
3. Start the workspace container.
4. Run the backend from the container terminal or Gradle tool window:

```bash
./gradlew quarkusDev
```

#### Manual fallback without a devcontainer

You can still run the local services first and keep the app on the host machine:

```bash
docker compose -f .devcontainer/docker-compose.yml up -d postgres rabbitmq minio
./gradlew quarkusDev
```

If a host port is already in use, create a local [.devcontainer/.env.example](.devcontainer/.env.example) copy named .devcontainer/.env and adjust the exposed ports.

Useful local endpoints:
- App: <http://localhost:8080>
- GraphQL UI: <http://localhost:8080/q/graphql-ui>
- Dev UI: <http://localhost:8080/q/dev/>
- OpenAPI: <http://localhost:8080/q/openapi>
- RabbitMQ UI: <http://localhost:15672>
- MinIO Console: <http://localhost:9001>

> The MinIO bucket is still initialized automatically by the application at startup.
>
> The previous Dev Services properties are intentionally kept as comments in the app config so newer developers can learn the difference between both approaches.

## Async Crawl Flow

### Crawl a comic

```http
POST /api/v1/crawl/nettruyen/comic
Content-Type: application/json

{
  "slugNId": "one-piece-12345"
}
```

This will:
1. Crawl comic metadata and thumbnail
2. Create chapter records with `INIT` status
3. Publish chapter crawl jobs to RabbitMQ

### Subscribe to live progress

```http
GET /api/v1/crawl/progress/{comicSlug}
Accept: text/event-stream
```

Example SSE payload:

```json
{
  "type": "completed",
  "message": "Chapter crawled successfully",
  "data": {
    "chapterId": 10,
    "chapterNumber": "10",
    "status": "FINISHED",
    "comicSlug": "one-piece-12345"
  }
}
```

Supported event types:
- `processing`
- `completed`
- `failed`
- `skipped`

### Retry pending chapters

```http
POST /api/v1/crawl/nettruyen/comic/{slug}/retry
```

If the comic already has an active crawl job in the queue, the retry request is blocked with `409 Conflict`.

## Build and Test

```bash
./gradlew build
./gradlew test
```

Build a native image:

```bash
./gradlew buildProdNative
```

## Notes

- SSE progress is currently in-memory per application instance
- Frontend clients can subscribe at any time and receive live events from that point onward
- Chapter crawling concurrency is configurable through the RabbitMQ consumer settings in the application config
