#!/bin/sh
set -e

# Usage check
if [ -z "$1" ]; then
  echo "❌ Version is required"
  echo "Usage: ./build-native-micro.sh <version>"
  exit 1
fi

VERSION="$1"
REGISTRY="ghcr.io/proselin/warada_v2"
IMAGE="${REGISTRY}/display-service:${VERSION}-native-micro"

echo "🔨 Building display-service native binary..."
./gradlew :display-service:build -Dquarkus.profile=prod -x test

echo "🐳 Building Docker image: ${IMAGE}"
docker build \
  -f src/apps/display-service/src/main/docker/Dockerfile.native-micro \
  -t "${IMAGE}" \
  src/apps/display-service/

echo "✅ Done: ${IMAGE}"
