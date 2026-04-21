# Feature Design: devservice-200426

## Goal

Move local development from automatic Quarkus Dev Services to a predictable devcontainer-based workflow that works the same in VS Code and IntelliJ IDEA.

## Design Summary

The project now uses a shared container workspace under the .devcontainer directory.
The workspace container connects to PostgreSQL, RabbitMQ, and MinIO defined in Docker Compose.

This keeps the local runtime explicit and easier to understand:
- developers can see every service directly
- ports stay stable across machines
- onboarding becomes simpler for both editors
- the old Dev Services properties remain documented as comments for learning purposes

## Main Decisions

### IDE compatibility
A standard devcontainer.json file is used so both VS Code and IntelliJ IDEA can open the same development environment.

### Local infrastructure
Docker Compose starts these services:
- PostgreSQL for metadata storage
- RabbitMQ for chapter crawl jobs
- MinIO for image storage
- a devcontainer workspace for Java and Gradle development

### Configuration strategy
The dev profile now reads DEV_* environment variables with localhost defaults.
That means the app can run:
- inside the devcontainer using compose service names
- on the host machine using localhost

### Backward learning path
The previous Dev Services config is kept in comments inside the application properties file so new developers can compare both approaches quickly.
