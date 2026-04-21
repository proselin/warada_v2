#!/bin/sh
set -e

# Usage check
if [ -z "$1" ]; then
  echo "❌ Version is required"
  echo "Usage: ./build-and-push.sh <version>"
  exit 1
fi

VERSION="$1"
REGISTRY="ghcr.io/proselin/warada_v2"
IMAGE="${REGISTRY}/core-service:${VERSION}-native-micro"

echo "🔨 Building core-service native binary..."
./gradlew :core-service:build -Dquarkus.profile=prod -x test

echo "🐳 Building Docker image: ${IMAGE}"
docker build \
  -f src/apps/core-service/src/main/docker/Dockerfile.native-micro \
  -t "${IMAGE}" \
  src/apps/core-service/

echo "📤 Pushing image: ${IMAGE}"
docker push "${IMAGE}"

echo "✅ Done: ${IMAGE}"
