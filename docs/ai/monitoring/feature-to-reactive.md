# Feature: REST Blocking to Reactive — Monitoring

## What to Observe

Switching from blocking to reactive changes the thread model. Phase 1 moves the HTTP layer to the event loop but keeps DB work on worker threads. Phase 2 removes worker thread usage for DB entirely.

---

## OpenTelemetry Traces

| Signal | Phase 1 (current) | Phase 2 (Hibernate Reactive) |
|--------|------------------|------------------------------|
| Accept thread | `vert.x-eventloop-thread-*` | `vert.x-eventloop-thread-*` |
| Work thread | `executor-thread-*` (worker pool) | `vert.x-eventloop-thread-*` (no switch) |
| Span duration | Short e-loop + async worker segment | Single non-blocking span |

---

## Phase 1 — Critical Signals to Watch

### Worker pool saturation

| Metric | Description | Alert threshold |
|--------|-------------|----------------|
| `worker_pool_queue_size` | Pending tasks on worker pool | > 5 sustained |
| `worker_pool_usage` | Active threads / pool size | > 80% |
| `http.server.request.duration` p99 | End-to-end latency | Spike under concurrent crawl |

If the pool saturates, requests queue silently — HTTP 200 eventually but with multi-second delays.

**Check**: Vert.x `BlockedThreadChecker` in logs:
```
WARN  [io.ver.cor.imp.BlockedThreadChecker] Thread vert.x-worker-thread-N has been blocked for NNNms
```

### SSE endpoint event-loop violation (Phase 1 current bug)

In dev mode, the SSE endpoint causes:
```
WARN  [io.ver.cor.imp.BlockedThreadChecker] Thread vert.x-eventloop-thread-N has been blocked for NNNms
```
This confirms `CrawlServiceImpl.crawlNettruyenSSEChapter` is executing Hibernate calls on the event loop.
**Immediate fix**: Add `@Blocking` to `CrawlRestResource#crawlChapterByComic`.

---

## Phase 2 — Expected Monitoring Changes

After full Hibernate Reactive migration:

| Signal | Phase 1 baseline | Phase 2 expectation |
|--------|-----------------|---------------------|
| `worker_pool_queue_size` | Can spike under crawl load | ≈ 0 for DB operations |
| JVM thread count | Scales with concurrent DB requests | Flat (event-loop driven) |
| `http.server.request.duration` variance | High under concurrent crawl | Lower, more consistent |
| `BlockedThreadChecker` warnings | SSE endpoint (known bug) | None |

---

## Health Endpoints

- `GET /q/health/live` — liveness
- `GET /q/health/ready` — readiness (includes DB connectivity)
- `GET /zhealth/ping` — app-level ping (`Uni<String>`, resolves on event loop)

Phase 2: readiness check uses reactive datasource probe — no change to endpoint URLs.

---

## Log Signals

Worker thread confirmation (Phase 1 — correct for POST endpoints):
```
INFO  executor-thread-N  [START] Crawling Nettruyen Comic from slug-123
INFO  executor-thread-N  [DONE] Crawling Nettruyen Comic from slug-123
```

Event-loop violation (Phase 1 — SSE endpoint bug to fix):
```
INFO  vert.x-eventloop-thread-N  [START] Crawling SSE comic ...  ← WRONG THREAD
```

Phase 2 — all crawl work on event loop is expected and non-blocking:
```
INFO  vert.x-eventloop-thread-N  [START] Crawling Nettruyen Comic ...  ← correct in Phase 2
```

---

## Alerts / Degradation

| Condition | Cause | Action |
|-----------|-------|--------|
| `BlockedThreadChecker` on event loop (SSE) | Phase 1 bug | Add `@Blocking` to SSE resource method |
| `worker_pool_queue_size` growing | Phase 1 pool exhaustion | Increase `quarkus.vertx.worker-pool-size`; prioritize Phase 2 |
| HTTP 500 spike | Exception not mapped | Check `GlobalExceptionMapper` and verify `Uni` failure propagation |
| DB connection timeout | Reactive pool exhausted (Phase 2) | Tune `quarkus.datasource.reactive.max-size` |
