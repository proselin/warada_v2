# Design: refactor/beauty — RabbitMQ DLX Fix + Console Log Colors

**Date:** 2026-04-21  
**Branch:** `refactor/beauty`  
**Status:** Approved

---

## Problem Statement

Two developer experience issues identified during local development:

1. **RabbitMQ DLX startup error** — All three services spam connection-retry errors on `quarkusDev` startup:
   ```
   SRMSG17026: Unable to bind queue 'xxx.dlq' to exchange 'DLX':
   NOT_FOUND - no exchange 'DLX' in vhost '/'
   ```
   Root cause: `auto-bind-dlq=true` in SmallRye Reactive Messaging defaults the dead-letter exchange name to `DLX`, which is a convention not pre-created by RabbitMQ.

2. **Hard-to-read console colors** — The default Quarkus log format uses low-contrast colors that are difficult to scan in the terminal during development.

---

## Design

### Fix 1: RabbitMQ DLQ configuration

**Approach:** Option A — point `dead-letter-exchange` to `amq.direct`, the built-in RabbitMQ exchange guaranteed to exist in every vhost. No RabbitMQ topology pre-configuration required.

**Properties to add** to each affected incoming channel:

```
mp.messaging.incoming.<channel>.dead-letter-exchange=amq.direct
mp.messaging.incoming.<channel>.dead-letter-routing-key=<dlq-queue-name>
```

**Affected channels:**

| Service | Channel key | DLQ name |
|---|---|---|
| `core-service` | `comic-extracted-in` | `comic.extracted.queue.dlq` |
| `core-service` | `chapter-images-extracted-in` | `chapter.images.extracted.queue.dlq` |
| `crawl-service` | `chapter-pending-in` | `chapter.pending.queue.dlq` |

**Why `amq.direct`?**  
RabbitMQ pre-declares several exchanges in every vhost: `amq.direct`, `amq.topic`, `amq.fanout`, `amq.headers`, and `amq.match`. `amq.direct` is the correct type (direct routing) for DLQ message routing by routing key. Using it eliminates the need for any RabbitMQ provisioning step.

**Files changed:**
- `src/apps/core-service/src/main/resources/application.properties`
- `src/apps/crawl-service/src/main/resources/application.properties`

---

### Fix 2: Console log colors

**Approach:** Add a `%highlight` log format string to the `%dev` profile of each service. JBoss Log Manager (used by Quarkus) supports ANSI color codes via `%highlight{pattern}{LEVEL=color}`.

**Properties to add** to the `%dev` profile of each service:

```properties
%dev.quarkus.log.console.color=true
%dev.quarkus.log.console.format=%d{HH:mm:ss} %highlight{%-5p}{FATAL=red blink, ERROR=red, WARN=yellow bold, INFO=green bold, DEBUG=cyan, TRACE=blue} [%c{1.}] (%t) %s%e%n
```

**Color mapping:**

| Level | Color |
|---|---|
| FATAL | Red blinking |
| ERROR | Red |
| WARN | Yellow bold |
| INFO | Green bold |
| DEBUG | Cyan |
| TRACE | Blue |

**Files changed:**
- `src/apps/core-service/src/main/resources/application.properties`
- `src/apps/crawl-service/src/main/resources/application.properties`
- `src/apps/display-service/src/main/resources/application.properties`

---

## Scope

- No schema migrations
- No new dependencies
- No docker-compose changes
- Config-only changes in `application.properties` for `core-service`, `crawl-service`, `display-service`

---

## Testing

- Start `docker compose up -d` then run each service with `./gradlew :<service>:quarkusDev`
- Verify no `SRMSG17026` / `NOT_FOUND - no exchange 'DLX'` errors appear on startup
- Verify log output shows color-coded levels in the terminal
