# Native Package Per Service — Implementation Tracker

**Date:** 2026-04-21  
**Design Ref:** [../2026-04-21-native-package-per-service-design.md](../2026-04-21-native-package-per-service-design.md)  
**Status:** In Progress

---

## Implementation Checklist

| # | Component | Status | Notes |
|---|-----------|--------|-------|
| 1 | Gradle multi-project structure (`settings.gradle`) | ✅ Done | All 3 services + shared-lib registered |
| 2 | Per-service `build.gradle` | ✅ Done | core-service, crawl-service, display-service |
| 3 | Per-service `application.properties` | ✅ Done (partial) | `%prod` native build props missing on core & crawl |
| 4 | `Dockerfile.native` per service | ✅ Done | Added to all 3 services |
| 5 | `Dockerfile.native-micro` per service | ✅ Done | Added to all 3 services |
| 6 | Root `buildProdNative` task (fixed) | ✅ Done | Replaced broken `System.setProperty` approach |
| 7 | Per-service `scripts/<service>/build-native.sh` | ✅ Done | All 3 services |
| 8 | Per-service `scripts/<service>/build-native-micro.sh` | ✅ Done | All 3 services |
| 9 | Per-service `scripts/<service>/build-and-push.sh` | ✅ Done | All 3 services |
| 10 | Root `scripts/build-all-native.sh` | ✅ Done | Orchestrates all 3 services |
| 11 | Old flat scripts removed/replaced | ✅ Done | Replaced with per-service structure |
| 12 | GitHub Actions `build-native.yml` (matrix) | ✅ Done | Replaced old monolithic `cicd.yaml` |

---

## Gap Analysis (vs. Design Doc)

### Was Missing Before This Implementation

1. **No Dockerfiles anywhere** — the old `scripts/build-native.sh` referenced `src/main/docker/Dockerfile.native` (root-level, monolithic), which never existed. The design requires Dockerfiles at `src/apps/<service>/src/main/docker/`.

2. **No `%prod` native build properties** — `core-service` and `crawl-service` `application.properties` had no `%prod.*` native config. `display-service` had `%prod.*` DB config but also lacked native build flags.

3. **Root `buildProdNative` task was broken** — used `System.setProperty("quarkus.profile", "prod")` inside `doFirst`, which runs after Quarkus plugin reads configuration, making native activation a no-op. Fixed to use `-Dquarkus.profile=prod` at the command line.

4. **Scripts were monolithic** — `scripts/build-native.sh` etc. had hard-coded single-service paths (`src/main/docker/Dockerfile.native` at root). Per-service directories under `scripts/` were absent.

5. **No `scripts/build-all-native.sh`** — no root orchestration script.

6. **GitHub Actions `cicd.yaml` was monolithic and broken** — referenced the non-existent `src/main/docker/Dockerfile.native` path and built all services as one job. Replaced with a matrix strategy (`fail-fast: false`) running each service in parallel.

---

## Architecture (Implemented)

```
warada_v2/
├── src/apps/
│   ├── core-service/src/main/docker/
│   │   ├── Dockerfile.native           ✅ ubi8/ubi-minimal base
│   │   └── Dockerfile.native-micro     ✅ quarkus-micro-image base
│   ├── crawl-service/src/main/docker/
│   │   ├── Dockerfile.native           ✅
│   │   └── Dockerfile.native-micro     ✅
│   └── display-service/src/main/docker/
│       ├── Dockerfile.native           ✅
│       └── Dockerfile.native-micro     ✅
├── scripts/
│   ├── core-service/
│   │   ├── build-native.sh             ✅
│   │   ├── build-native-micro.sh       ✅
│   │   └── build-and-push.sh           ✅
│   ├── crawl-service/                  ✅ same structure
│   ├── display-service/                ✅ same structure
│   └── build-all-native.sh             ✅ orchestrates all 3
├── build.gradle                        ✅ fixed buildProdNative task
└── .github/workflows/
    └── build-native.yml                ✅ matrix CI (parallel per service)
```

---

## Usage

### Local — single service
```bash
# Native binary only
./scripts/core-service/build-native.sh

# Native + Docker image (micro, production default)
./scripts/crawl-service/build-native-micro.sh 1.0.0

# Native + Docker build + push to GHCR
./scripts/display-service/build-and-push.sh 1.0.0
```

### Local — all services at once
```bash
./scripts/build-all-native.sh 1.0.0
```

### Gradle direct
```bash
# Single service
./gradlew :crawl-service:build -Dquarkus.profile=prod

# All services (via aggregation task)
./gradlew buildProdNative -Dquarkus.profile=prod
```

### CI
Push to `main` branch triggers `.github/workflows/build-native.yml` — matrix job builds all 3 services in parallel.

---

## Testing

### Smoke test (after native build)
```bash
# Run the native binary directly
./src/apps/core-service/build/*-runner -Dquarkus.http.host=0.0.0.0
curl http://localhost:8082/q/health

# Docker smoke test
docker run --rm -p 8082:8082 ghcr.io/proselin/warada_v2/core-service:1.0.0-native-micro
curl http://localhost:8082/q/health
```

### Verify build config (dry-run)
```bash
./gradlew :core-service:tasks --group=build
```

---

## Known Limitations / Out of Scope

- GraalVM native reflection hints per-service — handled as needed during actual native compilation
- `shared-lib` native build — it's a library, not runnable
- Multi-arch builds (arm64/amd64) — deferred to future iteration
- `docker-compose.dev.yml` with JVM Dockerfiles — deferred; current `docker-compose.yml` remains infra-only
