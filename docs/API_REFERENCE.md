# API Reference — Warada v2

Base URL (dev): `http://localhost:8080`

---

## Table of Contents
- [Common Types](#common-types)
- [Error Responses](#error-responses)
- [REST API](#rest-api)
  - [Comics — Create](#post-apiv1comics)
  - [Comics — Delete](#delete-apiv1comicsid)
  - [Chapters — Create](#post-apiv1chapters)
  - [Chapters — Delete](#delete-apiv1chaptersid)
  - [Crawl — Trigger Comic Crawl](#post-apiv1crawlnettruyencomic)
  - [Crawl — Trigger Chapter Crawl](#post-apiv1crawlnettruyenchapter)
  - [Crawl — SSE Chapter Stream](#get-apiv1crawlnettruyenssecomic)
- [GraphQL API](#graphql-api)
  - [Queries](#queries)
    - [ping](#ping)
    - [comic](#comic)
    - [comics](#comics)
    - [detailChapterById](#detailchapterbyid)
    - [detailChapterBySlug](#detailchapterbyslug)
    - [chapters](#chapters)
    - [tag](#tag)
    - [tags](#tags)
  - [Mutations](#mutations)
    - [updateComic](#updatecomic)
    - [updateChapter](#updatechapter)

---

## Common Types

### `CrawlStatus`
String enum — represents the crawl pipeline state of a resource.

| Value | Meaning |
|---|---|
| `INIT` | Created, not yet crawled |
| `STARTED` | Crawl in progress |
| `FINISHED` | Crawl complete |

### `PageRequest` (GraphQL input)
Used in paginated query arguments.

| Field | Type | Default | Description |
|---|---|---|---|
| `page` | `Int` | `0` | Page number, 0-based |
| `size` | `Int` | `20` | Items per page |
| `sortBy` | `String` | `null` | Field name to sort by |
| `sortOrder` | `String` | `"asc"` | `"asc"` or `"desc"` |

### `Comic` (response object)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | |
| `title` | `String` | |
| `description` | `String` | |
| `author` | `String` | |
| `status` | `String` | e.g. `"ongoing"`, `"completed"` |
| `slug` | `String` | URL-safe unique identifier |
| `originId` | `String` | ID from source site |
| `originUrl` | `String` | Source URL |
| `originPathParams` | `String` | Source path params |
| `chapterCount` | `Int` | |
| `crawlingStatus` | `CrawlStatus` | |
| `thumbImage` | `Image` | Thumbnail image object |
| `tags` | `[Tag]` | |
| `createdAt` | `DateTime` | ISO-8601 |
| `updatedAt` | `DateTime` | ISO-8601 |

### `ChapterDetail` (response object)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | |
| `chapterNumber` | `String` | e.g. `"Chapter 1"` |
| `sourceUrl` | `String` | Original URL on source site |
| `originPathParams` | `String` | |
| `type` | `String` | |
| `position` | `Int` | Sort order within comic |
| `crawlingStatus` | `CrawlStatus` | |
| `comic` | `Comic` | Parent comic (may be lazy-loaded/null) |
| `images` | `[Image]` | Chapter page images |
| `createdAt` | `DateTime` | ISO-8601 |
| `updatedAt` | `DateTime` | ISO-8601 |

### `Image` (response object)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | |
| `fileName` | `String` | |
| `filePath` | `String` | Path within MinIO bucket |
| `originUrl` | `String` | |
| `position` | `Int` | |
| `type` | `String` | `"THUMB"` or `"CHAPTER_IMAGE"` |
| `createdAt` | `DateTime` | ISO-8601 |
| `updatedAt` | `DateTime` | ISO-8601 |

### `Tag` (response object)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | |
| `name` | `String` | Display name |
| `normalizedName` | `String` | Lowercase, normalized |
| `comicCount` | `Int` | |
| `createdAt` | `DateTime` | ISO-8601 |
| `updatedAt` | `DateTime` | ISO-8601 |

---

## Error Responses

All REST errors return JSON with the following shape:

```json
{
  "processId": "550e8400-e29b-41d4-a716-446655440000",
  "status": 404,
  "message": "Comic not found with id: 99",
  "timestamp": "2026-03-13T10:00:00Z"
}
```

| HTTP Status | Cause |
|---|---|
| `400 Bad Request` | Business rule violation |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate unique field (e.g. slug already exists) |
| `500 Internal Server Error` | Unexpected server error |

---

## REST API

### `POST /api/v1/comics`
Create a new comic record directly (without crawling).

**Request**
```
Content-Type: application/json
```
```json
{
  "title": "One Piece",
  "description": "A pirate adventure story.",
  "author": "Eiichiro Oda",
  "status": "ongoing",
  "slug": "one-piece",
  "origin_id": "12345",
  "origin_url": "https://nettruyenar.com/truyen-tranh/one-piece-12345",
  "origin_path_params": "one-piece-12345",
  "tags": ["Action", "Adventure", "Fantasy"]
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | string | No | |
| `description` | string | No | |
| `author` | string | No | |
| `status` | string | No | Defaults to `"ongoing"` |
| `slug` | string | **Yes** | Must be unique |
| `origin_id` | string | No | Must be unique if provided |
| `origin_url` | string | No | Must be unique if provided |
| `origin_path_params` | string | No | Must be unique if provided |
| `tags` | string[] | No | Tag names; created if they don't exist |

**Response `201 Created`**
Returns the created [`Comic`](#comic-response-object) object.

```json
{
  "id": 1,
  "title": "One Piece",
  "slug": "one-piece",
  "status": "ongoing",
  "crawlingStatus": "INIT",
  "chapterCount": 0,
  "createdAt": "2026-03-13T10:00:00",
  "updatedAt": "2026-03-13T10:00:00",
  ...
}
```

**Error Responses**
- `409 Conflict` — slug or origin fields already exist

---

### `DELETE /api/v1/comics/{id}`
Delete a comic by ID. **Cascades** to all linked chapters (orphan removal is active).

**Path Parameters**

| Param | Type | Description |
|---|---|---|
| `id` | Long | Comic ID |

**Response `204 No Content`** — No body.

**Error Responses**
- `404 Not Found` — Comic does not exist

---

### `POST /api/v1/chapters`
Create a new chapter attached to an existing comic.

**Request**
```
Content-Type: application/json
```
```json
{
  "comic_id": 1,
  "chapter_number": "Chapter 1",
  "type": "normal",
  "position": 1,
  "origin_url": "https://nettruyenar.com/truyen-tranh/one-piece-12345/chap-1",
  "origin_path_params": "one-piece-12345/chap-1"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `comic_id` | Long | **Yes** | Must reference an existing comic |
| `chapter_number` | string | No | Display name, e.g. `"Chapter 1"` |
| `type` | string | No | |
| `position` | integer | No | Sort order |
| `origin_url` | string | No | Must be unique if provided |
| `origin_path_params` | string | No | Must be unique if provided |

**Response `201 Created`**
Returns a [`ChapterDetail`](#chapterdetail-response-object) object.

```json
{
  "id": 10,
  "chapterNumber": "Chapter 1",
  "type": "normal",
  "position": 1,
  "crawlingStatus": "INIT",
  "sourceUrl": null,
  "originPathParams": "one-piece-12345/chap-1",
  "createdAt": "2026-03-13T10:00:00",
  "updatedAt": "2026-03-13T10:00:00",
  "comic": { "id": 1, ... },
  "images": []
}
```

**Error Responses**
- `404 Not Found` — `comic_id` does not exist

---

### `DELETE /api/v1/chapters/{id}`
Delete a chapter by ID.

**Path Parameters**

| Param | Type | Description |
|---|---|---|
| `id` | Long | Chapter ID |

**Response `204 No Content`** — No body.

**Error Responses**
- `404 Not Found` — Chapter does not exist

---

### `POST /api/v1/crawl/nettruyen/comic`
Trigger a full comic crawl from NetTruyen (scrapes metadata + thumbnail).

**Request**
```
Content-Type: application/json
```
```json
{
  "slug-and-id": "one-piece-12345"
}
```

**Response `200 OK`**
```
Success
```

**Error Responses**
- `409 Conflict` — Comic with this slug/id already exists

---

### `POST /api/v1/crawl/nettruyen/chapter`
Trigger image crawl for a single chapter.

**Request**
```
Content-Type: application/json
```
```json
{
  "chapter_id": 10
}
```

**Response `200 OK`**
```
Success
```

---

### `GET /api/v1/crawl/nettruyen/sse/comic`
Stream chapter crawl progress for a comic via Server-Sent Events.

**Query Parameters**

| Param | Type | Description |
|---|---|---|
| `slug-n-id` | string | Comic slug+id, e.g. `"one-piece-12345"` |

**Response** — `text/event-stream`

Each SSE event is a JSON object:
```json
{
  "type": "ADD",
  "message": "Chapter 1 crawled successfully",
  "data": {
    "chapterId": 10,
    "chapterName": "Chapter 1"
  }
}
```

| Event `type` | Meaning |
|---|---|
| `ADD` | Chapter was crawled and added |
| `IGNORE` | Chapter was skipped (already finished) |

---

## GraphQL API

**Endpoint:** `POST /graphql`
**UI (dev):** `http://localhost:8080/q/graphql-ui`

---

### Queries

#### `ping`
Health check.

```graphql
query {
  ping
}
```

**Response:** `"Pong"`

---

#### `comic`
Get a single comic by ID.

```graphql
query {
  comic(id: 1) {
    id
    title
    slug
    description
    author
    status
    chapterCount
    crawlingStatus
    createdAt
    updatedAt
    thumbImage {
      id
      filePath
      fileName
    }
    tags {
      id
      name
    }
  }
}
```

**Arguments**

| Argument | Type | Required |
|---|---|---|
| `id` | `BigInteger` | Yes |

---

#### `comics`
Get a paginated list of comics.

```graphql
query {
  comics(pageRequest: {
    page: 0
    size: 20
    sortBy: "createdAt"
    sortOrder: "desc"
  }) {
    id
    title
    slug
    status
    chapterCount
    crawlingStatus
    thumbImage {
      filePath
    }
    tags {
      name
    }
  }
}
```

**Arguments**

| Argument | Type | Required |
|---|---|---|
| `pageRequest` | `PageRequest` | No |

---

#### `detailChapterById`
Get full chapter detail including images by chapter ID.

```graphql
query {
  detailChapterById(id: 10) {
    id
    chapterNumber
    sourceUrl
    position
    crawlingStatus
    images {
      id
      filePath
      fileName
      position
    }
  }
}
```

**Arguments**

| Argument | Type | Required |
|---|---|---|
| `id` | `BigInteger` | Yes |

---

#### `detailChapterBySlug`
Get full chapter detail by slug (origin path params).

```graphql
query {
  detailChapterBySlug(slug: "one-piece-12345/chap-1") {
    id
    chapterNumber
    position
    images {
      filePath
    }
  }
}
```

**Arguments**

| Argument | Type | Required |
|---|---|---|
| `slug` | `String` | Yes |

---

#### `chapters`
Get a paginated list of chapters for a comic.

```graphql
query {
  chapters(comicId: 1, pageRequest: {
    page: 0
    size: 50
    sortBy: "position"
    sortOrder: "asc"
  }) {
    id
    chapterNumber
    position
    crawlingStatus
    sourceUrl
  }
}
```

**Arguments**

| Argument | Type | Required |
|---|---|---|
| `comicId` | `BigInteger` | Yes |
| `pageRequest` | `PageRequest` | No |

---

#### `tag`
Get a single tag by ID.

```graphql
query {
  tag(id: 3) {
    id
    name
    normalizedName
    comicCount
  }
}
```

---

#### `tags`
Get all tags ordered by comic count descending.

```graphql
query {
  tags {
    id
    name
    comicCount
  }
}
```

---

### Mutations

#### `updateComic`
Update metadata of an existing comic. Only non-null fields are applied (partial update).

```graphql
mutation {
  updateComic(
    id: 1
    input: {
      title: "One Piece (Updated)"
      description: "New description"
      author: "Eiichiro Oda"
      status: "completed"
    }
  ) {
    id
    title
    description
    author
    status
    updatedAt
  }
}
```

**Arguments**

| Argument | Type | Required |
|---|---|---|
| `id` | `BigInteger` | Yes |
| `input` | `UpdateComicRequest` | Yes |

**`UpdateComicRequest` input fields** (all optional — only provided fields are updated)

| Field | Type | Notes |
|---|---|---|
| `title` | `String` | |
| `description` | `String` | |
| `author` | `String` | |
| `status` | `String` | e.g. `"ongoing"`, `"completed"` |

**Error Responses**
- `404 Not Found` — Comic does not exist

---

#### `updateChapter`
Update metadata of an existing chapter. Only non-null fields are applied (partial update).

```graphql
mutation {
  updateChapter(
    id: 10
    input: {
      chapterNumber: "Chapter 1 (Revised)"
      type: "special"
      position: 1
    }
  ) {
    id
    chapterNumber
    type
    position
    updatedAt
  }
}
```

**Arguments**

| Argument | Type | Required |
|---|---|---|
| `id` | `BigInteger` | Yes |
| `input` | `UpdateChapterRequest` | Yes |

**`UpdateChapterRequest` input fields** (all optional — only provided fields are updated)

| Field | Type | Notes |
|---|---|---|
| `chapter_number` | `String` | |
| `type` | `String` | |
| `position` | `Int` | |

**Error Responses**
- `404 Not Found` — Chapter does not exist
