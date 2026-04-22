# Feature Implementation: refactor-beauty-210426

## Implemented Changes

### 1. RabbitMQ DLQ dead-letter exchange fix

Added `dead-letter-exchange=amq.direct` and `dead-letter-routing-key` to all incoming channels that use `auto-bind-dlq=true`.

**core-service** (`src/apps/core-service/src/main/resources/application.properties`):
```properties
mp.messaging.incoming.comic-extracted-in.dead-letter-exchange=amq.direct
mp.messaging.incoming.comic-extracted-in.dead-letter-routing-key=comic.extracted.queue.dlq

mp.messaging.incoming.chapter-images-extracted-in.dead-letter-exchange=amq.direct
mp.messaging.incoming.chapter-images-extracted-in.dead-letter-routing-key=chapter.images.extracted.queue.dlq
```

**crawl-service** (`src/apps/crawl-service/src/main/resources/application.properties`):
```properties
mp.messaging.incoming.chapter-pending-in.dead-letter-exchange=amq.direct
mp.messaging.incoming.chapter-pending-in.dead-letter-routing-key=chapter.pending.queue.dlq
```

### 2. Console log colors

Added high-contrast `%highlight` log format to the `%dev` profile of all three services:

```properties
%dev.quarkus.log.console.color=true
%dev.quarkus.log.console.format=%d{HH:mm:ss} %highlight{%-5p}{FATAL=red blink, ERROR=red, WARN=yellow bold, INFO=green bold, DEBUG=cyan, TRACE=blue} [%c{1.}] (%t) %s%e%n
```

Color mapping:
- FATAL → red blinking
- ERROR → red
- WARN → yellow bold
- INFO → green bold
- DEBUG → cyan
- TRACE → blue

Applied to: `core-service`, `crawl-service`, `display-service`.

## How to verify

```bash
docker compose up -d
./gradlew :core-service:quarkusDev
```

Expected: no `SRMSG17026 / NOT_FOUND - no exchange 'DLX'` errors on startup.

To confirm DLQ bindings are correct, open the RabbitMQ Management UI at `http://localhost:15672`, navigate to **Exchanges → amq.direct → Bindings**, and verify DLQ queue names appear as bindings.
