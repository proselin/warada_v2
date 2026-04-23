#!/bin/sh
set -e

echo "🔨 Building display-service native binary..."
./gradlew :display-service:build -Dquarkus.profile=prod -x test
echo "✅ display-service native build complete"
