# Warada v2

A comic/manga aggregation backend service built with **Quarkus** (Java 21). It crawls comic data and images from NetTruyen, stores them in PostgreSQL and MinIO, and exposes them via **GraphQL** and **REST** APIs.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | [Quarkus](https://quarkus.io/) 3.28.1 |
| Language | Java 21 |
| Build | Gradle (wrapper included) |
| Database | PostgreSQL 16 |
| Object Storage | MinIO |
| API | SmallRye GraphQL + RESTEasy Reactive |
| Migrations | Liquibase |
| Observability | OpenTelemetry |
| Container | GraalVM Native Image + Docker |

## Features

- 🕷️ **Crawl comics** from NetTruyen (metadata, thumbnails, chapters, images)
- 📡 **SSE streaming** for real-time crawl progress
- 📊 **GraphQL API** for flexible comic/chapter/image queries with pagination
- 🗄️ **MinIO image storage** with automatic temp file cleanup
- 🏥 **Health checks** (SmallRye Health + custom endpoints)
- 📈 **OpenTelemetry** traces, metrics, and logs (production)
- 🐳 **Native image** Docker builds for minimal footprint

## Quick Start

### Prerequisites
- Java 21
- Docker & Docker Compose

### 1. Start infrastructure
```bash
docker compose up -d
```
This starts PostgreSQL (port 5432) and MinIO (port 9000/9001).

### 2. Create MinIO bucket
Open MinIO Console at `http://localhost:9001` (login: `admin` / `admin123123123123`) and create bucket `warada-images`.

### 3. Run in dev mode
```bash
./gradlew quarkusDev
```

### 4. Access the app
| URL | Description |
|---|---|
| `http://localhost:8080/q/graphql-ui` | GraphQL UI |
| `http://localhost:8080/q/dev/` | Quarkus Dev UI |
| `http://localhost:8080/q/openapi` | OpenAPI spec |
| `http://localhost:8080/q/health` | Health checks |
| `http://localhost:8080/zhealth/ping` | Custom ping |

## API Overview

### GraphQL (at `/graphql`)
```graphql
{ comics(pageRequest: {page: 0, size: 10}) { id title slug author } }
{ comic(id: 1) { id title chapters { id chapterNumber } } }
{ detailChapterById(id: 1) { id chapterNumber images { id fileName } } }
```

### REST (at `/api/v1/crawl`)
```bash
# Crawl a comic
POST /api/v1/crawl/nettruyen/comic  {"slugNId": "comic-slug-123"}

# Crawl a chapter
POST /api/v1/crawl/nettruyen/chapter  {"chapterId": 1}

# SSE crawl all chapters
GET /api/v1/crawl/nettruyen/sse/comic?slug-n-id=comic-slug-123
```

## Building

### JVM
```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

### Native Image
```bash
# Container-based native build (no GraalVM required locally)
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true

# Build Docker image
./scripts/build-native.sh <version>

# Build and push to GHCR
./scripts/build-and-push-native.sh <version>
```

## Project Structure
```
src/main/java/com/github/warada_v2/
├── commons/          # Utilities, constants, enums
├── configurations/   # Config mappings (MinIO, Warada)
├── converter/        # JPA attribute converters
├── db_entities/      # JPA entities (Comic, Chapter, Image)
├── exceptions/       # Custom exceptions
├── mapper/           # Global exception mapper
├── models/           # DTOs and request/response records
├── repositories/     # Panache repositories
├── resources/        # API endpoints (GraphQL + REST)
├── rest/client/      # MicroProfile REST clients
├── scheduler/        # Scheduled jobs
└── services/         # Business logic
```

## Documentation
Detailed docs available in `docs/ai/`:
- [Requirements](docs/ai/requirements/README.md)
- [Design & Architecture](docs/ai/design/README.md)
- [Implementation Guide](docs/ai/implementation/README.md)
- [Planning & Tasks](docs/ai/planning/README.md)
- [Deployment](docs/ai/deployment/README.md)
- [Monitoring](docs/ai/monitoring/README.md)
- [Testing](docs/ai/testing/README.md)

## License
Private project — `ghcr.io/proselin/warada_v2`
