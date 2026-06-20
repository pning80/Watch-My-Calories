#!/usr/bin/env bash
# capture-429.sh — force a 429 from the dev Gemini route and save the response.
#
# Stage 0.3 Job 2 helper. Requires the dev x-backend-key in Secret Manager
# (auto-fetched via gcloud). No iPhone required.
#
# Usage:
#   ./Backend/scripts/capture-429.sh [output-path]
#
# Default output: Backend/test/contract/ios/rate-limit-429.response.json
set -euo pipefail

BACKEND_URL="${BACKEND_URL:?Set BACKEND_URL to your dev Cloud Run URL, e.g. https://<service>-<project-number>.us-central1.run.app}"
GCP_PROJECT="${GCP_PROJECT:-$(gcloud config get-value project 2>/dev/null)}"
SECRET_NAME="${SECRET_NAME:-watchmycalories-dev-app-backend-api-key}"
OUT="${1:-Backend/test/contract/ios/rate-limit-429.response.json}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-200}"

# Allow override of the key via env (skip gcloud lookup) for CI/local-dev.
if [[ -z "${APP_KEY:-}" ]]; then
    echo "→ Fetching dev x-backend-key from Secret Manager ($SECRET_NAME)..."
    APP_KEY="$(gcloud secrets versions access latest --secret="$SECRET_NAME" --project="$GCP_PROJECT")"
fi

echo "→ Hammering $BACKEND_URL/v1beta/models/default:generateContent up to $MAX_ATTEMPTS times..."
TMP_BODY="$(mktemp)"
TMP_HEADERS="$(mktemp)"
trap 'rm -f "$TMP_BODY" "$TMP_HEADERS"' EXIT

for i in $(seq 1 "$MAX_ATTEMPTS"); do
    STATUS="$(curl -s -o "$TMP_BODY" -D "$TMP_HEADERS" -w "%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -H "x-backend-key: $APP_KEY" \
        -H "X-App-Platform: ios" \
        -d '{"contents":[{"parts":[{"text":"hi"}]}]}' \
        "$BACKEND_URL/v1beta/models/default:generateContent")"

    if [[ "$STATUS" == "429" ]]; then
        RETRY_AFTER="$(grep -i '^retry-after:' "$TMP_HEADERS" | awk '{print $2}' | tr -d '\r' || true)"
        echo "✓ Got 429 on attempt $i (Retry-After: ${RETRY_AFTER:-<none>})"

        mkdir -p "$(dirname "$OUT")"
        # Wrap body + key headers so the test fixture is self-describing.
        jq -n \
            --arg retry "$RETRY_AFTER" \
            --argjson body "$(cat "$TMP_BODY")" \
            '{
                "_meta": {
                    "capturedAt": (now | strftime("%Y-%m-%dT%H:%M:%SZ")),
                    "endpoint": "/v1beta/models/default:generateContent",
                    "status": 429,
                    "retryAfter": $retry
                },
                "body": $body
            }' > "$OUT"

        echo "→ Saved: $OUT"
        exit 0
    fi

    # Light progress every 10 attempts
    if (( i % 10 == 0 )); then
        echo "  attempt $i — status $STATUS, no 429 yet"
    fi
done

echo "✗ No 429 after $MAX_ATTEMPTS attempts — bump MAX_ATTEMPTS or check rate-limit config" >&2
exit 1
