#!/usr/bin/env bash
#
# Build the release AAB with the upload keystore.
#
# Password resolution order:
#   1. RELEASE_STORE_PASSWORD env var (for CI).
#   2. macOS Keychain service "watchmycalories-upload-keystore"
#      (run scripts/keychain-setup.sh once to populate it).
#   3. Interactive `read -s` prompt (masked).
#
# The password is held only in this script's process memory — never on disk,
# never in shell history.
#
# Default invocation builds the release AAB ready to upload to Play Console:
#   scripts/build-release.sh
# Pass other gradle args to override:
#   scripts/build-release.sh :app:assembleRelease
#
set -euo pipefail

cd "$(dirname "$0")/.."

KEYCHAIN_SERVICE="watchmycalories-upload-keystore"

PASS="${RELEASE_STORE_PASSWORD:-}"

if [ -z "$PASS" ] && command -v security >/dev/null 2>&1; then
  PASS=$(security find-generic-password -s "$KEYCHAIN_SERVICE" -w 2>/dev/null || true)
fi

if [ -z "$PASS" ]; then
  echo "(no Keychain entry for '$KEYCHAIN_SERVICE' — run scripts/keychain-setup.sh once to skip this prompt next time)"
  read -s -p "Upload keystore password: " PASS
  echo
fi

if [ -z "$PASS" ]; then
  echo "error: empty password" >&2
  exit 1
fi

# Assumes same password for storepass and keypass (PKCS12 enforces this).
export RELEASE_STORE_PASSWORD="$PASS"
export RELEASE_KEY_PASSWORD="$PASS"
unset PASS

if [ "$#" -eq 0 ]; then
  set -- :app:bundleRelease
fi

./gradlew "$@"
