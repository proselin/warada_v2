# Feature: CRUD for Comic & Chapter (2026-03-13)

## Overview
This document describes the full implementation of CRUD operations for the `Comic` and `Chapter` resources in `warada_v2`.

## API Contract

| Operation | Resource | Protocol | Endpoint / Operation |
|---|---|---|---|
| **Create** | Comic | REST | `POST /api/v1/comics` |
| **Read** | Comic | GraphQL | `query { comic(id) }`, `query { comics(...) }` |
| **Update** | Comic | GraphQL | `mutation { updateComic(id, input) }` |
| **Delete** | Comic | REST | `DELETE /api/v1/comics/{id}` |
| **Create** | Chapter | REST | `POST /api/v1/chapters` |
| **Read** | Chapter | GraphQL | `query { detailChapterById(id) }`, `query { detailChapterBySlug(slug) }`, `query { chapters(comicId, ...) }` |
| **Update** | Chapter | GraphQL | `mutation { updateChapter(id, input) }` |
| **Delete** | Chapter | REST | `DELETE /api/v1/chapters/{id}` |

## Implementation Stages

### Stage 1: Request/Response Models
New Java records (`models/` package):
- `CreateComicRequest` — title, description, author, status, slug, originId, originUrl, originPathParams, tags
- `UpdateComicRequest` — title, description, author, status (all optional/nullable fields)
- `CreateChapterRequest` — comicId, chapterNumber, type, position, originUrl, originPathParams
- `UpdateChapterRequest` — chapterNumber, type, position (all optional/nullable fields)

### Stage 2: Service Layer
Added three new methods to `ComicService` / `ChapterService` interfaces and implemented in their `*Impl` classes:
- `createComic(CreateComicRequest)` / `createChapter(CreateChapterRequest)` — validates parent entity, builds entity, persists via Panache
- `updateComic(Long, UpdateComicRequest)` / `updateChapter(Long, UpdateChapterRequest)` — partial update (null-safe per field)
- `deleteComic(Long)` / `deleteChapter(Long)` — delegates to `panache.deleteById`, throws `ResourceNotFoundException` if not found

### Stage 3: GraphQL Mutations
`@Mutation` methods added to `ComicGqlResource` and `ChapterGqlResource`:
- `updateComic(id, input: UpdateComicRequest)` → returns updated `Comic`
- `updateChapter(id, input: UpdateChapterRequest)` → returns updated `ChapterDetailGql`

### Stage 4: REST Endpoints
Two new JAX-RS resources:
- `ComicRestResource` at `/api/v1/comics` — `POST /` (create), `DELETE /{id}` (delete)
- `ChapterRestResource` at `/api/v1/chapters` — `POST /` (create), `DELETE /{id}` (delete)

### Stage 5: Exception Handling
Added `ResourceNotFoundExceptionMapper` (`@Provider`) to map `ResourceNotFoundException` → HTTP 404 JSON response, consistent with the existing `ApiError` schema.

## Key Decisions
- Updates use **partial update** semantics: only non-null fields in the request are applied.
- Create comic initialises `crawlingStatus = INIT`, `chapterCount = 0` by default.
- Create chapter requires a valid `comicId`; throws 404 if the comic does not exist.
- Tags on comic creation are resolved/created via the existing `TagService.resolveOrCreateTags`.
- `getChapterBySlug` stub was fixed to use `chapterRepository.findBySlug`.
