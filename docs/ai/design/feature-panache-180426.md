---
phase: design
title: Panache Cleanup — Design
description: Recommended persistence design for reducing repository boilerplate in warada_v2
---

# Design: panache-180426

## Decision
Use a lighter Quarkus persistence structure:
- keep repository wrappers as the main data-access boundary
- reduce interface-plus-impl duplication where it adds no value
- use Panache support only to simplify persistence code, not to move business logic into entities

## Recommended Pattern
- Repositories should own custom data-access queries.
- Services should continue to own transactions and orchestration.
- Entities should remain focused on mapping and domain state.

## Why this fits the project
The current codebase is already service-oriented. The biggest pain point is the amount of repetitive repository boilerplate, not a lack of persistence features.

## Design Constraints
- keep backward behavior stable
- avoid broad architecture churn
- keep the code understandable for future work
