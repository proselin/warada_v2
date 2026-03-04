---
phase: testing
title: Testing Strategy
description: Define testing approach, test cases, and quality assurance
---

# Testing Strategy

## Test Coverage Goals
**What level of testing do we aim for?**

- **Unit tests:** Cover all service layer business logic and extractors
- **Integration tests:** Verify REST and GraphQL endpoints with test database
- **Native tests:** Validate the application works correctly as a GraalVM native image
- **Current state:** Minimal test coverage — `ExampleResourceTest.java` only

### Test Frameworks
| Framework | Dependency | Purpose |
|---|---|---|
| JUnit 5 | `quarkus-junit5` | Unit and integration testing |
| REST Assured | `rest-assured` | REST API endpoint testing |
| Quarkus Test | `@QuarkusTest` | Full CDI integration testing |
| Quarkus Native Test | `@QuarkusIntegrationTest` | Native image testing |

## Unit Tests
**What individual components need testing?**

### NettruyenExtractorImpl
- [ ] `extractComicHtmlContent` — Parse valid HTML with JSON-LD ComicSeries data
- [ ] `extractComicHtmlContent` — Handle missing/malformed JSON-LD data
- [ ] `extractComicHtmlContent` — Extract slug and comicId from JavaScript variables
- [ ] `getChapterList` — Parse chapter list response and generate path params
- [ ] `extractChapterDetail` — Extract image URLs from `data-sv1`/`data-sv2` attributes
- [ ] `extractChapterDetail` — Handle pages with no images

### CrawlServiceImpl
- [ ] `crawlNettruyenComic` — Successfully crawl and persist a new comic
- [ ] `crawlNettruyenComic` — Throw `ConflictException` for duplicate comic
- [ ] `crawlNettruyenChapter` — Successfully crawl chapter images
- [ ] `crawlNettruyenChapter` — Skip already finished chapters
- [ ] `crawlNettruyenChapter` — Handle chapter not found (throw RuntimeException)
- [ ] `crawlChapter` — Set status to STARTED before processing
- [ ] `crawlChapter` — Set status to FINISHED after successful processing
- [ ] `crawlChapter` — Skip chapters not in INIT status
- [ ] `crawlNettruyenSSEChapter` — Throw `NotFoundException` for missing comic
- [ ] `crawlNettruyenSSEChapter` — Throw `ConflictException` for non-FINISHED comic

### ComicServiceImpl
- [ ] `getPageableComics` — Return paginated comics with default pagination
- [ ] `getPageableComics` — Return paginated comics with custom pagination
- [ ] `findById` — Return comic when found
- [ ] `findById` — Throw `NotFoundException` when not found

### ChapterServiceImpl
- [ ] `getChapterById` — Return chapter when found
- [ ] `getChapterById` — Return null when not found
- [ ] `pageableChapterByComicId` — Return paginated chapters for a comic
- [ ] `getChapterBySlug` — (Currently returns empty chapter — needs implementation)

### PageRequest
- [ ] `toPage` — Convert to Panache Page correctly
- [ ] `toSort` — Return null for empty sortBy
- [ ] `toSort` — Return ascending sort
- [ ] `toSort` — Return descending sort
- [ ] `getOffSet` — Calculate correct offset

### Utils
- [ ] `getExtensionFromUrl` — Extract extension from URL with query params
- [ ] `getExtensionFromUrl` — Return `.jpg` default when no extension found

### FileTempClearer
- [ ] `cleanTempObjects` — Skip when disabled
- [ ] `cleanTempObjects` — Delete objects older than cutoff
- [ ] `cleanTempObjects` — Skip objects newer than cutoff
- [ ] `cleanTempObjects` — Handle MinIO listing errors gracefully

### GlobalExceptionMapper
- [ ] Map `Throwable` to 500 response with `ApiError` body
- [ ] Include unique `processId` in error response

## Integration Tests
**How do we test component interactions?**

