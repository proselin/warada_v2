# Feature: REST Blocking to Reactive — Deployment

## Phase 1 Deployment Impact

Phase 1 is a pure code change with no infrastructure modifications.

---

## Phase 1 — Environment Compatibility

| Component | Requirement | Notes |
|-----------|------------|-------|
| Quarkus | 3.x (RESTEasy Reactive) | Already in use; `quarkus-rest` extension handles `Uni<Response>` natively |
| SmallRye Mutiny | Bundled with Quarkus | No version change needed |
| Java | 21 | Unchanged |
| PostgreSQL | 16 | Unchanged — services still use blocking Hibernate ORM Panache |
| MinIO | Current | Unchanged |

---

## Phase 1 — Build Commands

```bash
./gradlew build
./gradlew buildProdNative
./scripts/build-and-push-native-micro.sh
```

No new build flags or environment variables are required.

---

## Phase 1 — Configuration

Optional tuning for worker pool (if crawl ops saturate pool before Phase 2):
```yaml
quarkus:
  vertx:
    worker-pool-size: 40   # default is 20
```

---

## Phase 2 Deployment Impact (Hibernate Reactive)

Phase 2 replaces the JDBC driver with the reactive PostgreSQL driver.
Infrastructure (PostgreSQL) is unchanged; only the driver protocol changes (JDBC → Vert.x reactive SQL).

### Dependency changes (`build.gradle`)

```groovy
// REMOVE:
implementation 'io.quarkus:quarkus-hibernate-orm-panache'
// ADD:
implementation 'io.quarkus:quarkus-hibernate-reactive-panache'
implementation 'io.quarkus:quarkus-reactive-pg-client'

// NOTE: keep quarkus-jdbc-postgresql if Liquibase is configured to use it (separate datasource)
```

### Configuration changes (`application.yaml`)

```yaml
# Phase 1 (JDBC):
quarkus:
  datasource:
    jdbc:
      url: jdbc:postgresql://localhost:5432/postgres

# Phase 2 (Reactive):
quarkus:
  datasource:
    reactive:
      url: postgresql://localhost:5432/postgres
    # jdbc remains for Liquibase only, disabled for app:
    jdbc:
      url: jdbc:postgresql://localhost:5432/postgres
      transactions: disabled  # reactive driver handles transactions
```

### Liquibase compatibility

Liquibase requires JDBC. Keep `quarkus-jdbc-postgresql` for the `quarkus-liquibase` extension.
Configure Liquibase to use a separate named datasource if needed, or keep the JDBC URL alongside the reactive URL (Quarkus supports this).

---

## Phase 2 — Rollback

```bash
git revert <phase-2-commit-sha>
./gradlew build
# redeploy previous image/jar
```

Schema is unchanged — no Liquibase rollback needed.

---

## Smoke Test After Deploy

```bash
# Ping
curl -f http://<host>/zhealth/ping
# Expected: pong (HTTP 200)

# Create comic (reactive POST)
curl -f -X POST http://<host>/api/v1/comics \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","slug":"test-slug","author":"Author","status":"ongoing"}'
# Expected: HTTP 201

# Crawl trigger (reactive POST)
curl -f -X POST http://<host>/api/v1/crawl/nettruyen/comic \
  -H "Content-Type: application/json" \
  -d '{"slugNId":"some-slug-123"}'
# Expected: HTTP 200 (or 409 if already exists)

# SSE stream
curl -N http://<host>/api/v1/crawl/nettruyen/sse/comic?slug-n-id=some-slug-123
# Expected: SSE event stream (chapter ADD/IGNORE events)
```
# Expected: HTTP 200, body "Success"

# SSE stream (Multi — unchanged)
curl -N http://<host>/api/v1/crawl/nettruyen/sse/comic?slug-n-id=some-slug-123
# Expected: stream of SSE events
```
