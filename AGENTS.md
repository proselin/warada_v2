# AGENTS.md

## Project

**warada_v2** — Quarkus backend that crawls manga/comic content from NetTruyen, stores metadata in PostgreSQL, stores images in MinIO (S3-compatible), and exposes the data via a **GraphQL API** (reads) and a **REST API** (crawl triggers + SSE progress).

## Commands

```bash
# Start infrastructure (PostgreSQL + MinIO)
docker compose up -d

# Dev mode with live reload (port 8080)
./gradlew quarkusDev

# Run all tests
./gradlew test

# Run a single test
./gradlew test --tests "com.github.warada_v2.ExampleResourceTest.testPingPong"

# Build JVM JAR
./gradlew build

# Build native image (requires Docker)
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true

# Build native image for production
./gradlew buildProdNative
```

## Folder Structure

```
src/main/java/com/github/warada_v2/
├── api/            # GraphQL & REST read resources, DTOs
├── crawl/          # Crawl domain: client, models, resources, scheduler, services
└── shared/         # Entities, repositories, configurations, exceptions, converters, enums

src/main/resources/
├── application.yaml       # Config (%dev/%prod/%test-native profiles)
├── db/migration/          # Liquibase changesets
└── graphql/                # GraphQL schema files

src/test/java/...          # Tests mirror src/main/java package layout

docs/
├── ai/<phase>/feature-{name}.md  # Per-feature docs: requirements, design, planning,
│                                  # implementation, testing, deployment, monitoring, snapshots
├── superpowers/            # Specs/plans for larger workflows
├── API_REFERENCE.md
├── CURRENT_STATE_REVIEW.md
└── PROJECT_LEARNING.md
```

See [.github/copilot-instructions.md](.github/copilot-instructions.md) for full conventions (naming, DI, transactions, exception handling, crawl workflow, documentation workflow).