### REST API Tests
- [ ] `POST /api/v1/crawl/nettruyen/comic` — Returns 200 on success
- [ ] `POST /api/v1/crawl/nettruyen/comic` — Returns 409 for duplicate
- [ ] `POST /api/v1/crawl/nettruyen/chapter` — Returns 200 on success
- [ ] `GET /api/v1/crawl/nettruyen/sse/comic` — Returns SSE stream
- [ ] `GET /zhealth/ping` — Returns "pong" with 200

### GraphQL API Tests
- [ ] Query `{ ping }` — Returns "Pong"
- [ ] Query `{ comics(pageRequest: {page: 0, size: 10}) { id title } }` — Returns comic list
- [ ] Query `{ comic(id: 1) { id title chapters { id } } }` — Returns comic with chapters
- [ ] Query `{ detailChapterById(id: 1) { id chapterNumber images { id } } }` — Returns chapter detail
- [ ] Query `{ chapters(comicId: 1, pageRequest: {page: 0, size: 10}) { id } }` — Returns paginated chapters

### Database Integration
- [ ] Liquibase migrations run successfully on clean database
- [ ] Comic → Chapter → Image cascade persist works correctly
- [ ] Comic delete cascades to chapters and images
- [ ] Unique constraints enforced (slug, origin_url, origin_id)

### MinIO Integration
- [ ] Image upload to temp path succeeds
- [ ] Image copy (rename) from temp to permanent path succeeds
- [ ] Temp file cleanup deletes old objects

## End-to-End Tests
**What user flows need validation?**

- [ ] Full comic crawl flow: trigger crawl → comic appears in GraphQL query
- [ ] Full chapter crawl flow: trigger chapter crawl → images appear in chapter detail query
- [ ] SSE crawl flow: trigger SSE crawl → receive progress events → all chapters crawled

## Test Data
**What data do we use for testing?**

### Test Fixtures
- Sample NetTruyen HTML pages for extractor testing (mock responses)
- Sample JSON-LD data for comic parsing
- Sample chapter HTML with `data-sv1`/`data-sv2` image tags
- Pre-seeded database records for query testing

### Test Environment
- **Database:** Quarkus DevServices provides auto-configured PostgreSQL for `@QuarkusTest`
- **MinIO:** Requires running MinIO instance or mock
- **NetTruyen:** Should be mocked to avoid external dependency in tests

## Test Reporting & Coverage
**How do we verify and communicate test results?**

### Running Tests
```bash
# Unit + integration tests
./gradlew test

# Native integration tests
./gradlew testNative
```

### Coverage
- Current coverage: **Minimal** (only `ExampleResourceTest.java`)
- Target: All service and extractor logic covered
- Report location: `build/reports/tests/test/index.html`

## Manual Testing
**What requires human validation?**

### Crawl Testing Checklist
- [ ] Crawl a real comic from NetTruyen and verify data in database
- [ ] Crawl chapters via SSE and verify real-time event stream
- [ ] Verify images are accessible in MinIO console
- [ ] Verify GraphQL queries return correct data after crawl

### API Testing Tools
- **GraphQL UI:** `http://localhost:8080/q/graphql-ui` (dev mode)
- **OpenAPI/Swagger:** `http://localhost:8080/q/openapi` or `http://localhost:8080/q/swagger-ui`
- **REST clients:** cURL, Postman, HTTPie

### Sample Test Commands
```bash
# Health check
curl http://localhost:8080/zhealth/ping

# Crawl a comic
curl -X POST http://localhost:8080/api/v1/crawl/nettruyen/comic \
  -H "Content-Type: application/json" \
  -d '{"slugNId": "some-comic-slug-12345"}'

# Query comics via GraphQL
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ comics(pageRequest: {page: 0, size: 5}) { id title slug } }"}'
```

## Performance Testing
**How do we validate performance?**

- Native image startup time should be < 1 second
- GraphQL query response time should be < 200ms for paginated queries
- Image download throughput depends on NetTruyen and network speed
- No formal load testing framework in place yet

## Bug Tracking
**How do we manage issues?**

- Issues tracked via GitHub Issues on the project repository
- Bug severity levels: Critical (app crash), Major (feature broken), Minor (cosmetic/non-blocking)
- All bugs should include: steps to reproduce, expected behavior, actual behavior, logs
