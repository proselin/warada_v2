# Feature: RabbitMQ Queue + SSE Crawl Progress

## Summary

This feature moves chapter crawl processing out of the old request-bound SSE flow and into an asynchronous job pipeline.

Frontend clients can now subscribe to a dedicated SSE endpoint and receive live chapter progress while the backend processes queued jobs from RabbitMQ.

## Motivation

The previous approach coupled crawling and streaming in the same request. That made the crawl process harder to scale and harder for multiple frontend clients to observe.

The new design separates responsibilities:
- REST starts the crawl job
- RabbitMQ buffers chapter work
- a consumer processes chapters asynchronously
- SSE broadcasts progress updates to any subscribed frontend

## Implemented Components

### REST endpoints
- `POST /api/v1/crawl/nettruyen/comic`
  - crawls comic metadata
  - creates chapter stubs
  - publishes chapter jobs to RabbitMQ

- `POST /api/v1/crawl/nettruyen/comic/{slug}/retry`
  - re-queues pending chapters
  - blocks retry if the comic already has an active queued job

- `GET /api/v1/crawl/progress/{comicSlug}`
  - exposes live Server-Sent Events for frontend subscribers

### Messaging
- `ChapterCrawlProducer`
  - publishes `ChapterCrawlMessage` records to the queue

- `ChapterCrawlConsumer`
  - consumes messages
  - crawls the chapter images
  - emits progress events back to the broadcaster

### SSE broadcaster
- `CrawlProgressBroadcaster`
  - keeps per-comic subscribers in memory
  - emits `processing`, `completed`, `failed`, and `skipped` events
  - tracks active comics to prevent duplicate retry requests

## Event Payload

```json
{
  "type": "processing",
  "message": "Processing chapter",
  "data": {
    "chapterId": 12,
    "chapterNumber": "12",
    "status": "STARTED",
    "comicSlug": "one-piece-12345"
  }
}
```

## Operational Notes

- RabbitMQ management UI is available at `http://localhost:15672`
- default local credentials are `guest / guest`
- SSE broadcasting is currently in-memory and scoped to a single backend instance
- subscribers receive live events from the moment they connect

## Future Improvements

- persist job history for replayable progress snapshots
- support multi-instance SSE/event distribution through a shared broker
- expose aggregate comic-level progress percentages for frontend progress bars
