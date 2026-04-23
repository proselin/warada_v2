---
phase: requirements
title: "Refactor: Separate Module Architecture"
description: Requirements for decomposing warada_v2 monolith into independent microservices communicating via RabbitMQ
---

# Requirements: Refactor — Separate Module Architecture

## Problem Statement

The current `warada_v2` application is a monolith that combines **crawling**, **data persistence**, and **display** concerns into a single Quarkus process. While this works today, it creates coupling problems:

- Crawl load directly impacts GraphQL API latency (same JVM, same thread pool)
- Cannot scale crawling independently from the display layer
- Any crash or deploy requires full-app restart, affecting all users
- Crawl logic, data ownership, and read API are entangled in `CrawlServiceImpl`

## Goals

1. **Separation of concerns** — Each service owns one domain: crawl, persist, or serve.
2. **Async decoupling via RabbitMQ** — Services communicate through events; no direct synchronous calls between crawl and core paths.
3. **Independent scalability** — Run more crawl workers without scaling the GraphQL API tier.
4. **Independent deployability** — Deploy or restart one service without affecting others.
5. **Shared contract** — A shared library (`shared-lib`) holds JPA entities, event message models, enums, and common utilities so all services use the same schema.

## Non-Goals

- This refactor does NOT change the external API surface (GraphQL schema, REST endpoints stay the same).
- This refactor does NOT change the database schema.
- This refactor does NOT add new features (auth, rate limiting, etc.) — those belong to a separate hardening milestone.

## Modules Required

| Module | Type | Responsibility |
|---|---|---|
| `shared-lib` | Java library | Shared entities, event models, enums, exceptions |
| `crawl-service` | Quarkus app | Scrape NetTruyen, upload images to MinIO, publish events |
| `core-service` | Quarkus app | Consume events, persist to PostgreSQL, manage entity state |
| `display-service` | Quarkus app | Serve GraphQL read API to end users |

## Functional Requirements

### crawl-service
- FR-C1: Expose `POST /api/v1/crawl/nettruyen/comic` to trigger comic crawl.
- FR-C2: Expose `GET /sse/comic` for real-time chapter crawl progress via SSE.
- FR-C3: Publish a `ComicExtractedEvent` to RabbitMQ after extracting comic metadata.
- FR-C4: Consume `ChapterPendingEvent` from RabbitMQ to know which chapters need image download.
- FR-C5: Download chapter images and upload to MinIO (temp → permanent rename).
- FR-C6: Publish a `ChapterImagesExtractedEvent` to RabbitMQ after all images are uploaded.
- FR-C7: Call `core-service` REST endpoint to check for comic duplicate before starting crawl.
- FR-C8: Retain the `FileTempClearer` scheduler to clean stale MinIO temp objects.

### core-service
- FR-K1: Consume `ComicExtractedEvent` → persist `Comic` + `Tag` + stub `Chapter` entities with `CrawlStatus.INIT`.
- FR-K2: After persisting chapters, publish `ChapterPendingEvent` for each new chapter.
- FR-K3: Consume `ChapterImagesExtractedEvent` → persist `Image` entities, update `Chapter` status to `FINISHED`.
- FR-K4: Expose `GET /internal/comics/exists?slug={slug}` for dedup checks by crawl-service.
- FR-K5: Expose `GET /internal/chapters/{id}` for crawl-service to fetch chapter origin URL.
- FR-K6: Reject duplicate comics (slug or origin_id conflict) and publish a `CrawlRejectedEvent` (optional dead-letter routing).

### display-service
- FR-D1: Expose the full GraphQL schema: `comics`, `comic`, `chapters`, `detailChapterById`, `detailChapterBySlug`, tags.
- FR-D2: Read-only access to PostgreSQL (same instance, or read replica).
- FR-D3: Support paginated queries with `PageRequest` / `PageResponse`.

### shared-lib
- FR-S1: Contain all JPA entity classes (`Comic`, `Chapter`, `Image`, `Tag`).
- FR-S2: Contain all RabbitMQ event message records (`ComicExtractedEvent`, `ChapterPendingEvent`, `ChapterImagesExtractedEvent`).
- FR-S3: Contain common enums (`CrawlStatus`, `ImageType`), `PageRequest`, `PageResponse`, `ApiError`.
- FR-S4: Contain common exceptions (`ResourceNotFoundException`, `ConflictException`, `BusinessException`).
- FR-S5: Be a plain Gradle Java library (no Quarkus plugin) consumed by all three services.

## Non-Functional Requirements

| Requirement | Target |
|---|---|
| crawl-service native startup | < 1 second |
| display-service P99 GraphQL latency | < 200ms |
| RabbitMQ message delivery guarantee | at-least-once (DLQ on failure) |
| core-service idempotency | duplicate events must not create duplicate DB rows |
| Shared library versioning | semantic versioning via `gradle.properties` |

## Constraints

- All services continue to use **Quarkus 3.28.1** and **Java 21**.
- RabbitMQ is already provisioned and used for `chapter-crawl`; extend this broker for the new events.
- PostgreSQL remains a single shared instance; display-service uses the same JDBC URL as core-service (read-only user recommended for display-service in production).
- MinIO remains a single shared instance; only crawl-service writes images.
- Build toolchain stays as **Gradle** with the existing wrapper.
