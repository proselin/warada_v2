---
phase: deployment
title: CRUD for Comic & Chapter — Deployment Notes
description: Deployment considerations for the Comic/Chapter CRUD feature
---

# Deployment: CRUD for Comic & Chapter

## Deployment Impact

### No Database Migration Required
All CRUD operations use existing tables (`comics`, `chapters`). No new Liquibase changesets are needed.

### No Configuration Changes Required
No new `application.properties` keys are required. The feature uses existing CDI beans, repositories, and infrastructure.

### Binary Size Impact
Minimal — only new Java classes (no new Quarkus extensions added).

## Verification Steps After Deployment

### Smoke Tests
Run these after deploying to any environment:

```bash
BASE=http://localhost:8080

# 1. Create a comic
curl -s -X POST "$BASE/api/v1/comics" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Test Comic","slug":"test-comic-deploy","status":"ongoing"}' | jq .

# 2. Get the comic via GraphQL
curl -s -X POST "$BASE/graphql" \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ comic(id: 1) { id title slug } }"}' | jq .

# 3. Update the comic via GraphQL mutation
curl -s -X POST "$BASE/graphql" \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { updateComic(id: 1, input: { title: \"Updated\" }) { id title } }"}' | jq .

# 4. Create a chapter
curl -s -X POST "$BASE/api/v1/chapters" \
  -H 'Content-Type: application/json' \
  -d '{"comic_id":1,"chapter_number":"Chapter 1","position":1}' | jq .

# 5. Update the chapter via GraphQL mutation
curl -s -X POST "$BASE/graphql" \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { updateChapter(id: 1, input: { position: 99 }) { id position } }"}' | jq .

# 6. Delete the chapter
curl -s -X DELETE "$BASE/api/v1/chapters/1" -o /dev/null -w "%{http_code}"

# 7. Delete the comic
curl -s -X DELETE "$BASE/api/v1/comics/1" -o /dev/null -w "%{http_code}"
```

### Expected Responses
| Step | Expected HTTP Status |
|---|---|
| Create comic | 201 |
| Get comic (GraphQL) | 200 |
| Update comic (GraphQL) | 200 |
| Create chapter | 201 |
| Update chapter (GraphQL) | 200 |
| Delete chapter | 204 |
| Delete comic | 204 |

## Native Image Build
The new classes use standard CDI, JAX-RS, and GraphQL annotations — all fully supported by GraalVM native image. No additional reflection configuration in `reflect-config.json` is required.

Build commands remain unchanged:
```bash
# Container native build
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true

# Production native
./gradlew buildProdNative
```

## Rollback Plan
If a defect is found post-deployment:
1. Redeploy the previous image tag — the new endpoints will not be available.
2. No database rollback needed (no schema changes were made).
3. Any comics/chapters created via the new API will remain in the database and are accessible via existing GraphQL queries.
