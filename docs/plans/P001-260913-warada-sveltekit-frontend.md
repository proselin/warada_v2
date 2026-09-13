# P001 — Warada SvelteKit Frontend

**Status:** active
**Created:** 2026-09-13

## 1. Reason

Warada needs an efficient, safe frontend for reading comics and operating crawls. The first release prioritizes reactive UI, a small client runtime, accessibility, maintainability, and existing Quarkus APIs rather than SEO.

## 2. Precondition

- Existing GraphQL, REST, and SSE contracts are understood and suitable for the first reader/admin workflows.
- SvelteKit + TypeScript is accepted as the frontend stack.
- UI/UX Pro Max guidance is available under `.github/prompts/ui-ux-pro-max`.
- The design system and first-release scope are approved before implementation.

## 3. Post-condition

- A separate SvelteKit frontend can browse comics, show comic details and chapters, read chapter images, and display crawl progress.
- The frontend has documented design-system, responsive, accessibility, typed API, and validation rules.
- Backend business logic remains in Quarkus; no unnecessary BFF is introduced.

## 4. Definition of Done

- [ ] Generate and review a Warada design system with UI/UX Pro Max.
- [ ] Define reader and admin information architecture and responsive behavior.
- [ ] Define GraphQL, REST, and SSE integration boundaries.
- [ ] Define loading, empty, error, accessibility, keyboard, and reduced-motion behavior.
- [ ] Define testing, security-header, and deployment validation criteria.
- [ ] Create linked task documents before implementation starts.

## 5. Logic

Use SvelteKit + TypeScript as a client-rendered application, with prerendering only where useful and safe. Use UI/UX Pro Max design-system search for a content-first comic reader, supplemented by Svelte stack and UX searches.

Build in this order:

1. Design system and frontend foundation.
2. Shared shell and accessibility primitives.
3. Reader discovery, comic details, chapter list, and image reader.
4. Admin crawl controls and SSE progress.
5. API typing, failure states, tests, security headers, and responsive validation.

## 6. Impact

- Backend source and build files live under `backend/`; frontend workspace lives under `frontend/`.
- New design-system documentation and tokens.
- Integration with existing backend GraphQL, REST crawl, and SSE progress endpoints.
- No initial changes to crawler source abstraction, backend business rules, or database schema.

## 7. Linked references

- [Working principle](../WORKING_PRINCIPLE.md)
- [Project README](../../README.md)
- [Requirements](../ai/requirements/README.md)
- [Design notes](../ai/design/README.md)
- [UI/UX Pro Max prompt](../../.github/prompts/ui-ux-pro-max.prompt.md)

## Decisions and boundaries

- No SEO-specific work in the first milestone.
- No frontend-owned authentication or business logic initially.
- No second comic source until the NetTruyen integration is stable behind a backend abstraction.
- No BFF unless the existing API boundaries prove insufficient.
- Light/dark mode remains a design validation decision, not an implementation assumption.

## Current progress

- The backend now lives under `backend/`.
- The SvelteKit frontend foundation and initial reader/admin routes live under `frontend/`.
- Frontend dependency installation and full checks remain blocked until npm registry access is available.
