#!/usr/bin/env bash
# sync-dev-backend-key.sh
#
# Propagates the dev-only legacy `x-backend-key` from the SINGLE SOURCE OF TRUTH
# (Backend/.env.dev → APP_BACKEND_API_KEY) into the two gitignored client config
# files that the iOS and Android builds read:
#
#   - WatchMyCaloriesAndroid/local.properties   (→ BuildConfig.APP_BACKEND_API_KEY)
#   - Ads/AdMob-iOS.local.xcconfig              (→ Info.plist AppBackendApiKey)
#
# Run this once after setting or rotating APP_BACKEND_API_KEY in Backend/.env.dev.
# The server side derives independently via Backend/deploy.sh (→ Secret Manager →
# Cloud Run env), so .env.dev drives everything.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT/Backend/.env.dev"
ANDROID_PROPS="$ROOT/WatchMyCaloriesAndroid/local.properties"
IOS_XCCONFIG="$ROOT/Ads/AdMob-iOS.local.xcconfig"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: $ENV_FILE not found. Set APP_BACKEND_API_KEY there first." >&2
  exit 1
fi

# Extract APP_BACKEND_API_KEY (first match); strip whitespace and surrounding quotes.
KEY="$(grep -E '^[[:space:]]*APP_BACKEND_API_KEY[[:space:]]*=' "$ENV_FILE" \
        | head -n1 | sed -E 's/^[^=]*=[[:space:]]*//; s/[[:space:]]*$//; s/^"//; s/"$//')"

if [ -z "$KEY" ]; then
  echo "Error: APP_BACKEND_API_KEY is empty or missing in $ENV_FILE" >&2
  exit 1
fi

# Upsert "<name><sep><value>" into a file, creating the file if needed.
upsert() {
  local file="$1" name="$2" value="$3" sep="$4"
  touch "$file"
  if grep -qE "^[[:space:]]*${name}[[:space:]]*=" "$file"; then
    local tmp; tmp="$(mktemp)"
    awk -v n="$name" -v v="$value" -v s="$sep" \
      '$0 ~ "^[[:space:]]*"n"[[:space:]]*=" { print n s v; next } { print }' \
      "$file" > "$tmp"
    mv "$tmp" "$file"
  else
    printf '%s%s%s\n' "$name" "$sep" "$value" >> "$file"
  fi
}

upsert "$ANDROID_PROPS" "APP_BACKEND_API_KEY" "$KEY" "="
upsert "$IOS_XCCONFIG"  "APP_BACKEND_API_KEY" "$KEY" " = "

echo "✓ Synced APP_BACKEND_API_KEY from Backend/.env.dev →"
echo "    $ANDROID_PROPS"
echo "    $IOS_XCCONFIG"
