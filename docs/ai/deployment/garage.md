# Garage object storage

Warada uses Garage as its S3-compatible object storage backend. The backend
continues to use the existing S3 client integration, so object paths and the
`warada-images` bucket remain unchanged.

## Local development

Start Garage with Compose or Podman:

```bash
podman-compose up -d garage
```

Garage listens on:

- S3 API: `http://localhost:3900`
- RPC: `http://localhost:3901`
- Web API: `http://localhost:3902`
- Admin API: `http://localhost:3903`

The image is pulled from Docker Hub via `GARAGE_IMAGE` (override if you mirror
it to a private registry):

```bash
export GARAGE_IMAGE=docker.io/dxflrs/garage:v2.3.0
```

Since Garage `v2.3.0`, the container automatically bootstraps a single-node
cluster and creates a default access key/bucket at startup, driven by the
`--single-node --default-bucket` flags and the `GARAGE_DEFAULT_ACCESS_KEY`,
`GARAGE_DEFAULT_SECRET_KEY`, and `GARAGE_DEFAULT_BUCKET` environment
variables already set in both `docker-compose.yml` and
`.devcontainer/docker-compose.yml` (`warada-dev` /
`a1251ed99e3e8e221af619fdbafb77cd` / `warada-images`). No manual
`garage layout`/`garage key`/`garage bucket` commands are required for
local development.

To confirm the node came up correctly:

```bash
podman-compose exec garage /garage status
podman-compose exec garage /garage bucket list
```

For production, generate your own access key and secret (do not reuse the
dev defaults), and provide `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`,
`S3_SECURE`, and `S3_REGION` to the backend. The `rpc_secret`, `admin_token`, and
`metrics_token` values in `garage.toml` are dev-only placeholders — regenerate
them with `openssl rand -hex 32` / `openssl rand -base64 32` before deploying
anywhere beyond a local machine.

## S3 region must match `garage.toml`

`garage.toml` sets `[s3_api].s3_region = "garage"`. The `quarkus-minio`
client defaults to signing requests with region `us-east-1` if
`quarkus.minio.region` is not set, which Garage rejects with:

```
AuthorizationHeaderMalformed: Authorization header malformed, unexpected
scope: '.../us-east-1/s3/aws4_request', expected: '.../garage/s3/aws4_request'
```

Both `application-dev.yaml` and `application-prod.yaml` set
`quarkus.minio.region` to `${DEV_S3_REGION:garage}` / `${S3_REGION:garage}`
respectively, so this is handled by default. If you change `s3_region` in
`garage.toml`, update `DEV_S3_REGION` / `S3_REGION` to match.

