#!/bin/sh
set -e

echo "🔨 Building crawl-service native binary..."
./gradlew :crawl-service:build -Dquarkus.profile=prod -x test
echo "✅ crawl-service native build complete"
