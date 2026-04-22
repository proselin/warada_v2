# Feature Design: refactor-beauty-210426

## Goal

Fix two developer experience issues that appear during `quarkusDev` startup:
1. RabbitMQ DLQ binding errors caused by a missing `DLX` exchange
2. Low-contrast console log colors that are hard to scan in the terminal

## Design Summary

### Problem 1: RabbitMQ DLX startup error

All incoming RabbitMQ channels configured with `auto-bind-dlq=true` default to binding their DLQ to an exchange named `DLX`. RabbitMQ does not pre-create this exchange — it is a convention, not a built-in. This causes a repeating error loop on startup:

```
SRMSG17026: Unable to bind queue 'xxx.dlq' to exchange 'DLX':
NOT_FOUND - no exchange 'DLX' in vhost '/'
```

**Fix:** Point each affected channel's dead-letter exchange to `amq.direct`, which is a pre-declared direct exchange guaranteed to exist in every RabbitMQ vhost. Set `dead-letter-routing-key` to the DLQ queue name so messages route correctly.

Affected channels:

| Service | Channel | DLQ |
|---|---|---|
| core-service | comic-extracted-in | comic.extracted.queue.dlq |
| core-service | chapter-images-extracted-in | chapter.images.extracted.queue.dlq |
| crawl-service | chapter-pending-in | chapter.pending.queue.dlq |

### Problem 2: Console log colors

The default Quarkus log format uses low-contrast colors. A `%highlight` format string is added to the `%dev` profile of each service with a high-contrast color mapping per log level.

## Main Decisions

### Use `amq.direct` instead of a custom exchange
RabbitMQ guarantees `amq.direct` exists in every vhost with no setup required. No docker-compose changes, no RabbitMQ provisioning scripts, no definitions file to maintain.

### Config-only change
All fixes are in `application.properties`. No code changes, no new dependencies, no schema migrations.

### Color format scoped to `%dev` profile
Log format changes apply only during development. Production log format is unchanged.
