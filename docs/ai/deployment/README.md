---
phase: deployment
title: Deployment Strategy
description: Define deployment process, infrastructure, and release procedures
---

# Deployment Strategy

## Infrastructure
**Where will the application run?**

### Required Services
| Service | Image / Version | Purpose |
|---|---|---|
| **PostgreSQL** | `postgres:16-alpine` | Primary database |
| **MinIO** | `minio/minio:RELEASE.2025-01-18T00-31-37Z` | Object storage for images |
| **Warada v2** | `ghcr.io/proselin/warada_v2:<version>-native` | Application (native image) |
| **OTel Collector** | `opentelemetry-collector-contrib:0.140.0` | Observability (optional) |

### Environment Separation

| Env | Profile | Database | MinIO | OTel | Notes |
|---|---|---|---|---|---|
| **Dev** | `%dev` | Local Docker (port 5432) | Local Docker (port 9000) | Disabled | Live coding with `quarkusDev` |
| **Test-Native** | `%test-native` | Local Docker | Local Docker | Disabled | For native image testing |
| **Production** | `%prod` | External (env vars) | External (env vars) | Enabled | Native image in container |

## Deployment Pipeline
**How do we deploy changes?**

### Build Process

#### JVM Build
```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

#### Native Build (requires GraalVM or container build)
```bash
# Local native build
./gradlew build -Dquarkus.native.enabled=true

# Container-based native build (no GraalVM needed locally)
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

#### Docker Image Build
```bash
# Build native Docker image
./scripts/build-native.sh <version>

# Build and push to GHCR
./scripts/build-and-push-native.sh <version>

# Build native-micro (smaller image)
./scripts/build-native-micro.sh <version>
./scripts/build-and-push-native-micro.sh <version>
```

### Docker Images

| Dockerfile | Base Image | Use Case |
|---|---|---|
| `Dockerfile.native` | `ubi9/ubi-minimal:9.6` | Standard native image |
| `Dockerfile.native-micro` | `ubi9-quarkus-micro-image:2.0` | Minimal footprint |
| `Dockerfile.jvm` | — | JVM-based (fallback) |
| `Dockerfile.legacy-jar` | — | Legacy JAR packaging |

### CI/CD Pipeline
Currently manual build and push via scripts. Future CI/CD should:
1. Run `./gradlew test` (unit + integration tests)
2. Build native image: `./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true`
3. Build Docker image: `docker build -f src/main/docker/Dockerfile.native -t ghcr.io/proselin/warada_v2:<tag>-native .`
4. Push to GHCR: `docker push ghcr.io/proselin/warada_v2:<tag>-native`

## Environment Configuration
**What settings differ per environment?**

### Development (`%dev` profile)
```yaml
quarkus.datasource.jdbc.url: jdbc:postgresql://localhost:5432/postgres
quarkus.datasource.username: postgres
quarkus.datasource.password: postgres
quarkus.otel.enabled: false
minio.endpoint: http://localhost:9000
minio.access-key: <from MinIO console>
minio.secret-key: <from MinIO console>
```

### Production (`%prod` profile)
All sensitive values injected via environment variables:

| Env Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL (e.g., `jdbc:postgresql://host:5432/db`) |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `MINIO_ENDPOINT` | MinIO API URL |
| `MINIO_ACCESS_KEY` | MinIO access key |
| `MINIO_SECRET_KEY` | MinIO secret key |
| `MINIO_SECURE` | `true` for HTTPS, `false` for HTTP |
| `OTLP_ENDPOINT` | OpenTelemetry collector gRPC endpoint |

### Production Docker Run Example
```bash
docker run -i --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://postgres:5432/postgres \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e MINIO_ENDPOINT=http://minio:9000 \
  -e MINIO_ACCESS_KEY=<key> \
  -e MINIO_SECRET_KEY=<secret> \
  -e MINIO_SECURE=false \
  -e OTLP_ENDPOINT=http://otel-collector:4317 \
  ghcr.io/proselin/warada_v2:<version>-native
```

## Deployment Steps
**What's the release process?**

### Pre-Deployment Checklist
- [ ] All tests pass (`./gradlew test`)
- [ ] Native build succeeds
- [ ] Database migrations reviewed (new changesets in `db/migration/`)
- [ ] MinIO bucket exists in target environment
- [ ] Environment variables configured in target

### Deployment Execution
1. Build and push Docker image:
   ```bash
   ./scripts/build-and-push-native.sh <version>
   ```
2. Deploy container in target environment with required env vars
3. Application auto-runs Liquibase migrations on startup

### Post-Deployment Validation
- [ ] `GET /zhealth/ping` returns `pong`
- [ ] `GET /q/health` returns healthy status
- [ ] GraphQL query `{ ping }` returns `"Pong"`
- [ ] Test a comic crawl operation

### Rollback Procedure
1. Stop the new container
2. Deploy previous version container image
3. If database migration needs rollback, apply reverse changeset manually

## Database Migrations
**How do we handle schema changes?**

- **Tool:** Liquibase (via `quarkus-liquibase` extension)
- **Master changelog:** `src/main/resources/db/migration/db-changelog-master.yaml`
- **Auto-migrate:** `quarkus.liquibase.migrate-at-start: true` (all profiles)
- **Migration files:**
  - `initialize-270925.sql` — Initial schema (comics, chapters, images, sequences, indexes)
  - `remove-unique-260226.sql` — Schema refinement

### Adding New Migrations
1. Create a new SQL file in `src/main/resources/db/migration/`
2. Add `include` entry in `db-changelog-master.yaml`
3. Test migration locally with `./gradlew quarkusDev`

## Secrets Management
**How do we handle sensitive data?**

- **Development:** Local settings are defined in `application.properties` and backed by Quarkus Dev Services where applicable
- **Production:** All secrets injected via environment variables:
  - `DB_USERNAME`, `DB_PASSWORD`
  - `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`
  - `OTLP_ENDPOINT`
- **Container registry:** GHCR authentication via `docker login ghcr.io`
- **Recommendation:** Use a secret manager (Vault, Kubernetes Secrets) in production

## Rollback Plan
**What if something goes wrong?**

| Trigger | Action |
|---|---|
| Application fails to start | Check logs; redeploy previous version |
| Migration fails | Fix migration SQL; avoid destructive changes |
| Crawling broken | Check NetTruyen HTML structure changes; update regex patterns |
| Health check fails | Check database/MinIO connectivity |
