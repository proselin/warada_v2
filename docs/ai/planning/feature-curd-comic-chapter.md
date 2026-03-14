---
phase: planning
title: CRUD for Comic & Chapter — Planning
description: Task breakdown and milestones for the Comic/Chapter CRUD feature
---

# Planning: CRUD for Comic & Chapter

## Milestone
**Milestone 9: Direct CRUD API for Comic & Chapter**

## Task Breakdown

### Phase 1: Models ✅
- [x] Task 1.1: Create `CreateComicRequest` record
- [x] Task 1.2: Create `UpdateComicRequest` record
- [x] Task 1.3: Create `CreateChapterRequest` record
- [x] Task 1.4: Create `UpdateChapterRequest` record

### Phase 2: Service Layer ✅
- [x] Task 2.1: Add `createComic`, `updateComic`, `deleteComic` to `ComicService` interface
- [x] Task 2.2: Implement `createComic` in `ComicServiceImpl` (tag resolution, entity build, persist)
- [x] Task 2.3: Implement `updateComic` in `ComicServiceImpl` (partial update, persist)
- [x] Task 2.4: Implement `deleteComic` in `ComicServiceImpl` (deleteById, 404 guard)
- [x] Task 2.5: Add `createChapter`, `updateChapter`, `deleteChapter` to `ChapterService` interface
- [x] Task 2.6: Implement `createChapter` in `ChapterServiceImpl` (validate comicId, build, persist)
- [x] Task 2.7: Implement `updateChapter` in `ChapterServiceImpl` (partial update, persist)
- [x] Task 2.8: Implement `deleteChapter` in `ChapterServiceImpl` (deleteById, 404 guard)
- [x] Task 2.9: Fix `getChapterBySlug` stub to use repository

### Phase 3: API Layer ✅
- [x] Task 3.1: Add `@Mutation updateComic` to `ComicGqlResource`
- [x] Task 3.2: Add `@Mutation updateChapter` to `ChapterGqlResource`
- [x] Task 3.3: Create `ComicRestResource` with `POST /api/v1/comics` and `DELETE /api/v1/comics/{id}`
- [x] Task 3.4: Create `ChapterRestResource` with `POST /api/v1/chapters` and `DELETE /api/v1/chapters/{id}`

### Phase 4: Exception Handling ✅
- [x] Task 4.1: Create `ResourceNotFoundExceptionMapper` to map `ResourceNotFoundException` → HTTP 404 + `ApiError` JSON

### Phase 5: Testing 🔲
- [ ] Task 5.1: Unit tests for `ComicServiceImpl` create/update/delete
- [ ] Task 5.2: Unit tests for `ChapterServiceImpl` create/update/delete
- [ ] Task 5.3: Unit tests for `ComicRestResource` and `ChapterRestResource`
- [ ] Task 5.4: Unit tests for `ComicGqlResource` and `ChapterGqlResource` mutations
- [ ] Task 5.5: Unit tests for `ResourceNotFoundExceptionMapper`

## Dependencies
```
CreateComicRequest / UpdateComicRequest / CreateChapterRequest / UpdateChapterRequest
    └── ComicService / ChapterService interface additions
        └── ComicServiceImpl / ChapterServiceImpl implementations
            ├── ComicGqlResource (mutation)
            ├── ChapterGqlResource (mutation)
            ├── ComicRestResource (create, delete)
            └── ChapterRestResource (create, delete)
ResourceNotFoundExceptionMapper (supports all 404 responses)
```
