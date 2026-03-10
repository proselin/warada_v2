# Feature: Unit Test Implementation (06-02-26)

## Overview
This document outlines the stages and strategies used to implement unit tests and achieve over 50% code coverage for the `warada_v2` project.

## Stages of Implementation

### Stage 1: Assessment and Setup
- Analyzed the existing project structure and identified core components: Services, Repositories, Entities, Models, and Resources.
- Discovered that the initial test coverage was very low, with only a commented-out ExampleResourceTest.
- Modified `build.gradle` to include required testing dependencies: 
  - `io.quarkus:quarkus-junit5-mockito` for comprehensive mocking capabilities during JVM unit tests.
  - `io.quarkus:quarkus-jacoco` for generating code coverage reports.
  - Configured Gradle `jacocoTestReport` task to automatically run after `test`.

### Stage 2: Addressing Simple Coverage Patterns (Models and DTOs)
- **Strategy:** Focus on pure models, Exception mappers, and Data Transfer Objects (DTOs) first to quickly gain code coverage on lines containing Getters, Setters, `equals`, `hashCode`, and constructors.
- **Implementation:**
  - Wrote unit tests for `PageRequest`, testing its default parameters, offsetting, and sorting object generations (Panache `Sort`).
  - Wrote unit tests for `PageResponse` and `ApiError`.
  - Wrote unit tests for DTO Records like `ExtractedComicDetail`, `ExtractedChapterDetail`, `ApiResponse`, and `Crawl` request classes. Since Java Records auto-generate accessors and `toString`, instantiating these and verifying fields provided a massive coverage boost for minimal test logic.

### Stage 3: Core Service Layer & Mocking
- **Strategy:** Services contain the bulk of the business logic. We needed to isolate them from the database (Panache ORM) and external services (Nettruyen API, MinIO).
- **Implementation:**
  - Created `ComicServiceImplTest`, using `@InjectMocks` and `@Mock` to simulate `ComicRepository`.
  - Created `ChapterServiceImplTest`, using `@InjectMocks` and `@Mock` to simulate `ChapterRepository`. Replaced overly broad Matchers like `any(Sort.class)` with `nullable(Sort.class)` to test Panache queries accurately.
  - Created `TagServiceImplTest` to test complex comic count tracking and tag resolution parsing. Mocked `TagRepository`.
  - Created `CrawlServiceImplTest`, a complex workflow service. Mocked the `NettruyenExtractor`, `MinioClient`, and repositories to thoroughly cover conflicting scrape attempts (ConflictException) vs. successful inserts and chapter updates.
  - Created `NettruyenExtractorImplTest` simulating specific HTML responses and verifying regex and JS-variable parsing logic.

### Stage 4: API Presentation Layer
- **Strategy:** To drive coverage further past 50%, the Resource endpoints required testing. Since `@QuarkusTest` boots up an expensive context that slows down the test suite runtime, we opted for pure Mockito unit tests which execute in milliseconds.
- **Implementation:**
  - `ComicGqlResourceTest`
  - `ChapterGqlResourceTest`
  - `TagGqlResourceTest`
  - `HealthCheckResourceTest`
  - `CrawlRestResourceTest` pulling in the `CrawlService` to ensure REST requests accurately redirect to Service methods.

### Stage 5: Verification
- Ran `./gradlew clean test jacocoTestReport`.
- Monitored `build/reports/jacoco/test/jacocoTestReport.xml`.
- Identified missed lines, iteratively tested missing branches (especially in Panache sort mapping enums and `null` values).
- Passed the project >50% comprehensive Line Coverage milestone.

## Key Learnings & Future Work
- **Panache Testing caveats:** Panache `Sort` fields and Panache Query parameters require strict mocking rules, such as `nullable` matches over `any()` in testing.
- **Testing DTO Records:** Records naturally harbor many bytecode lines for standard methods, making them an excellent quick-win for code coverage metrics.
- **Future Integration Tests:** While this stage focused purely on unit testing and heavy mocking, subsequent testing tiers should utilize embedded PostgreSQL and LocalStack (or Testcontainers MinIO) to verify the data layer natively via `@QuarkusTest`.
