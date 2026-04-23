#!/bin/sh
set -e

# Usage check
if [ -z "$1" ]; then
  echo "❌ Version is required"
  echo "Usage: ./build-all-native.sh <version>"
  exit 1
fi

VERSION="$1"

echo "🚀 Building all services — version: ${VERSION}"
echo "================================================"

echo ""
echo ">>> core-service"
sh scripts/core-service/build-and-push.sh "${VERSION}"

echo ""
echo ">>> crawl-service"
sh scripts/crawl-service/build-and-push.sh "${VERSION}"

echo ""
echo ">>> display-service"
sh scripts/display-service/build-and-push.sh "${VERSION}"

echo ""
echo "================================================"
echo "✅ All services built and pushed for version: ${VERSION}"
