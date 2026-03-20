# Feature: Redis Job Queue for Scheduled Comic Updates — Design

## Architecture

```
┌──────────────────────┐
│  ComicUpdateScheduler │  (cron: every hour)
│  Scans DB for comics  │
│  where nextUpdateDate │
│  <= now & updateOn    │
│  != NONE              │
└──────────┬───────────┘
           │ enqueueComicUpdates(ids)
           ▼
┌──────────────────────┐
│     Redis List        │  key: "warada:comic-update-queue"
│  [comicId, comicId…]  │  RPUSH (enqueue) / LPOP (dequeue)
└──────────┬───────────┘
           │ dequeueComicUpdate()
           ▼
┌──────────────────────┐
│  ComicUpdateWorker    │  (every 5 minutes)
│  1. Dequeue comic ID  │
│  2. Re-crawl chapters │
│  3. Compute next date │
│  4. Persist comic     │
└──────────────────────┘
```

## Data Model Changes

### New Fields on `Comic` Entity

| Field | Type | DB Column | Default | Description |
|-------|------|-----------|---------|-------------|
| `updateOn` | `UpdateSchedule` | `update_on VARCHAR(10)` | `NONE` | WEEK, MONTH, or NONE |
| `fixedTimeToNextUpdate` | `Integer` | `fixed_time_to_next_update INTEGER` | `0` | Day of week (1-7) or month (1-31) |
| `nextUpdateDate` | `LocalDateTime` | `next_update_date TIMESTAMP(6)` | `null` | When the next crawl should trigger |
| `lastUpdateDate` | `LocalDateTime` | `last_update_date TIMESTAMP(6)` | `null` | When chapters were last re-crawled |

### `UpdateSchedule` Enum

```java
WEEK("WEEK", "Update on a specific day of the week"),
MONTH("MONTH", "Update on a specific day of the month"),
NONE("NONE", "No automatic update scheduled");
```

Stored as a `VARCHAR` via `UpdateScheduleConverter` (JPA `AttributeConverter`).

## Redis Queue Design

- **Key**: `warada:comic-update-queue` (Redis List)
- **Enqueue**: `RPUSH` — appends comic IDs to the tail
- **Dequeue**: `LPOP` — pops from the head (FIFO)
- **Data type**: Comic ID as `String` (parsed to `Long` on dequeue)
- **No TTL** on queue entries — worker processes them eventually
- **Concurrency**: Both scheduler and worker use `Scheduled.ConcurrentExecution.SKIP` to prevent overlapping runs

## Next Update Date Computation

### Weekly Schedule
- `fixedTimeToNextUpdate` = 1 (Monday) to 7 (Sunday) — maps to `DayOfWeek.of(value)`
- Computes `next(targetDay)` from current date using `TemporalAdjusters.next()`

### Monthly Schedule
- `fixedTimeToNextUpdate` = 1 to 31 (day of month)
- Clamps to actual month length (e.g., Feb 28/29)
- If target day has passed this month, moves to the next month

## Component Interaction

1. **ComicUpdateQueueService** (interface + impl): Pure Redis list operations, no business logic
2. **ComicUpdateScheduler**: DB scan → enqueue. Runs hourly via `@Scheduled(cron)`
3. **ComicUpdateWorker**: Dequeue → crawl → compute next date → persist. Runs every 5m via `@Scheduled(every)`
4. **CrawlService**: Reused for chapter re-crawling (existing `crawlNettruyenComic` and `crawlNettruyenChapter`)

## Error Handling

- Worker catches all exceptions per comic and logs them — does not re-enqueue failed items
- If a comic is deleted between enqueue and dequeue, the worker skips it with a warning
- If `updateOn == NONE` at dequeue time (changed after enqueue), the worker skips it
