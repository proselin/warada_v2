# Podman local development

Warada's Quarkus development profile uses Podman for container operations and
native builds. The Compose files remain compatible with both Docker Compose
and `podman-compose`.

## Start infrastructure

```bash
systemctl --user enable --now podman.socket
podman-compose -f docker-compose.yml up -d
```

For the devcontainer infrastructure:

```bash
export CONTAINER_SOCKET="${XDG_RUNTIME_DIR}/podman/podman.sock"
podman-compose -f .devcontainer/docker-compose.yml up -d \
  postgres rabbitmq garage
```

When Quarkus Dev Services or Testcontainers need the Podman API, expose the
rootless socket through Docker-compatible variables:

```bash
export DOCKER_HOST="unix://${XDG_RUNTIME_DIR}/podman/podman.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

Run the backend with:

```bash
./backend/mvnw -f backend/pom.xml quarkus:dev
```

Build a native image with:

```bash
./backend/mvnw -f backend/pom.xml \
  -Pnative -Dquarkus.native.container-build=true package
```

The dev profile sets `quarkus.docker.executable-name=podman` and
`quarkus.native.container-runtime=podman`. It also disables Quarkus Dev
Services and the LGTM observability container because those services are
provided by Compose; this avoids Testcontainers requiring a Docker-compatible
socket just to start dev mode.
