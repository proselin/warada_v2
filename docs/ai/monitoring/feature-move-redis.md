# Feature: Redis Job Queue for Scheduled Comic Updates — Monitoring

## Key Metrics

| Metric | Source | What to Watch |
|--------|--------|---------------|
| Redis queue length | `LLEN warada:comic-update-queue` | Growing unboundedly = worker can't keep up |
| Worker processing time | Application logs | Increasing = crawl latency issues |
| Scheduler enqueue count | Application logs | Spike = many comics due at once |
| Redis connection errors | Application logs (ERROR level) | `Connection refused` = Redis down |

## Log Patterns

### ComicUpdateScheduler
```
INFO  [ComicUpdateScheduler] Scanning comics due for update at {timestamp}
INFO  [ComicUpdateScheduler] Enqueued {count} comics for update
INFO  [ComicUpdateScheduler] No comics due for update
```

### ComicUpdateWorker
```
INFO  [ComicUpdateWorker] Processing comic ID {id} from queue
INFO  [ComicUpdateWorker] Finished updating comic ID {id}, next update: {date}
WARN  [ComicUpdateWorker] Comic ID {id} not found, skipping
WARN  [ComicUpdateWorker] Comic ID {id} has updateOn=NONE, skipping
ERROR [ComicUpdateWorker] Failed to process comic ID {id}
```

## Health Checks

- **Application health**: `GET /q/health` — includes Redis client readiness check via Quarkus SmallRye Health
- **Redis connectivity**: If Redis is down, the worker and scheduler will log errors but the application continues running (graceful degradation)

## Alerting Recommendations

| Condition | Severity | Action |
|-----------|----------|--------|
| Redis down (`Connection refused`) | Warning | Check Redis service, restart if needed |
| Queue size > 100 | Warning | Worker may be too slow or stuck |
| Worker ERROR count > 5/hour | Warning | Check crawl target availability |
| Scheduler not running (no scan logs for 2+ hours) | Critical | Check scheduler configuration and app health |
