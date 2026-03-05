---
phase: requirements
title: Requirements & Problem Understanding
description: Clarify the problem space, gather requirements, and define success criteria
---

# Requirements & Problem Understanding

## Problem Statement
**What problem are we solving?**

- **Core problem:** Reading manga/comic content from sources like NetTruyen requires navigating through heavy, ad-laden websites with poor user experience. There is no clean API to access comic data programmatically.
- **Who is affected:** Manga/comic readers who want a clean, fast reading experience, and developers who want to build comic reader frontends.
- **Current workaround:** Users must visit the original website (NetTruyen) directly, dealing with ads, slow load times, and inconsistent availability.

## Goals & Objectives
**What do we want to achieve?**

### Primary Goals
- Crawl and store comic metadata (title, author, tags, description, chapters) from NetTruyen
- Crawl and store comic images (thumbnails and chapter pages) in MinIO object storage
- Expose comic data via **GraphQL API** for flexible querying by frontend clients
- Expose crawl operations via **REST API** for administrative control
- Support Server-Sent Events (SSE) for real-time crawl progress tracking

### Secondary Goals
- Provide health check and observability endpoints
- Support native GraalVM compilation for minimal resource usage in production
- Automatic cleanup of temporary files in MinIO

### Non-Goals
- User authentication/authorization (not implemented yet)
- Frontend UI (this is a backend-only service)
- Supporting sources other than NetTruyen (currently)

## User Stories & Use Cases
**How will users interact with the solution?**

- **As an admin**, I want to crawl a comic from NetTruyen by providing a slug, so that the comic metadata and thumbnail are stored in the system.
- **As an admin**, I want to crawl individual chapters or all chapters of a comic via SSE, so that chapter images are downloaded and stored.
- **As a reader (via frontend)**, I want to query comics with pagination via GraphQL, so I can browse available content.
- **As a reader (via frontend)**, I want to query chapter details and images via GraphQL, so I can read comics.
- **As the system**, I want to periodically clean up temporary files in MinIO that are older than a configured age.

### Edge Cases
- Crawling a comic that already exists (should return a conflict error)
- Crawling a chapter that is already finished (should skip gracefully)
- Image download failures during chapter crawl (should log errors and continue)
- NetTruyen website structure changes (extractor patterns may break)

## Success Criteria
**How will we know when we're done?**

- Comics can be successfully crawled and persisted to PostgreSQL
- Chapter images are downloaded to MinIO and linked to chapters in the database
- GraphQL API returns paginated comics, chapter details, and images
- SSE endpoint streams real-time crawl progress
- Native image builds successfully and runs with minimal memory footprint
- Temporary files are automatically cleaned up on schedule

## Constraints & Assumptions
**What limitations do we need to work within?**

### Technical Constraints
- Java 21 required (GraalVM native image target)
- Quarkus 3.28.x framework
- PostgreSQL as the sole database
- MinIO for object/image storage
- Images must be downloaded with specific HTTP headers (Referer, Origin) to bypass hotlink protection

### Business Constraints
- Dependent on NetTruyen's HTML structure — changes may break crawling
- Image URLs from NetTruyen may have multiple server sources (data-sv1, data-sv2)

### Assumptions
- NetTruyen website remains accessible and structurally consistent
- MinIO bucket (`warada-images`) is pre-created
- PostgreSQL database is available and migrations run on startup via Liquibase

## Questions & Open Items
**What do we still need to clarify?**

- Rate limiting strategy for crawling to avoid being blocked by NetTruyen
- Support for additional comic sources beyond NetTruyen
- User authentication/authorization for API access
- Caching strategy for frequently queried comics/chapters
