#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"

echo "Checking ${BASE_URL}/health"
curl -fsS "${BASE_URL}/health" | grep -q '"status":"ok"\|"status": "ok"'

echo "Checking ${BASE_URL}/version"
curl -fsS "${BASE_URL}/version"
echo

echo "Checking ${BASE_URL}/message"
curl -fsS "${BASE_URL}/message"
echo

echo "Smoke test passed"
