# Feature Planning: devservice-200426

## Plan

1. Inspect the existing Quarkus dev profile and identify active Dev Services usage.
2. Add a standard .devcontainer workspace for Java 21 and Gradle development.
3. Define PostgreSQL, RabbitMQ, and MinIO in Docker Compose.
4. Switch the dev profile to explicit environment-driven local service configuration.
5. Preserve the old Dev Services properties as commented reference examples.
6. Update README and feature documentation for onboarding in both VS Code and IntelliJ IDEA.
7. Validate Docker Compose and Gradle startup paths.

## Validation

- Docker Compose configuration resolves successfully.
- Support services can start with optional host port overrides.
- Gradle configuration loads without errors.
