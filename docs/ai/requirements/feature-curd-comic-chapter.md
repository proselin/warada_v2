---
phase: requirements
title: CRUD for Comic & Chapter — Requirements
description: Requirements and acceptance criteria for the Comic/Chapter CRUD feature
---

# Requirements: CRUD for Comic & Chapter

## Problem Statement
The system currently only supports read (GET) operations for `Comic` and `Chapter` via GraphQL, and write operations are restricted to the crawl pipeline. There is no direct API for administrators or integrations to create, update, or delete records without triggering a full crawl.

## Goals
- Provide a REST API for **creating** and **deleting** comics and chapters directly.
- Provide a GraphQL API for **updating** comics and chapters.
- Reuse existing read (GET) GraphQL queries already in place.

## User Stories
- **As an admin**, I want to `POST /api/v1/comics` with a JSON body so that I can create a comic record without crawling.
- **As an admin**, I want to `DELETE /api/v1/comics/{id}` so that I can remove a comic and its records.
- **As an admin**, I want to call `mutation { updateComic(id, input) }` via GraphQL so I can update comic metadata.
- **As an admin**, I want to `POST /api/v1/chapters` so I can create a chapter attached to an existing comic.
- **As an admin**, I want to `DELETE /api/v1/chapters/{id}` so I can remove a chapter.
- **As an admin**, I want to call `mutation { updateChapter(id, input) }` via GraphQL so I can update chapter metadata.

## Acceptance Criteria
- `POST /api/v1/comics` returns HTTP 201 with created `Comic` JSON; returns 400 on missing required fields.
- `DELETE /api/v1/comics/{id}` returns HTTP 204 on success; returns 404 if comic not found.
- `POST /api/v1/chapters` returns HTTP 201 with `ChapterDetailGql` JSON; returns 404 if `comicId` does not exist.
- `DELETE /api/v1/chapters/{id}` returns HTTP 204 on success; returns 404 if chapter not found.
- GraphQL `updateComic` mutation returns the updated `Comic` object; 404 if not found.
- GraphQL `updateChapter` mutation returns the updated `ChapterDetailGql` object; 404 if not found.
- All 404 responses use the shared `ApiError` JSON schema `{ processId, status, message, timestamp }`.

## Non-Goals
- No authentication or authorisation enforcement (out of scope — Milestone 7).
- No bulk create/update/delete operations.
- No cascading delete of MinIO images on comic/chapter deletion (images remain in storage).

## Constraints
- REST endpoints must use `@Consumes(APPLICATION_JSON)` / `@Produces(APPLICATION_JSON)`.
- GraphQL mutations follow the existing `@GraphQLApi` + `@Mutation` + `@Blocking` convention.
- All write operations must be `@Transactional` at the service layer.
