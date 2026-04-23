---
phase: implementation
title: "Refactor: Separate Module Architecture — Implementation Notes"
description: Key implementation details, code patterns, and gotchas for the multi-module migration
---

# Implementation Notes: Refactor — Separate Module Architecture

## Gradle Multi-Module Setup

### Root `settings.gradle`

All modules live under `src/`. Services in `src/apps/`, shared libraries in `src/libs/`. The monolith source is preserved under `legacy/src/`.

```groovy
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = 'warada_v2'

include 'shared-lib'
project(':shared-lib').projectDir = file('src/libs/shared-lib')

include 'crawl-service'
project(':crawl-service').projectDir = file('src/apps/crawl-service')

include 'core-service'
project(':core-service').projectDir = file('src/apps/core-service')

include 'display-service'
project(':display-service').projectDir = file('src/apps/display-service')
```

### Root `build.gradle` (conventions only — no `quarkus` plugin at root level)
```groovy
subprojects {
    apply plugin: 'java'

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        compileOnly 'org.projectlombok:lombok:1.18.42'
        annotationProcessor 'org.projectlombok:lombok:1.18.42'
    }
}
```

### Each Quarkus service `build.gradle` must apply `io.quarkus` plugin:
```groovy
plugins {
    id 'io.quarkus'
}

dependencies {
    implementation enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")
    implementation project(':shared-lib')
    // ... service-specific extensions
}
```

> ⚠️ **Important**: Only apply the `io.quarkus` plugin to service modules. `shared-lib` uses `java-library` only.

---

## JPA Entity Scanning from shared-lib

Quarkus by default only scans entities in the current module's JAR. To pick up entities from `shared-lib`:

In each service's `application.properties` / `application.yaml`:
```properties
quarkus.hibernate-orm.packages=com.github.warada_v2.shared.entities
```

Or in `application.yaml`:
```yaml
quarkus:
  hibernate-orm:
    packages: com.github.warada_v2.shared.entities
```

This tells Quarkus Hibernate ORM to also scan the `shared.entities` package from the shared JAR on the classpath.

---

## RabbitMQ Event Publishing (SmallRye Reactive Messaging)

### Producer example — `ComicCrawlProducer` in crawl-service:
```java
@ApplicationScoped
@JBossLog
public class ComicCrawlProducer {

    @Channel("comic-extracted")
    MutinyEmitter<ComicExtractedEvent> emitter;

    public Uni<Void> publish(ComicExtractedEvent event) {
        return emitter.send(event)
            .onFailure().invoke(e -> log.errorf(e, "Failed to publish ComicExtractedEvent for slug=%s", event.slug()));
    }
}
```

### Consumer example — `ComicExtractedConsumer` in core-service:
```java
@ApplicationScoped
@JBossLog
public class ComicExtractedConsumer {

    @Inject
    ComicPersistService persistService;

    @Incoming("comic-extracted")
    @Blocking
    public void consume(ComicExtractedEvent event) {
        try {
            persistService.persistComic(event);
        } catch (ConflictException e) {
            log.warnf("Comic already exists, skipping: slug=%s", event.slug());
            // acknowledge — do not rethrow (prevents DLQ on expected duplicates)
        }
    }
}
```

> ⚠️ **Key rule**: Consumers must `@Blocking` for any JPA operations. Do NOT let JPA run on the Vert.x event loop.

---

## Internal REST Client (crawl-service → core-service)

### Interface in crawl-service:
```java
@RegisterRestClient(configKey = "core-service")
@Path("/internal")
public interface CoreServiceClient {

    @GET
    @Path("/comics/exists")
    @Produces(MediaType.APPLICATION_JSON)
    Response comicExists(@QueryParam("slug") String slug);
}
```

### `application.yaml` (crawl-service):
```yaml
quarkus:
  rest-client:
    core-service:
      url: ${CORE_SERVICE_URL:http://localhost:8082}
      connect-timeout: 3000
      read-timeout: 5000
```

### Usage in `CrawlServiceImpl`:
```java
@Inject
@RestClient
CoreServiceClient coreServiceClient;

private void checkDuplicate(String slug) {
    Response response = coreServiceClient.comicExists(slug);
    if (response.getStatus() == 200) {
        throw new ConflictException("Comic already exists: " + slug);
    }
}
```

