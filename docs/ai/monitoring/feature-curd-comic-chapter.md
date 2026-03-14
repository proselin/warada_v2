---
phase: monitoring
title: CRUD for Comic & Chapter — Monitoring & Observability
description: Monitoring considerations for the new Comic/Chapter CRUD endpoints
---

# Monitoring: CRUD for Comic & Chapter

## New Metrics to Track

### REST Endpoints
OpenTelemetry (via `quarkus-opentelemetry`) automatically traces all JAX-RS requests. The following endpoint paths will appear in traces:

| Endpoint | Span Name | Key Attributes |
|---|---|---|
| `POST /api/v1/comics` | `POST /api/v1/comics` | `http.status_code`, `http.method` |
| `DELETE /api/v1/comics/{id}` | `DELETE /api/v1/comics/{id}` | `http.status_code`, `comic.id` |
| `POST /api/v1/chapters` | `POST /api/v1/chapters` | `http.status_code`, `http.method` |
| `DELETE /api/v1/chapters/{id}` | `DELETE /api/v1/chapters/{id}` | `http.status_code`, `chapter.id` |

### GraphQL Mutations
SmallRye GraphQL tracing automatically traces mutations:

| Mutation | Trace Operation |
|---|---|
| `updateComic` | `mutation updateComic` |
| `updateChapter` | `mutation updateChapter` |

## Error Monitoring

### 404 Not Found
- Emitted by `ResourceNotFoundExceptionMapper` for missing `Comic` or `Chapter` resources.
- Log key: `ResourceNotFoundException` message — e.g., `"Comic not found with id: 99"`.
- Track 404 rate on write endpoints; spikes may indicate stale references or client bugs.

### 500 Internal Server Error
- Caught by `GlobalExceptionMapper`; logged via `e.printStackTrace()` (existing).
- May occur on database constraint violations (e.g., duplicate `slug` on create).

## Logging
Service implementations use `@JBossLog` with structured log messages:

| Event | Level | Message Pattern |
|---|---|---|
| Comic created | `INFO` | `Created comic id={id} slug={slug}` |
| Comic updated | `INFO` | `Updated comic id={id}` |
| Comic deleted | `INFO` | `Deleted comic id={id}` |
| Chapter created | `INFO` | `Created chapter id={id} chapterNumber={num} comicId={cid}` |
| Chapter updated | `INFO` | `Updated chapter id={id}` |
| Chapter deleted | `INFO` | `Deleted chapter id={id}` |

## Health Checks
No new custom health indicators needed. The existing `SmallRye Health` liveness/readiness checks at `/q/health` cover database connectivity, which is sufficient for CRUD operations.

## Business Metrics
Consider adding custom metrics (future work) to track:
- Total comics created via direct API vs. crawl pipeline
- Total chapters created via direct API vs. crawl pipeline
- Delete rate anomalies (bulk deletes could indicate a bug)
