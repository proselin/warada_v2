---
phase: testing
title: CRUD for Comic & Chapter — Testing Strategy
description: Test cases and coverage plan for the Comic/Chapter CRUD feature
---

# Testing: CRUD for Comic & Chapter

## Unit Tests

### `ComicServiceImplTest` (new test cases)
- [ ] `createComic` — successfully creates a comic with tags resolved via `TagService`
- [ ] `createComic` — creates a comic without tags (empty tag list)
- [ ] `createComic` — sets default status "ongoing" when status is null
- [ ] `createComic` — sets `crawlingStatus = INIT` and `chapterCount = 0`
- [ ] `updateComic` — updates only non-null fields (partial update)
- [ ] `updateComic` — throws `ResourceNotFoundException` when comic not found
- [ ] `deleteComic` — calls `comicRepository.deleteById(id)` successfully
- [ ] `deleteComic` — throws `ResourceNotFoundException` when `deleteById` returns false

### `ChapterServiceImplTest` (new test cases)
- [ ] `createChapter` — successfully creates chapter linked to existing comic
- [ ] `createChapter` — throws `ResourceNotFoundException` when `comicId` does not exist
- [ ] `createChapter` — sets `crawlingStatus = INIT` on new chapter
- [ ] `updateChapter` — updates only non-null fields (partial update)
- [ ] `updateChapter` — throws `ResourceNotFoundException` when chapter not found
- [ ] `deleteChapter` — calls `chapterRepository.deleteById(id)` successfully
- [ ] `deleteChapter` — throws `ResourceNotFoundException` when `deleteById` returns false
- [ ] `getChapterBySlug` — now returns result from `chapterRepository.findBySlug` (fixed stub)

### `ComicRestResourceTest` (new)
- [ ] `POST /api/v1/comics` — returns 201 with comic JSON on valid request
- [ ] `DELETE /api/v1/comics/{id}` — returns 204 on success
- [ ] `DELETE /api/v1/comics/{id}` — returns 404 when `ResourceNotFoundException` is thrown

### `ChapterRestResourceTest` (new)
- [ ] `POST /api/v1/chapters` — returns 201 with `ChapterDetailGql` JSON on valid request
- [ ] `POST /api/v1/chapters` — returns 404 when comicId not found
- [ ] `DELETE /api/v1/chapters/{id}` — returns 204 on success
- [ ] `DELETE /api/v1/chapters/{id}` — returns 404 when `ResourceNotFoundException` is thrown

### `ComicGqlResourceTest` (new mutation test cases)
- [ ] `updateComic` — calls `comicService.updateComic(id, input)` and returns result
- [ ] `updateComic` — propagates `ResourceNotFoundException` from service

### `ChapterGqlResourceTest` (new mutation test cases)
- [ ] `updateChapter` — calls `chapterService.updateChapter(id, input)` and returns `ChapterDetailGql`
- [ ] `updateChapter` — propagates `ResourceNotFoundException` from service

### `ResourceNotFoundExceptionMapperTest` (new)
- [ ] Returns HTTP 404 with `ApiError` JSON body
- [ ] `ApiError.status` equals `404`
- [ ] `ApiError.message` equals the exception message
- [ ] `ApiError.processId` is a non-null UUID string

## Sample GraphQL Test Queries

### Update Comic
```graphql
mutation {
  updateComic(id: 1, input: { title: "Updated Title" }) {
    id
    title
    updatedAt
  }
}
```

### Update Chapter
```graphql
mutation {
  updateChapter(id: 5, input: { position: 10 }) {
    id
    position
    updatedAt
  }
}
```

## Sample REST curl Commands

```bash
# Create a comic
curl -X POST http://localhost:8080/api/v1/comics \
  -H 'Content-Type: application/json' \
  -d '{"title":"My Comic","slug":"my-comic","status":"ongoing"}'

# Delete a comic
curl -X DELETE http://localhost:8080/api/v1/comics/1

# Create a chapter
curl -X POST http://localhost:8080/api/v1/chapters \
  -H 'Content-Type: application/json' \
  -d '{"comic_id":1,"chapter_number":"Chapter 1","position":1}'

# Delete a chapter
curl -X DELETE http://localhost:8080/api/v1/chapters/5
```
