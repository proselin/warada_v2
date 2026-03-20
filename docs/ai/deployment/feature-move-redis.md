# Feature: Redis Job Queue for Scheduled Comic Updates — Deployment

## Infrastructure Requirements

### Redis

- **Image**: `redis:7-alpine`
- **Port**: 6379
- **Volume**: `./temp/redis-data:/data` (dev), persistent volume (prod)
- **No authentication** in dev; configure `REDIS_URL` with credentials in prod

### Docker Compose (dev)

```yaml
redis:
  image: redis:7-alpine
  volumes:
    - ./temp/redis-data:/data
  ports:
    - "6379:6379"
```

### Production Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `REDIS_URL` | Redis connection URL | `redis://user:pass@redis-host:6379` |

## Database Migration

The Liquibase migration `add-update-schedule-260314.sql` runs automatically at startup (`quarkus.liquibase.migrate-at-start: true`). It adds:
- 4 new columns to `comics` table (non-destructive `ALTER TABLE ADD COLUMN`)
- 1 partial index on `next_update_date`

**Rollback**: Drop the columns and index manually if needed.

## Startup Verification

After deployment, verify:
1. Application starts without `ClassNotFoundException` (micrometer metrics fix applied)
2. Redis connection established — check logs for absence of `Connection refused: localhost:6379`
3. `ComicUpdateScheduler` runs every hour — check logs for `[ComicUpdateScheduler] Scanning comics due for update`
4. `ComicUpdateWorker` polls every 5 minutes — check logs for queue processing or empty polls
5. Health endpoint responds: `GET /q/health`

## Monitoring

- **Queue size**: Monitor via Redis CLI (`LLEN warada:comic-update-queue`) or add a health check
- **Worker errors**: Logged at ERROR level with comic ID
- **Scheduler scans**: Logged at INFO level with count of enqueued comics
