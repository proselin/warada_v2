#!/bin/sh

# Usage check
if [ -z "$1" ]; then
  echo "❌ Version is required"
  echo "Usage: ./build-native-micro.sh <version>"
  exit 1
fi

VERSION="$1"
IMAGE="ghcr.io/proselin/warada_v2:${VERSION}-native-micro"

echo "🚀 Building image: $IMAGE"

docker build \
  -f src/main/docker/Dockerfile.native-micro \
  -t "$IMAGE" \
  .

echo "✅ Build completed: $IMAGE"
