# Feature Implementation: devservice-200426

## Implemented Changes

### 1. Shared devcontainer setup
Added a complete development container workspace under the .devcontainer directory:
- devcontainer.json
- Dockerfile
- docker-compose.yml

### 2. Cross-IDE support
The same setup is usable from:
- VS Code Dev Containers
- IntelliJ IDEA Dev Containers support

### 3. Dev profile cleanup
The active dev profile no longer depends on Quarkus Dev Services.
It now uses explicit environment-driven service configuration for:
- PostgreSQL
- RabbitMQ
- MinIO

### 4. Legacy reference preserved
The old Dev Services settings are still present as commented examples in the application properties file.
This helps newer developers understand both development models.

## How to run

### VS Code or IntelliJ
1. Open the repository with the existing devcontainer config.
2. Wait for the services to start.
3. Run:

```bash
./gradlew quarkusDev
```

### Host machine fallback

```bash
docker compose -f .devcontainer/docker-compose.yml up -d postgres rabbitmq minio
./gradlew quarkusDev
```

If ports like 15672 or 9001 are already occupied, add a local .devcontainer/.env file and override the exposed host ports.

## Expected local endpoints
- App: http://localhost:8080
- GraphQL UI: http://localhost:8080/q/graphql-ui
- RabbitMQ UI: http://localhost:15672
- MinIO Console: http://localhost:9001
