# Native Package Per Service — Design Spec

**Date:** 2026-04-21  
**Status:** Approved

---

## Problem

The project has three Quarkus services (`core-service`, `crawl-service`, `display-service`) and a `shared-lib`, managed as a Gradle multi-project build. Currently:

- No per-service Dockerfiles exist (the `docker-compose.yml` references paths that don't exist yet).
- Existing `scripts/` build scripts are single-service, non-parametric, and don't distinguish between services.
- There is no automated CI pipeline for native builds.

The goal is to produce a **GraalVM native binary** and a **Docker image** for each service, triggered both locally and in CI, with native compilation activated automatically when building for the `prod` profile.

---

## Approach

**Approach A — Gradle-first, per-service native tasks**

Each service is independently buildable. Native compilation is activated via the Quarkus `prod` profile. An aggregation Gradle task at the root level allows building all services at once. Shell scripts and a GitHub Actions matrix job provide full local and CI coverage.

---

## Architecture

```
warada_v2/
├── src/apps/
│   ├── core-service/
│   │   ├── build.gradle              ← native config via prod profile
│   │   └── src/main/docker/
│   │       ├── Dockerfile.native          ← ubi8/ubi-minimal base
│   │       ├── Dockerfile.native-micro    ← quarkus-micro-image base (~10MB)
│   │       └── Dockerfile.ubi-minimal     ← explicit ubi variant alias
│   ├── crawl-service/                ← same structure
│   └── display-service/              ← same structure
├── scripts/
│   ├── core-service/
│   │   ├── build-native.sh
│   │   ├── build-native-micro.sh
│   │   └── build-and-push.sh
│   ├── crawl-service/                ← same structure
│   ├── display-service/              ← same structure
│   └── build-all-native.sh           ← orchestrates all 3 services
├── build.gradle                      ← adds buildProdNative aggregation task
└── .github/workflows/
    └── build-native.yml              ← matrix CI (parallel per service)
```

---

## Component Details

### 1. Gradle Build Configuration

**Per-service `build.gradle`** — each service includes native Quarkus properties that activate on the `prod` profile:

```properties
# In application.properties for each service
%prod.quarkus.native.enabled=true
%prod.quarkus.native.container-build=true
%prod.quarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21
%prod.quarkus.package.jar.enabled=false
```

**Build invocation** per service:
```bash
# Native binary only
./gradlew :crawl-service:build -Dquarkus.profile=prod

# Or via system property in Gradle
./gradlew :core-service:build -Pquarkus.profile=prod
```

**Root `build.gradle`** — adds aggregation task:
```groovy
tasks.register('buildProdNative') {
    dependsOn ':core-service:build', ':crawl-service:build', ':display-service:build'
    // Passes prod profile to all subprojects
}
```

### 2. Dockerfiles (per service, 3 variants)

All Dockerfiles live in `src/apps/<service>/src/main/docker/`.

**`Dockerfile.native`** — uses `ubi8/ubi-minimal`:
```dockerfile
FROM registry.access.redhat.com/ubi8/ubi-minimal:8.10
WORKDIR /work/
RUN chown 1001 /work && chmod "g+rwX" /work && chown 1001:root /work
COPY --chown=1001:root build/*-runner /work/application
EXPOSE 8080
USER 1001
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

**`Dockerfile.native-micro`** — uses `quay.io/quarkus/quarkus-micro-image` (~10MB, production default):
```dockerfile
FROM quay.io/quarkus/quarkus-micro-image:2.0
WORKDIR /work/
RUN chown 1001 /work && chmod "g+rwX" /work && chown 1001:root /work
COPY --chown=1001:root build/*-runner /work/application
EXPOSE 8080
USER 1001
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

**`Dockerfile.ubi-minimal`** — explicit alias to `ubi8/ubi-minimal` (same as `Dockerfile.native`, kept as a named variant for clarity in CI matrix).

**`docker-compose.yml`** — updated service `build.dockerfile` entries to reference `Dockerfile.native-micro` per service.

### 3. Shell Scripts

All scripts follow the existing pattern in `scripts/`:
- Accept `<version>` as `$1` (required)
- Use `GHCR_REGISTRY=ghcr.io/proselin/warada_v2`
- Tag images as `<service-name>:<version>-native-micro`

**Per-service scripts:**

| Script | Purpose |
|--------|---------|
| `build-native.sh` | Runs `./gradlew :<service>:build -Dquarkus.profile=prod` only |
| `build-native-micro.sh` | Native build + `docker build -f Dockerfile.native-micro` |
| `build-and-push.sh` | Native build + docker build + `docker push` to GHCR |

**Root orchestration:**
```bash
scripts/build-all-native.sh <version>
# Calls build-and-push.sh for all 3 services sequentially
```

### 4. GitHub Actions CI

**File:** `.github/workflows/build-native.yml`

**Triggers:**
- `push` to `main` branch
- `workflow_dispatch` with optional `version` input (defaults to `git SHA short`)

**Strategy:** `matrix` with `service: [core-service, crawl-service, display-service]` — runs all 3 in parallel.

**Job steps per service:**
1. `actions/checkout@v4`
2. `actions/setup-java@v4` with `distribution: temurin`, `java-version: 21`
3. Cache Gradle wrapper and caches
4. `./gradlew :<service>:build -Dquarkus.profile=prod`
5. `docker build -f src/apps/<service>/src/main/docker/Dockerfile.native-micro -t ghcr.io/proselin/warada_v2/<service>:<version>-native-micro`
6. `docker login ghcr.io` using `GHCR_TOKEN` secret
7. `docker push`

**Required secrets:** `GHCR_TOKEN` (GitHub personal access token with `write:packages` scope).

---

## Error Handling

- If `quarkus.profile` is not `prod`, native compilation is skipped (JVM build proceeds normally).
- If the container build daemon is unavailable, the Quarkus native build fails with a clear error; scripts exit with `set -e`.
- CI jobs are independent — a failure in one service does not block others (matrix `fail-fast: false`).

---

## Testing

- **Smoke test (local):** After native build, run `./build/*-runner` and hit the `/q/health` endpoint.
- **Docker smoke test:** `docker run --rm -p 8080:8080 <image>` + `curl http://localhost:8080/q/health`.
- **CI:** Health check step in the GitHub Actions workflow after image start.

---

## Out of Scope

- GraalVM native configuration hints / reflection configs (addressed per-service as needed during implementation).
- `shared-lib` native build (it's a library, not a runnable service).
- Multi-arch builds (arm64 / amd64) — can be added later via QEMU in CI.
