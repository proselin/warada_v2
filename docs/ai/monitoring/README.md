---
phase: monitoring
title: Monitoring & Observability
description: Define monitoring strategy, metrics, alerts, and incident response
---

# Monitoring & Observability

## Key Metrics
**What do we need to track?**

### Performance Metrics
- **HTTP request latency** — Response time for REST and GraphQL endpoints
- **Throughput** — Requests per second to `/graphql` and `/api/v1/crawl/*`
- **Database query time** — Hibernate/Panache query execution duration
- **MinIO operation latency** — Image upload/download/delete times
- **JVM/Native metrics** — Heap usage, thread count, GC pauses (JVM) or RSS (native)

### Business Metrics
- **Comics crawled** — Count of comics with `crawling_status = FINISHED`
- **Chapters crawled** — Count of chapters with `crawling_status = FINISHED` vs `INIT`
- **Images stored** — Total image count in database and MinIO
- **Crawl success rate** — Successful crawls vs failures
- **Temp files cleaned** — Files removed per scheduler run

### Error Metrics
- **HTTP 4xx/5xx rates** — By endpoint
- **Crawl failures** — NetTruyen extraction errors, image download failures
- **MinIO errors** — Connection failures, upload failures
- **Database errors** — Connection pool exhaustion, query failures

## Monitoring Tools
**What tools are we using?**

| Tool | Purpose | Status |
|---|---|---|
| **OpenTelemetry** | Traces, metrics, logs export | Configured (prod only) |
| **OTel Collector** | Central telemetry pipeline | Optional external deployment |
| **SmallRye Health** | Readiness/liveness probes | Active at `/q/health` |
| **SmallRye GraphQL Metrics** | GraphQL query metrics | Disabled in current config |
| **SmallRye GraphQL Tracing** | GraphQL distributed tracing | Disabled in current config |
| **SmallRye OpenAPI** | API documentation | Active at `/q/openapi` |

### OpenTelemetry Configuration (Production)
```properties
quarkus.otel.metrics.enabled=true
quarkus.otel.logs.enabled=true
quarkus.otel.traces.enabled=true
quarkus.otel.exporter.otlp.endpoint=${OTLP_ENDPOINT}
quarkus.otel.exporter.otlp.protocol=grpc
```

### OTel Collector Pipeline
An OpenTelemetry collector can be deployed separately when needed, with:
- **Receivers:** OTLP (gRPC on 4317, HTTP on 4318)
- **Processors:** Batch + resource (service.name: `warada`)
- **Exporters:** `debug`, Jaeger, Prometheus, Loki, or another production backend
- **Extensions:** health_check, zpages, pprof

## Logging Strategy
**What do we log and how?**

### Log Framework
- **Library:** JBoss Logging (via Lombok `@JBossLog`)
- **Default level:** `INFO`

### Log Format

**Development:**
Standard Quarkus console format (no color: `quarkus.console.color: false`)

**Production:**
```
HH:mm:ss LEVEL traceId=X, parentId=X, spanId=X, sampled=X [category] (thread) message
```
Includes OpenTelemetry trace context for distributed tracing correlation.

### Key Log Points
| Component | Level | What is logged |
|---|---|---|
| `CrawlServiceImpl` | INFO | Crawl start/end, chapter processing |
| `NettruyenExtractorImpl` | INFO | HTTP requests, extraction results |
| `FileTempClearer` | INFO | Scan results, deletion counts |
| `ComicGqlResource` | DEBUG | GraphQL query execution |
| REST Client | INFO | Request/response (scope: request-response, body limit: 1024) |
| `GlobalExceptionMapper` | ERROR | Unhandled exceptions (stack trace to server logs) |

### Sensitive Data Handling
- Stack traces are logged server-side but NOT returned in API responses
- API errors return only a generic message with a `processId` for correlation
- MinIO credentials are not logged

## Alerts & Notifications
**When and how do we get notified?**

### Critical Alerts (Recommended)
| Condition | Action |
|---|---|
| `/q/health` returns DOWN | Investigate database/MinIO connectivity |
| HTTP 5xx rate > 5% for 5 min | Check application logs, restart if necessary |
| Crawl service consecutive failures > 3 | Check NetTruyen availability, review extractor patterns |
| Database connection pool exhausted | Scale database or review connection settings |

### Warning Alerts (Recommended)
| Condition | Action |
|---|---|
| `FileTempClearer` reports errors | Check MinIO permissions/connectivity |
| HTTP latency P99 > 5 seconds | Review slow queries, check resource usage |
| MinIO disk usage > 80% | Plan storage expansion or increase cleanup frequency |

## Dashboards
**What do we visualize?**

### Recommended Dashboard Panels
1. **System Health** — Health check status, uptime, CPU/memory usage
2. **API Performance** — Request rate, latency percentiles (P50, P95, P99) by endpoint
3. **Crawl Operations** — Crawl rate, success/failure ratio, images processed
4. **Database** — Query latency, connection pool usage, active transactions
5. **MinIO Storage** — Object count, storage used, upload/download rates
6. **Errors** — Error rate over time, top error types, recent exceptions

## Health Checks
**How do we verify system health?**

### Endpoints
| Endpoint | Type | Description |
|---|---|---|
| `GET /q/health` | SmallRye Health | Combined readiness + liveness |
| `GET /q/health/ready` | SmallRye Health | Readiness probe |
| `GET /q/health/live` | SmallRye Health | Liveness probe |
| `GET /zhealth/ping` | Custom REST | Simple ping → "pong" |
| `{ ping }` | GraphQL | Returns "Pong" |

### Automated Checks (via SmallRye Health)
- Database connectivity (datasource health check)
- Application startup status

### Recommended Additional Checks
- MinIO connectivity check
- NetTruyen reachability check
- Disk/storage usage check

## Incident Response
**How do we handle issues?**

### Common Issues & Resolution

| Issue | Diagnosis | Resolution |
|---|---|---|
| App won't start | Check logs for migration errors or connection failures | Fix configuration or restart dependencies |
| Crawl returns 409 | Comic already exists in database | Expected behavior — skip or use different slug |
| Crawl returns 404 | Comic slug not found on NetTruyen | Verify URL/slug on NetTruyen website |
| Images missing | Check MinIO bucket and file paths | Verify MinIO connectivity; re-crawl chapter |
| GraphQL returns null | Check database for entity existence | Verify crawl completed successfully |
| Temp files accumulating | Scheduler not running or failing | Check scheduler logs; verify MinIO permissions |

### Post-Incident Process
1. Identify root cause from logs (use `processId` for correlation)
2. Fix configuration or code
3. Deploy fix
4. Verify via health checks and test queries
5. Document in project notes
