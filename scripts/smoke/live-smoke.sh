#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-}"
USERNAME="${USERNAME:-admin1}"
PASSWORD="${PASSWORD:-}"

if [[ -z "$BASE_URL" || -z "$PASSWORD" ]]; then
  echo "Usage: BASE_URL=http://host USERNAME=admin1 PASSWORD=*** scripts/smoke/live-smoke.sh"
  exit 2
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required but not installed"
  exit 2
fi

echo "[1/6] Login as ${USERNAME}"
LOGIN_JSON=$(curl -sS -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

TOKEN=$(echo "$LOGIN_JSON" | jq -r '.token // empty')
if [[ -z "$TOKEN" ]]; then
  echo "Login failed: $LOGIN_JSON"
  exit 1
fi

echo "[2/6] Frontend root"
FRONT_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" "${BASE_URL}/")
[[ "$FRONT_STATUS" == "200" ]] || { echo "Frontend status=$FRONT_STATUS"; exit 1; }

echo "[3/6] Products listing"
PRODUCTS_JSON=$(curl -sS "${BASE_URL}/api/v1/products?page=0&size=5" -H "Authorization: Bearer ${TOKEN}")
echo "$PRODUCTS_JSON" | jq -e '.content' >/dev/null

echo "[4/6] Low stock with ADMIN"
curl -sS "${BASE_URL}/api/v1/products/low-stock" -H "Authorization: Bearer ${TOKEN}" | jq -e '.' >/dev/null

echo "[5/6] Optional order contract check (orderItemId)"
ORDERS_JSON=$(curl -sS "${BASE_URL}/api/v1/orders?page=0&size=20" -H "Authorization: Bearer ${TOKEN}")
ORDER_ID=$(echo "$ORDERS_JSON" | jq -r '.content[0].id // empty')
if [[ -n "$ORDER_ID" ]]; then
  ORDER_DETAIL=$(curl -sS "${BASE_URL}/api/v1/orders/${ORDER_ID}" -H "Authorization: Bearer ${TOKEN}")
  echo "$ORDER_DETAIL" | jq -e '.items[0].orderItemId' >/dev/null
fi

echo "[6/6] STAFF should be forbidden on low-stock"
SUFFIX=$(date +%s)
STAFF_USER="staff_smoke_${SUFFIX}"
curl -sS -o /dev/null -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${STAFF_USER}\",\"password\":\"${PASSWORD}\",\"role\":\"STAFF\"}" || true

STAFF_LOGIN=$(curl -sS -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${STAFF_USER}\",\"password\":\"${PASSWORD}\"}")
STAFF_TOKEN=$(echo "$STAFF_LOGIN" | jq -r '.token // empty')
if [[ -z "$STAFF_TOKEN" ]]; then
  echo "Unable to login with staff account"
  exit 1
fi

STAFF_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/products/low-stock" -H "Authorization: Bearer ${STAFF_TOKEN}")
[[ "$STAFF_STATUS" == "403" ]] || { echo "Expected 403, got ${STAFF_STATUS}"; exit 1; }

echo "Smoke check PASSED"