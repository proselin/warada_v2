# Feature: Redis Job Queue for Scheduled Comic Updates — Planning

## Implementation Order

1. **Infrastructure**: Add Redis to `compose.yaml`, add Quarkus Redis dependencies to `build.gradle`
2. **Database**: Create Liquibase migration for new Comic columns + index
3. **Enum + Converter**: Create `UpdateSchedule` enum and `UpdateScheduleConverter`
4. **Entity**: Update `Comic` entity with new fields and converter annotations
5. **Repository**: Add `findComicsDueForUpdate()` query to `ComicRepository`
6. **Queue Service**: Create `ComicUpdateQueueService` interface and `ComicUpdateQueueServiceImpl` (Redis list operations)
7. **Scheduler**: Create `ComicUpdateScheduler` (hourly scan + enqueue)
8. **Worker**: Create `ComicUpdateWorker` (dequeue + crawl + compute next date)
9. **Configuration**: Add Redis hosts to `application.yaml` for dev/prod/test-native profiles
10. **Tests**: Unit tests for converter, queue service, scheduler, worker, and date computation
11. **Fix micrometer**: Disable `smallrye-graphql.metrics.enabled` to resolve `NoClassDefFoundError` at startup

## Files Created

| File | Purpose |
|------|---------|
| `src/main/java/.../commons/enums/UpdateSchedule.java` | WEEK/MONTH/NONE enum |
| `src/main/java/.../converter/UpdateScheduleConverter.java` | JPA AttributeConverter |
| `src/main/java/.../services/ComicUpdateQueueService.java` | Queue interface |
| `src/main/java/.../services/ComicUpdateQueueServiceImpl.java` | Redis list implementation |
| `src/main/java/.../scheduler/ComicUpdateScheduler.java` | Hourly scan + enqueue |
| `src/main/java/.../scheduler/ComicUpdateWorker.java` | Poll + crawl + update |
| `src/main/resources/db/migration/add-update-schedule-260314.sql` | Liquibase migration |

## Files Modified

| File | Change |
|------|--------|
| `build.gradle` | Added `quarkus-redis-client`, `quarkus-redis-cache` |
| `compose.yaml` | Added Redis 7 Alpine service on port 6379 |
| `application.yaml` | Redis hosts config for dev/prod/test-native; disabled `smallrye-graphql.metrics` |
| `Comic.java` | Added 4 new fields with JPA annotations |
| `ComicRepositoryImpl.java` | Added `findComicsDueForUpdate` query |
| `ComicRepository.java` | Added method signature |
| `db-changelog-master.yaml` | Registered new migration |

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Redis not running → worker errors | Graceful error logging; scheduler/worker use `ConcurrentExecution.SKIP` |
| Queue grows unbounded if worker is slower than scheduler | Worker processes one comic per 5m cycle; monitor queue size |
| `crawlNettruyenComic` throws ConflictException on re-crawl | Worker catches and logs exception, continues with chapter crawl |
| Month clamping edge case (day 31 in February) | `Math.min(dayOfMonth, monthLength)` handles it |
