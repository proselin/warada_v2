#!/bin/sh
set -e

echo "🔨 Building core-service native binary..."
./gradlew :core-service:build -Dquarkus.profile=prod -x test
echo "✅ core-service native build complete"
