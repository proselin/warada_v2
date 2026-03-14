---
phase: implementation
title: CRUD for Comic & Chapter — Implementation Guide
description: Technical implementation notes for the Comic/Chapter CRUD feature
---

# Implementation: CRUD for Comic & Chapter

## New / Modified Files

| File | Type | Change |
|---|---|---|
| `models/CreateComicRequest.java` | New | Record DTO for comic creation |
| `models/UpdateComicRequest.java` | New | Record DTO for comic update |
| `models/CreateChapterRequest.java` | New | Record DTO for chapter creation |
| `models/UpdateChapterRequest.java` | New | Record DTO for chapter update |
| `services/ComicService.java` | Modified | Added `createComic`, `updateComic`, `deleteComic` |
| `services/ComicServiceImpl.java` | Modified | Implemented the three new methods + `@JBossLog` |
| `services/ChapterService.java` | Modified | Added `createChapter`, `updateChapter`, `deleteChapter` |
| `services/ChapterServiceImpl.java` | Modified | Implemented the three new methods + fixed `getChapterBySlug` stub |
| `resources/ComicGqlResource.java` | Modified | Added `@Mutation updateComic` |
| `resources/ChapterGqlResource.java` | Modified | Added `@Mutation updateChapter` |
| `resources/ComicRestResource.java` | New | REST resource `POST /api/v1/comics`, `DELETE /api/v1/comics/{id}` |
| `resources/ChapterRestResource.java` | New | REST resource `POST /api/v1/chapters`, `DELETE /api/v1/chapters/{id}` |
| `exceptions/ResourceNotFoundExceptionMapper.java` | New | Maps `ResourceNotFoundException` → HTTP 404 + `ApiError` |

## Key Implementation Patterns

### Partial Update (Null-safe)
```java
@Transactional
public Comic updateComic(Long id, UpdateComicRequest request) {
    Comic comic = comicRepository.findByIdOptional(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comic", "id", id));
    if (request.title() != null) comic.setTitle(request.title());
    if (request.description() != null) comic.setDescription(request.description());
    if (request.author() != null) comic.setAuthor(request.author());
    if (request.status() != null) comic.setStatus(request.status());
    comicRepository.persist(comic);
    return comic;
}
```

### Create with Tag Resolution
```java
@Transactional
public Comic createComic(CreateComicRequest request) {
    Comic comic = Comic.builder()
            .title(request.title())
            .slug(request.slug())
            .crawlingStatus(CrawlStatus.INIT)
            .chapterCount(0)
            .tags(new ArrayList<>())
            .build();
    if (request.tags() != null && !request.tags().isEmpty()) {
        comic.setTags(tagService.resolveOrCreateTags(request.tags()));
    }
    comicRepository.persist(comic);
    return comic;
}
```

### GraphQL Mutation Pattern
```java
@Mutation("updateComic")
@Blocking
public Comic updateComic(@Name("id") Long id, @Name("input") UpdateComicRequest input) {
    return comicService.updateComic(id, input);
}
```

### REST Resource Pattern
```java
@POST
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response createComic(CreateComicRequest request) {
    var comic = comicService.createComic(request);
    return Response.status(Response.Status.CREATED).entity(comic).build();
}

@DELETE
@Path("/{id}")
@Produces(MediaType.TEXT_PLAIN)
public Response deleteComic(@PathParam("id") Long id) {
    comicService.deleteComic(id);
    return Response.noContent().build();
}
```

### Exception Mapper
```java
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {
    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        ApiError error = new ApiError(UUID.randomUUID().toString(), 404, exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND)
                .entity(error).type(MediaType.APPLICATION_JSON).build();
    }
}
```

## Conventions Followed
- `@JBossLog` used on all service implementations for structured logging.
- `@Transactional` on service write methods only (not repositories).
- `@Blocking` on all GraphQL methods that touch the database.
- Java `record` types for all request DTOs — immutable, concise, no boilerplate.
- JSON field names use `snake_case` via `@JsonProperty` on record components.
