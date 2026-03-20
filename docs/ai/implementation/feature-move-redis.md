# Feature: Redis Job Queue for Scheduled Comic Updates — Implementation

## Dependencies Added

```gradle
implementation 'io.quarkus:quarkus-redis-cache'
implementation 'io.quarkus:quarkus-redis-client'
```

## Liquibase Migration (`add-update-schedule-260314.sql`)

```sql
ALTER TABLE comics ADD COLUMN update_on VARCHAR(10) DEFAULT 'NONE' NOT NULL;
ALTER TABLE comics ADD COLUMN fixed_time_to_next_update INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE comics ADD COLUMN next_update_date TIMESTAMP(6);
ALTER TABLE comics ADD COLUMN last_update_date TIMESTAMP(6);
CREATE INDEX idx_comics_next_update_date ON comics (next_update_date) WHERE next_update_date IS NOT NULL;
```

## UpdateSchedule Enum

Located at `commons/enums/UpdateSchedule.java`. Values: `WEEK`, `MONTH`, `NONE`. Uses `fromValue(String)` factory method. Stored via `UpdateScheduleConverter` (JPA `AttributeConverter<UpdateSchedule, String>`).

## Comic Entity Changes

```java
@Convert(converter = UpdateScheduleConverter.class)
@Column(name = "update_on", nullable = false)
@Builder.Default
private UpdateSchedule updateOn = UpdateSchedule.NONE;

@Column(name = "fixed_time_to_next_update", nullable = false)
@Builder.Default
private Integer fixedTimeToNextUpdate = 0;

@Column(name = "next_update_date")
private LocalDateTime nextUpdateDate;

@Column(name = "last_update_date")
private LocalDateTime lastUpdateDate;
```

## ComicUpdateQueueServiceImpl

Uses Quarkus Redis `ListCommands<String, String>` injected via `RedisDataSource`:
- `enqueueComicUpdate(Long)` → `RPUSH` single ID
- `enqueueComicUpdates(List<Long>)` → `RPUSH` batch
- `dequeueComicUpdate()` → `LPOP` → parse to `Long`
- `queueSize()` → `LLEN`

Queue key: `warada:comic-update-queue`

## ComicUpdateScheduler

- `@Scheduled(cron = "0 0 * * * ?")` — every hour
- `@Transactional` for DB read
- Queries `comicRepository.findComicsDueForUpdate(now)` — `updateOn != 'NONE' AND nextUpdateDate <= now`
- Enqueues all matching comic IDs in one batch

## ComicUpdateWorker

- `@Scheduled(every = "5m")` — polls queue
- `@Transactional` for DB read/write
- Flow: dequeue → find comic → validate updateOn → crawl chapters → set lastUpdateDate → compute nextUpdateDate → persist
- `computeNextUpdateDate()`: static method using `TemporalAdjusters.next(DayOfWeek)` for weekly, day-of-month clamping for monthly

## Configuration (application.yaml)

```yaml
# dev profile
redis:
  hosts: redis://localhost:6379

# prod profile
redis:
  hosts: ${REDIS_URL:redis://localhost:6379}
```

## Bug Fix: SmallRye GraphQL Metrics

Disabled `quarkus.smallrye-graphql.metrics.enabled` across all profiles to fix `NoClassDefFoundError: io/micrometer/core/instrument/MeterRegistry` at startup. The SmallRye GraphQL metrics SPI could not find a compatible micrometer integration despite `quarkus-micrometer-registry-prometheus` being on the classpath.
