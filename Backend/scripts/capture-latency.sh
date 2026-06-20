#!/usr/bin/env bash
# capture-latency.sh — measure iOS-path p50/p95 latency from the Mac.
#
# Stage 0.3 Job 4 helper. Doesn't need an iPhone for the gemini path because
# we hit it with the dev x-backend-key directly (same code path iOS hits in
# Debug builds on the simulator/dev). For /attest/verify a real iOS call is
# needed, so the script just times /attest/challenge as the cheaper proxy
# (it shares all middleware up to the verify-specific work).
#
# Usage:
#   ./Backend/scripts/capture-latency.sh [N] [output-md]
#
# Defaults: N=100, output-md=Backend/test/contract/ios/baseline-latency.md
set -euo pipefail

N="${1:-100}"
OUT="${2:-Backend/test/contract/ios/baseline-latency.md}"
BACKEND_URL="${BACKEND_URL:?Set BACKEND_URL to your dev Cloud Run URL, e.g. https://<service>-<project-number>.us-central1.run.app}"
GCP_PROJECT="${GCP_PROJECT:-$(gcloud config get-value project 2>/dev/null)}"
SECRET_NAME="${SECRET_NAME:-watchmycalories-dev-app-backend-api-key}"
SERVICE_NAME="${SERVICE_NAME:-watchmycalories-backend-dev}"
REGION="${REGION:-us-central1}"

if [[ -z "${APP_KEY:-}" ]]; then
    APP_KEY="$(gcloud secrets versions access latest --secret="$SECRET_NAME" --project="$GCP_PROJECT")"
fi

REVISION="$(gcloud run services describe "$SERVICE_NAME" \
    --region="$REGION" --project="$GCP_PROJECT" \
    --format='value(status.traffic[0].revisionName)')"

echo "→ Backend: $BACKEND_URL"
echo "→ Revision: $REVISION"
echo "→ Iterations: $N per endpoint"

measure() {
    local method="$1" path="$2" data_args="$3" label="$4"
    local times_file
    times_file="$(mktemp)"
    echo "→ Measuring $label ($N requests)..."

    for i in $(seq 1 "$N"); do
        # %{time_total} is seconds with microsecond precision; we want ms.
        SECS="$(curl -s -o /dev/null -w "%{time_total}" \
            -X "$method" \
            -H "Content-Type: application/json" \
            -H "x-backend-key: $APP_KEY" \
            -H "X-App-Platform: ios" \
            $data_args \
            "$BACKEND_URL$path")"
        # Multiply by 1000 with awk (portable; bash arithmetic can't do floats)
        echo "$SECS" | awk '{printf "%d\n", $1*1000}' >> "$times_file"
    done

    # Compute p50/p95 with awk on sorted values
    sort -n "$times_file" | awk -v label="$label" '
        BEGIN { c = 0 }
        { a[c++] = $1 }
        END {
            p50 = a[int(c*0.5)]
            p95 = a[int(c*0.95)]
            mean = 0; for (i=0;i<c;i++) mean += a[i]; mean = mean/c
            printf "  %s — p50: %dms, p95: %dms, mean: %dms (n=%d)\n", label, p50, p95, mean, c
            printf "%s|%d|%d|%d\n", label, p50, p95, mean > "/tmp/_lat_row"
        }
    '
    cat /tmp/_lat_row >> /tmp/_lat_table
    rm -f "$times_file" /tmp/_lat_row
}

rm -f /tmp/_lat_table
touch /tmp/_lat_table

# /attest/challenge — proxy for /attest/verify (same middleware stack up to handler)
measure GET "/attest/challenge" "" "/attest/challenge"

# /v1beta/models/default:generateContent — text-only body to avoid Gemini cost while still exercising the route
DATA_ARGS='-d {"contents":[{"parts":[{"text":"ping"}]}]}'
measure POST "/v1beta/models/default:generateContent" "$DATA_ARGS" "/v1beta/models/default:generateContent"

# Write the markdown output
mkdir -p "$(dirname "$OUT")"
{
    echo "# iOS-path baseline latency (T1.10.h)"
    echo
    echo "Captured: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "Backend: \`$SERVICE_NAME\`"
    echo "Pinned revision: \`$REVISION\`"
    echo "Iterations: $N per endpoint"
    echo "Source: \`scripts/capture-latency.sh\` (Mac, x-backend-key path, text-only body)"
    echo
    echo "| Endpoint | p50 (ms) | p95 (ms) | mean (ms) |"
    echo "|---|---|---|---|"
    while IFS='|' read -r label p50 p95 mean; do
        printf "| %s | %s | %s | %s |\n" "$label" "$p50" "$p95" "$mean"
    done < /tmp/_lat_table
    echo
    echo "## Notes"
    echo "- \`/attest/challenge\` is used as the cheaper proxy for \`/attest/verify\` (a real verify needs an iPhone-emitted App Attest blob)."
    echo "- Gemini path uses a text-only body to avoid burning model tokens; same middleware/auth/Firestore path as a real image request."
} > "$OUT"

echo "→ Saved: $OUT"
rm -f /tmp/_lat_table
