# Feature: REST Blocking to Reactive — Requirements

## Overview

Refactor all JAX-RS REST endpoints in warada_v2 from blocking (returning `Response`) to reactive (returning `Uni<Response>` or `Multi<T>`) using Quarkus RESTEasy Reactive and SmallRye Mutiny.

## Problem Statement

The existing REST resources use a blocking execution model:
- Endpoints return `Response` directly, binding a worker thread for the full duration of each request.
- Crawl endpoints declare `throws Exception` (unnecessary since all thrown exceptions are `RuntimeException` subclasses).
- The SSE endpoint uses `@Blocking` even though it returns a reactive `Multi`.
- Neither the event-loop thread safety nor back-pressure guarantees are satisfied.

## Goals

1. All JAX-RS resource methods must return `Uni<Response>` (or `Multi<T>` for SSE/streaming endpoints).
2. Blocking JPA/Minio operations must be offloaded to `Infrastructure.getDefaultWorkerPool()` to avoid blocking the Vert.x event loop.
3. The `@Blocking` annotation must be removed from the SSE endpoint — the existing `runSubscriptionOn` inside the `Multi` pipeline is sufficient.
4. Unused `throws Exception` declarations must be removed from service interfaces and implementations.
5. All existing unit tests must pass with the updated reactive signatures.

## Affected Components

| Component | Change |
|-----------|--------|
| `CrawlRestResource` | `crawlIndividualComic`, `crawlChapter` → `Uni<Response>`; remove `@Blocking` from SSE |
| `ComicRestResource` | `createComic`, `deleteComic` → `Uni<Response>` |
| `ChapterRestResource` | `createChapter`, `deleteChapter` → `Uni<Response>` |
| `HealthCheckResource` | `ping` → `Uni<String>` |
| `CrawlService` (interface) | Remove `throws Exception` from all method signatures |
| `CrawlServiceImpl` | Remove `throws Exception`; wrap `catch (Exception)` to avoid checked-exception propagation |

## Non-Goals (Phase 1 — completed)

- No change to GraphQL resources (`*GqlResource`) — they continue using `@Blocking`.

## Identified Problems After Phase 1

### Problem 1 — SSE endpoint blocks the event loop (dev-breaking)

`CrawlServiceImpl.crawlNettruyenSSEChapter` performs two synchronous Hibernate calls **before** returning the `Multi`:

```java
// Both lines run on the calling thread — the Vert.x event loop when invoked
// from a reactive REST resource that has no @Blocking or runSubscriptionOn wrapper.
Comic comic = comicRepository.findBySlugAndComicId(slugNId).orElse(null);  // blocking DB
...
Multi.createFrom().iterable(comic.getChapters())  // blocking lazy-load
```

Vert.x's `BlockedThreadChecker` detects this in dev mode and logs an error.
The app still starts, but the SSE endpoint fails at runtime.

### Problem 2 — Worker thread pool exhaustion under load

Phase 1 wrapped POST endpoint blocking calls in `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`.
This still holds a Vert.x worker thread for the **full duration** of each HTTP request (seconds for crawl ops).
The default pool size is **20 threads**; 20 concurrent crawl requests saturate it completely.
Further requests queue indefinitely → apparent hang / timeout.

### Root cause

Only the HTTP transport layer is reactive. The **persistence layer** (Hibernate ORM + JDBC) is still fully blocking.
True end-to-end non-blocking requires replacing `quarkus-hibernate-orm-panache` with:
- `quarkus-hibernate-reactive-panache`  
- `quarkus-reactive-pg-client`

---

## Phase 2 Goals — Reactive Database

1. Replace blocking Hibernate ORM Panache with Hibernate Reactive Panache.
2. All repository and service methods return `Uni<T>` / `Multi<T>`.
3. `@Transactional` replaced by `@WithTransaction` / `@ReactiveTransactional`.
4. REST resources no longer need `runSubscriptionOn` — event loop handles everything.
5. SSE endpoint no longer blocks the event loop.
6. Worker thread pool freed for CPU-intensive or MinIO I/O work.

## Acceptance Criteria (Phase 2)

- `./gradlew compileJava compileTestJava` completes with `BUILD SUCCESSFUL`.
- No `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` in REST resource layer.
- `./gradlew quarkusDev` starts without `BlockedThreadChecker` warnings on SSE endpoint.
- All unit tests adapted to `Uni.await().indefinitely()` where service methods switch to `Uni<T>`.
- Integration tests pass against live PostgreSQL (reactive driver).
