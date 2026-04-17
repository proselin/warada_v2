# Feature: lomnox-170426 — Lombok Logging Removal Phase 1

## Summary

This feature starts the Lombok reduction effort by removing Lombok-based logging annotations and switching the application to explicit JBoss Logger usage, which is natively aligned with Quarkus.

The goal of this phase is to reduce annotation-driven logging while keeping behavior unchanged.

## Scope Implemented

### Included in Phase 1
- remove Lombok logging annotations such as `@JBossLog`
- replace them with explicit `org.jboss.logging.Logger` fields
- keep existing log messages and operational behavior intact

### Not included yet
- full Lombok dependency removal from the build
- ORM entity conversion to normal Java classes
- DTO, enum, and exception cleanup

## Why this change

The project previously used Lombok to generate logger fields automatically.

For Quarkus services, explicit JBoss Logger usage provides:
- clearer source code
- no Lombok dependency for logging concerns
- better long-term maintainability for framework-native logging patterns

## Affected areas

The migration was applied to representative resource, service, and scheduler classes, including:
- comic and chapter API resources
- crawl orchestration services
- RabbitMQ producer and consumer flow
- SSE progress broadcasting
- temp file cleanup scheduler

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
- source compilation succeeded with Gradle using the project compile task
- Lombok logging annotations were removed from the migrated classes

### Current limitation
- the full test suite is not fully green yet because the local test environment still has Docker or Testcontainers issues and one existing failing assertion

## Risk classification

| Area | Risk | Status |
|---|---:|---|
| Logging migration | Low | Completed |
| Simple DTO and exception cleanup | Low to medium | Pending |
| ORM entity conversion | High | Deferred |

## Recommended next steps

1. remove Lombok from simple supporting classes
2. verify all remaining Lombok usages by category
3. handle entity conversion only as a separate focused refactor
4. remove the Lombok dependency from the build after usage reaches zero
