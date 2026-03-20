# Feature: Redis Job Queue for Scheduled Comic Updates — Testing

## Test Coverage

### Unit Tests Created

| Test Class | Tests | Purpose |
|-----------|-------|---------|
| `UpdateScheduleConverterTest` | 5 | Enum ↔ String conversion, null handling, invalid value |
| `ComicUpdateQueueServiceImplTest` | 5 | Enqueue single/batch, dequeue, empty queue, queue size |
| `ComicUpdateSchedulerTest` | 2 | Enqueue due comics, skip when none due |
| `ComicUpdateWorkerTest` | 5+ | Process queue, skip missing comic, skip NONE schedule, crawl + compute next date |

### ComicUpdateQueueServiceImplTest

Mocks `RedisDataSource` and `ListCommands`:
- `testEnqueueSingle` — verifies `RPUSH` called with correct key and value
- `testEnqueueBatch` — verifies batch `RPUSH` with multiple IDs
- `testDequeue` — verifies `LPOP` returns parsed Long
- `testDequeueEmpty` — verifies null return when queue is empty
- `testQueueSize` — verifies `LLEN` delegation

### ComicUpdateSchedulerTest

Mocks `ComicRepository` and `ComicUpdateQueueService`:
- `testScanAndEnqueue` — creates comics with `nextUpdateDate` in the past, verifies `enqueueComicUpdates` called
- `testNoDueComics` — returns empty list, verifies no enqueue call

### ComicUpdateWorkerTest

Mocks `ComicUpdateQueueService`, `ComicRepository`, `CrawlService`:
- `testProcessQueueEmptyQueue` — dequeue returns null, no DB calls
- `testProcessQueueComicNotFound` — dequeue returns ID, comic not in DB, logs warning
- `testProcessQueueNoneSchedule` — comic has `updateOn=NONE`, skips processing
- `testProcessQueueSuccess` — full flow: dequeue → crawl → compute next date → persist
- `testComputeNextUpdateDateWeekly` — verifies next Monday/Tuesday/etc. calculation
- `testComputeNextUpdateDateMonthly` — verifies day-of-month calculation with clamping

### Existing Tests

All 100 existing tests continue to pass after the Redis feature addition:
- ChapterServiceImplTest (fixed `testGetChapterBySlug` to match updated implementation)
- CrawlRestResourceTest, HealthCheckResourceTest
- All other service, repository, and converter tests
