# Feature Requirements: devservice-200426

## Objective

Replace the default local development experience based on Quarkus Dev Services with a reusable devcontainer environment backed by Docker Compose.

## Requirements

- support VS Code development with Dev Containers
- support IntelliJ IDEA developers using the same devcontainer definition
- keep PostgreSQL, RabbitMQ, and MinIO available in a predictable local stack
- retain the previous Dev Services examples as commented config for learning purposes
- update project documentation for onboarding and daily use

## Success Criteria

- developers can open the project in a containerized workspace
- the backend can connect to local infrastructure using explicit DEV_* environment variables
- README explains both IDE workflows and the manual fallback option
