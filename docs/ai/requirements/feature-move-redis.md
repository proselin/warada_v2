# Feature: Redis Job Queue for Scheduled Comic Updates — Requirements

## Overview

Introduce Redis as a job queue backend for scheduling automatic chapter re-crawls of comics. Each comic can be configured with a recurring update schedule (weekly or monthly), and a background worker processes the queue to crawl new chapters.

## Problem Statement

- Comics are only updated via manual REST API crawl triggers.
- There is no mechanism to periodically re-crawl comics that receive new chapters on a known schedule.
- Users must manually trigger crawls for each comic, which is not scalable.

## Goals

1. Add schedule-related fields to the `Comic` entity: `updateOn` (WEEK/MONTH/NONE), `fixedTimeToNextUpdate` (day number), `nextUpdateDate`, `lastUpdateDate`.
2. Use Redis as a lightweight job queue (RPUSH/LPOP on a list key) to decouple scheduling from processing.
3. Implement a **scanner scheduler** that runs hourly, finds comics due for update, and enqueues their IDs into Redis.
4. Implement a **worker** that polls the Redis queue every 5 minutes, re-crawls chapters for the dequeued comic, and computes the next update date.
5. Support `UpdateSchedule.NONE` to opt out of automatic updates (default for all existing comics).

## Affected Components

| Component | Change |
|-----------|--------|
| `Comic` entity | Add `updateOn`, `fixedTimeToNextUpdate`, `nextUpdateDate`, `lastUpdateDate` fields |
| `UpdateSchedule` enum | New enum: WEEK, MONTH, NONE |
| `UpdateScheduleConverter` | JPA converter for UpdateSchedule ↔ String |
| `ComicRepository` | Add `findComicsDueForUpdate(LocalDateTime now)` query |
| `ComicUpdateQueueService` | New interface + impl: enqueue/dequeue comic IDs via Redis list |
| `ComicUpdateScheduler` | Cron job (hourly): scan and enqueue due comics |
| `ComicUpdateWorker` | Scheduled job (every 5m): dequeue and process comic updates |
| `application.yaml` | Redis connection config for all profiles |
| `compose.yaml` | Add Redis 7 Alpine service |
| `build.gradle` | Add `quarkus-redis-client` and `quarkus-redis-cache` dependencies |
| Liquibase migration | `add-update-schedule-260314.sql`: new columns + index |

## Non-Goals

- Redis is not used as a general-purpose cache in this phase (cache extension is included but commented out).
- No UI or REST endpoint for managing update schedules — the fields are set directly on comic creation or via DB.
- No retry/dead-letter queue for failed updates — errors are logged and skipped.

## Configuration

| Key | Profile | Value |
|-----|---------|-------|
| `quarkus.redis.hosts` | dev | `redis://localhost:6379` |
| `quarkus.redis.hosts` | prod | `${REDIS_URL:redis://localhost:6379}` |
