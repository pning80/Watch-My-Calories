#!/usr/bin/env bash
# redact-fixture.sh — promote raw captures to redacted fixtures the tests load.
#
# Reads from Backend/test/contract/ios/.intermediate/ (output of mitm-capture.py)
# and writes redacted versions to Backend/test/contract/ios/.
#
# Replaces these sensitive values with markers that match
# Backend/test/contract/ios/README.md:
#   - keyID (any base64 string in keyId / x-apple-attest-key-id field) → __KEY_ID__
#   - challenge UUID → __CHALLENGE__
#   - attestation blob (long base64) → __ATTESTATION_B64__
#   - inlineData.data (image bytes) → __IMAGE_B64__
#   - x-app-attest-assertion header value (binary) → __ASSERTION_B64__
#
# Usage: ./Backend/scripts/redact-fixture.sh
set -euo pipefail

SRC="Backend/test/contract/ios/.intermediate"
DST="Backend/test/contract/ios"

if [[ ! -d "$SRC" ]]; then
    echo "✗ $SRC not found — run mitm-capture.py first" >&2
    exit 1
fi

redact_json() {
    local in="$1" out="$2"
    if [[ ! -f "$in" ]]; then
        echo "  skip $(basename "$in") (not captured)"
        return
    fi
    # jq walks every field; redact known sensitive keys regardless of nesting.
    jq '
        walk(
            if type == "object" then
                with_entries(
                    if .key == "keyId" or .key == "key_id" or .key == "keyID" then .value = "__KEY_ID__"
                    elif .key == "challenge" then .value = "__CHALLENGE__"
                    elif .key == "attestation" then .value = "__ATTESTATION_B64__"
                    elif .key == "data" and (.value | type == "string") and (.value | length > 100) then .value = "__IMAGE_B64__"
                    elif .key == "assertion" then .value = "__ASSERTION_B64__"
                    elif .key == "thoughtSignature" then .value = "__THOUGHT_SIG__"
                    elif .key == "responseId" then .value = "__RESPONSE_ID__"
                    else .
                    end
                )
            else .
            end
        )
    ' "$in" > "$out"
    echo "  ✓ $(basename "$out")"
}

echo "→ Redacting JSON fixtures..."
redact_json "$SRC/attest-challenge.response.json"        "$DST/attest-challenge.response.json"
redact_json "$SRC/attest-verify.request.json"            "$DST/attest-verify.request.json"
redact_json "$SRC/attest-verify.response.json"           "$DST/attest-verify.response.json"
redact_json "$SRC/gemini-generate-content.request.json"  "$DST/gemini-generate-content.request.json"
redact_json "$SRC/gemini-generate-content.response.json" "$DST/gemini-generate-content.response.json"

echo
echo "→ Promoting raw .bin captures (no redaction — these are byte-exact replay artifacts)..."
echo "  NOTE: the replay test substitutes a test keyID back in; the captured bytes go through as-is."
for src in "$SRC"/*.request.bin; do
    [[ -f "$src" ]] || continue
    name="$(basename "$src" .request.bin)"
    case "$name" in
        attest-verify)            cp "$src" "$DST/captured-attest-verify-request.bin" ;;
        gemini-generate-content)  cp "$src" "$DST/captured-gemini-request.bin" ;;
        *)                        echo "  (skipping $name — no destination mapping)" ;;
    esac
    echo "  ✓ $(basename "$src") → $DST/captured-${name}-request.bin"
done

echo
echo "→ Sanity check — looking for un-redacted base64 blobs in JSON fixtures..."
LEAK=0
for f in "$DST"/{attest-challenge,attest-verify,gemini-generate-content}*.json; do
    [[ -f "$f" ]] || continue
    # Any base64-looking blob > 60 chars that isn't a placeholder is suspicious
    if grep -oE '"[A-Za-z0-9+/=_-]{60,}"' "$f" | grep -v '__' | grep -q .; then
        echo "  ⚠ possible leak in $(basename "$f"):"
        grep -oE '"[A-Za-z0-9+/=_-]{60,}"' "$f" | grep -v '__' | head -3
        LEAK=1
    fi
done
if [[ "$LEAK" == "0" ]]; then
    echo "  ✓ no unredacted base64 blobs found in JSON fixtures"
else
    echo
    echo "✗ Possible un-redacted secrets — review before committing." >&2
    exit 1
fi

echo
echo "→ Done. Inspect $DST/ then commit if happy:"
ls -la "$DST"/*.json "$DST"/*.bin 2>/dev/null
