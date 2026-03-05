#!/bin/sh

set -e

# -----------------------------
# Config
# -----------------------------
IMAGE_NAME="ghcr.io/proselin/warada_v2"
DOCKERFILE="src/main/docker/Dockerfile.native-micro"

# -----------------------------
# Args
# -----------------------------
if [ -z "$1" ]; then
  echo "❌ Version is required"
  echo "Usage: ./build-and-push-native-micro.sh <version>"
  exit 1
fi

VERSION="$1"
TAG="${VERSION}-native-micro"
IMAGE="${IMAGE_NAME}:${TAG}"

# -----------------------------
# Auth check
# -----------------------------
if ! docker info >/dev/null 2>&1; then
  echo "❌ Docker is not running"
  exit 1
fi

echo "🔐 Make sure you are logged in to GHCR"
echo "   docker login ghcr.io -u <github-username>"

# -----------------------------
# Build
# -----------------------------
echo "🏗️  Building image: $IMAGE"

docker build \
  -f "$DOCKERFILE" \
  -t "$IMAGE" \
  .

# -----------------------------
# Push
# -----------------------------
echo "📦 Pushing image to GHCR: $IMAGE"

docker push "$IMAGE"

echo "✅ Done!"
echo "➡️  Image available at: $IMAGE"
