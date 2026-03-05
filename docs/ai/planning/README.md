---
phase: planning
title: Project Planning & Task Breakdown
description: Break down work into actionable tasks and estimate timeline
---

# Project Planning & Task Breakdown

## Milestones
**What are the major checkpoints?**

- [x] Milestone 1: **Core Infrastructure** — Database schema, Quarkus project setup, Docker Compose
- [x] Milestone 2: **Comic Crawling** — NetTruyen extraction, comic + thumbnail persistence
- [x] Milestone 3: **Chapter Crawling** — Chapter image download, MinIO integration, SSE streaming
- [x] Milestone 4: **GraphQL API** — Comic, chapter, and image queries with pagination
- [x] Milestone 5: **Operational Features** — Health checks, temp cleanup scheduler, OpenTelemetry
- [x] Milestone 6: **Native Build & Docker** — GraalVM native image, Dockerfiles, build scripts
- [ ] Milestone 7: **Production Hardening** — Auth, rate limiting, error handling improvements
- [ ] Milestone 8: **Multi-source Support** — Abstract extractor interface for additional comic sources

## Task Breakdown
**What specific work needs to be done?**

### Phase 1: Foundation ✅
- [x] Task 1.1: Initialize Quarkus project with Gradle
- [x] Task 1.2: Set up Docker Compose (PostgreSQL, MinIO)
- [x] Task 1.3: Create database schema and Liquibase migrations
- [x] Task 1.4: Define JPA entities (Comic, Chapter, Image)
- [x] Task 1.5: Implement Panache repositories
- [x] Task 1.6: Configure MinIO client (CDI producer, config mapping)

### Phase 2: Core Crawling ✅
- [x] Task 2.1: Implement NetTruyen REST client (`NettruyenRestService`)
- [x] Task 2.2: Build HTML/JSON-LD extractor (`NettruyenExtractorImpl`)
- [x] Task 2.3: Implement comic crawl workflow (`CrawlServiceImpl.crawlNettruyenComic`)
- [x] Task 2.4: Implement chapter crawl workflow (`CrawlServiceImpl.crawlChapter`)
- [x] Task 2.5: Implement image download with hotlink bypass headers
- [x] Task 2.6: Implement MinIO temp → permanent path rename workflow
- [x] Task 2.7: Add SSE streaming for batch chapter crawl

### Phase 3: API Layer ✅
- [x] Task 3.1: Implement GraphQL API for comics (list, get by ID)
- [x] Task 3.2: Implement GraphQL API for chapters (by ID, by slug, by comic ID)
- [x] Task 3.3: Define GraphQL schema files (comic, chapter, image)
- [x] Task 3.4: Implement REST crawl endpoints
- [x] Task 3.5: Add global exception mapper
- [x] Task 3.6: Add custom health check endpoint

### Phase 4: Operations ✅
- [x] Task 4.1: Implement FileTempClearer scheduler
- [x] Task 4.2: Configure OpenTelemetry (traces, metrics, logs)
- [x] Task 4.3: Configure SmallRye Health
- [x] Task 4.4: Set up production profile configuration

### Phase 5: Build & Deploy ✅
- [x] Task 5.1: Create native Dockerfiles (native, native-micro)
- [x] Task 5.2: Create build scripts (build-native, build-and-push-native)
- [x] Task 5.3: Configure GHCR image publishing

### Phase 6: Hardening 🔲
- [ ] Task 6.1: Add authentication/authorization
- [ ] Task 6.2: Implement rate limiting for crawl operations
- [ ] Task 6.3: Add input validation for REST/GraphQL inputs
- [ ] Task 6.4: Improve error handling in NettruyenExtractor (retry logic)
- [ ] Task 6.5: Add caching for frequently accessed comics/chapters
- [ ] Task 6.6: Complete ImageGqlResource with queries

## Dependencies
**What needs to happen in what order?**

```
PostgreSQL + MinIO (Docker Compose)
    └── DB Schema (Liquibase migrations)
        └── JPA Entities + Repositories
            ├── NetTruyen Extractor
            │   └── CrawlService
            │       ├── REST API (Crawl endpoints)
            │       └── SSE Streaming
            ├── ComicService + ChapterService
            │   └── GraphQL API
            └── FileTempClearer (Scheduler)
```

### External Dependencies
- NetTruyen website availability and HTML structure consistency
- MinIO bucket `warada-images` must exist before crawling
- Docker/GHCR access for container image publishing

## Timeline & Estimates
**Current status as of March 2026:**

| Phase | Status | Effort |
|---|---|---|
| Foundation | ✅ Complete | — |
| Core Crawling | ✅ Complete | — |
| API Layer | ✅ Complete | — |
| Operations | ✅ Complete | — |
| Build & Deploy | ✅ Complete | — |
| Hardening | 🔲 Not started | Est. 2-3 weeks |

## Risks & Mitigation
**What could go wrong?**

| Risk | Impact | Mitigation |
|---|---|---|
| NetTruyen changes HTML structure | Crawling breaks entirely | Monitor crawl errors; modular extractor pattern allows quick fixes |
| NetTruyen blocks IP/rate limits | Images fail to download | Add retry logic, delays between requests, proxy rotation |
| MinIO storage runs out | New images can't be saved | Monitor disk usage; temp cleanup scheduler reduces waste |
| Native image build breaks | Can't deploy to production | Maintain JVM Dockerfile as fallback |
| Database migration conflicts | App fails to start | Test migrations in staging; maintain rollback scripts |

## Resources Needed
**What do we need to succeed?**

### Infrastructure
- PostgreSQL 16 instance
- MinIO instance with `warada-images` bucket
- Docker registry (GHCR at `ghcr.io/proselin/warada_v2`)
- Optional: OpenTelemetry Collector for observability

### Tools
- Java 21 / GraalVM for native builds
- Docker for containerization
- Gradle (wrapper included)