---

## SSE Streaming in crawl-service

`CrawlProgressBroadcaster` remains as an `@ApplicationScoped` in-memory broadcaster. The SSE endpoint in crawl-service connects to it directly — no RabbitMQ needed for SSE since it's process-local.

```java
@GET
@Path("/sse/comic")
@Produces(MediaType.SERVER_SENT_EVENTS)
@SseElementType(MediaType.APPLICATION_JSON)
public Multi<CrawlProgressEvent> streamCrawl(@QueryParam("slug-n-id") String slugNId) {
    return broadcaster.getStream(slugNId);
}
```

If multiple crawl-service instances run (horizontal scale), clients must be sticky-routed to the same instance for SSE. Use a load balancer with session affinity (e.g., Nginx `ip_hash`), or replace with a distributed pub/sub (e.g., Redis Streams) for stateless SSE.

---

## Idempotency in core-service

All `persist*` methods must be idempotent. Use Panache's `persistOrUpdate` or handle unique constraint violations:

```java
@Transactional
public void persistComic(ComicExtractedEvent event) {
    if (comicRepository.findBySlug(event.slug()).isPresent()) {
        throw new ConflictException("Comic already exists: " + event.slug());
    }
    Comic comic = new Comic();
    comic.setSlug(event.slug());
    // ... map fields
    comicRepository.persist(comic);

    // publish ChapterPendingEvent for each new chapter
    event.chapters().forEach(ch -> chapterPendingProducer.publish(
        new ChapterPendingEvent(ch.chapterId(), event.slug(), ch.chapterNum(), ch.originUrl(), ch.originPathParams())
    ));
}
```

---

## Dev Mode Per Service

Run individual services in dev mode:
```bash
# Terminal 1 — core-service
./gradlew :core-service:quarkusDev

# Terminal 2 — crawl-service
./gradlew :crawl-service:quarkusDev

# Terminal 3 — display-service
./gradlew :display-service:quarkusDev
```

Each service will use Quarkus Dev Services for its own dependencies (PostgreSQL, RabbitMQ) unless configured to share. To share infrastructure, start Docker Compose manually first and disable Dev Services in `application.yaml`:
```yaml
quarkus:
  devservices:
    enabled: false
```

---

## Build All Modules
```bash
# Build all modules in dependency order
./gradlew build

# Build only a specific service
./gradlew :crawl-service:build

# Run tests for a specific service
./gradlew :core-service:test

# Native build for a specific service
./gradlew :display-service:build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

---

## Package Naming Convention

To avoid conflicts across modules, use module-specific sub-packages:

| Module | Base Package |
|---|---|
| shared-lib | `com.github.warada_v2.shared` |
| crawl-service | `com.github.warada_v2.crawl` |
| core-service | `com.github.warada_v2.core` |
| display-service | `com.github.warada_v2.display` |

---

## Common Gotchas

1. **Lombok `@JBossLog` in shared-lib**: Works if `quarkus-core` is on the compile classpath. Use `compileOnly 'io.quarkus:quarkus-core'` in `shared-lib/build.gradle` or switch to `@Slf4j` for the shared library to avoid the Quarkus dependency.

2. **Liquibase only in core-service**: Liquibase migration must run in `core-service` only. Display-service must start after core-service has completed its migrations. Set `quarkus.liquibase.migrate-at-start=false` in display-service and use a DB readiness check.

3. **JPA `@Converter` classes in shared-lib**: `CrawlStatusConverter` and `ImageTypeConverter` must be in shared-lib and visible to both core-service and display-service. Register them with `@Converter(autoApply = true)`.

4. **SmallRye Reactive Messaging channel names**: Channel names in `@Incoming`/`@Outgoing` must exactly match the keys under `mp.messaging.incoming.*` / `mp.messaging.outgoing.*` in config.

5. **RabbitMQ binding**: Use `mp.messaging.incoming.*.queue.name` and `mp.messaging.outgoing.*.exchange.name` — don't confuse the two. The exchange routes to the queue via a binding key.
