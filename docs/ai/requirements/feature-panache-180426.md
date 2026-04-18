---
phase: requirements
title: Panache Cleanup — Requirements
description: Reduce persistence boilerplate while keeping the project structure clean and consistent
---

# Requirements: panache-180426

## Goal
Simplify the persistence layer in the Quarkus backend by reducing repository boilerplate and aligning the code with a clearer Panache-based style.

## Functional Requirements
- Keep the repository wrapper approach for data access.
- Remove unnecessary duplication in repository interface and implementation pairs.
- Preserve current service-layer business logic and transaction boundaries.
- Preserve current REST, GraphQL, crawl, and scheduler behavior.
- Allow entities to adopt Panache support only where it removes real boilerplate and does not blur responsibilities.

## Non-Functional Requirements
- No regression in current build or tests.
- Keep entity field encapsulation and explicit mappings intact.
- Maintain readability for future contributors.
- Keep the change scoped to persistence/style cleanup.

## Out of Scope
- New API features
- Database schema redesign
- Message queue or SSE behavior changes
- Unrelated refactors outside persistence and style cleanup

## Success Criteria
- The persistence code uses one clear and consistent pattern.
- Repositories no longer duplicate trivial Panache CRUD wrappers.
- The application still builds and tests successfully.
