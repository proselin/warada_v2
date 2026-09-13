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
| **Garage** | `docker.io/dxflrs/garage:v2.3.0` | S3-compatible object storage for images |
| **Warada v2** | `ghcr.io/proselin/warada_v2:<version>-native` | Application (native image) |
| **OTel Collector** | `opentelemetry-collector-contrib:0.140.0` | Observability (optional) |

### Environment Separation

| Env | Profile | Database | Garage | OTel | Notes |
|---|---|---|---|---|---|
| **Dev** | `%dev` | Local container (port 5432) | Local container (port 3900) | Disabled | Live coding with `quarkusDev` |
| **Test-Native** | `%test-native` | Local container | Local container | Disabled | For native image testing |
| **Production** | `%prod` | External (env vars) | External (S3 env vars) | Enabled | Native image in container |

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
# Build native container image
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
3. Build container image: `podman build -f backend/src/main/docker/Dockerfile.native -t ghcr.io/proselin/warada_v2:<tag>-native backend`
4. Push to GHCR: `docker push ghcr.io/proselin/warada_v2:<tag>-native`

## Environment Configuration
**What settings differ per environment?**

### Development (`%dev` profile)
```yaml
quarkus.datasource.jdbc.url: jdbc:postgresql://localhost:5432/postgres
quarkus.datasource.username: postgres
quarkus.datasource.password: postgres
quarkus.otel.enabled: false
S3 endpoint: http://localhost:3900
S3 access key: <from Garage key create>
S3 secret key: <from Garage key create>
```

### Production (`%prod` profile)
All sensitive values injected via environment variables:

| Env Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL (e.g., `jdbc:postgresql://host:5432/db`) |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `S3_ENDPOINT` | Garage/S3 API host |
| `S3_ACCESS_KEY` | S3 access key |
| `S3_SECRET_KEY` | S3 secret key |
| `S3_SECURE` | `true` for HTTPS, `false` for HTTP |
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
