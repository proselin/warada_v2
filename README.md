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

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts:
- PostgreSQL on port 5432
- MinIO on ports 9000 and 9001
- RabbitMQ on ports 5672 and 15672

### 2. Create the image bucket

Open the MinIO console at <http://localhost:9001> and create a bucket named `warada-images`.

Default local credentials:
- MinIO user: `admin`
- MinIO password: `admin123123123123`

### 3. Run the application

```bash
./gradlew quarkusDev
```

Useful local endpoints:
- App: <http://localhost:8080>
- GraphQL UI: <http://localhost:8080/q/graphql-ui>
- Dev UI: <http://localhost:8080/q/dev/>
- OpenAPI: <http://localhost:8080/q/openapi>
- RabbitMQ management: <http://localhost:15672>

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
