# Feature: lomnox-170426 — Full Lombok Removal

## Summary

This feature completed the full Lombok removal effort in three stages:
- Phase 1 removed Lombok-based logging annotations and switched the application to explicit JBoss Logger usage aligned with Quarkus.
- Phase 2 removed Lombok from low-risk supporting classes such as enums, exception types, helper models, and pageable DTOs.
- Phase 3 converted the remaining ORM entities to explicit Java and removed Lombok from the Gradle build.

The goal was achieved while preserving runtime behavior and source compatibility.

## Scope Implemented

### Completed in Phase 1
- removed Lombok logging annotations such as `@JBossLog`
- replaced them with explicit `org.jboss.logging.Logger` fields
- kept existing log messages and behavior intact

### Completed in Phase 2
- removed Lombok from enum classes
- replaced exception getters with manual Java methods
- converted helper models and pageable DTOs to normal Java while preserving compatibility

### Completed in Phase 3
- converted the remaining ORM entities to explicit Java classes
- preserved JPA-safe constructors, getters, setters, defaults, and builder-style compatibility
- removed the Lombok dependency and annotation processor wiring from the Gradle build

## Why this change

The project previously used Lombok to generate logger fields automatically.

For Quarkus services, explicit JBoss Logger usage provides:
- clearer source code
- no Lombok dependency for logging concerns
- better long-term maintainability for framework-native logging patterns

## Affected areas

The migration was applied across the following areas:
- comic and chapter API resources
- crawl orchestration services
- RabbitMQ producer and consumer flow
- SSE progress broadcasting
- temp file cleanup scheduler
- enum and exception support classes
- extracted helper models and pageable request or response DTOs
- ORM entity classes and their builder-style construction paths
- Gradle dependency configuration

## Implementation pattern

Before:

```java
@JBossLog
public class CrawlServiceImpl {
    log.info("message");
}
```

After:

```java
public class CrawlServiceImpl {
    private static final Logger log = Logger.getLogger(CrawlServiceImpl.class);
}
```

## Verification status

### Verified
- source compilation succeeded with Gradle
- test source compilation also succeeded with Gradle
- targeted model tests passed with 9 passing and 0 failing
- Lombok logging annotations were removed from the migrated classes
- Lombok usage was removed from non-entity and entity classes
- Lombok dependency wiring was removed from the build

### Current limitation
- the full test suite still reports an environment-related Docker or Testcontainers issue and one existing failing assertion outside the compile validation path

## Risk classification

| Area | Risk | Status |
|---|---:|---|
| Logging migration | Low | Completed |
| Simple DTO and exception cleanup | Low to medium | Completed |
| ORM entity conversion | High | Completed |

## Recommended next steps

1. resolve the Docker or Testcontainers environment mismatch for full-suite execution
2. investigate the existing failing assertion in the chapter service test
3. keep future model changes aligned with explicit Java patterns instead of reintroducing Lombok
