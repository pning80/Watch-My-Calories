#!/usr/bin/env bash
#
# One-time setup: store the upload-keystore password in macOS Keychain so
# scripts/build-release.sh can retrieve it without prompting.
#
# The first time the build script reads the password, macOS will pop a GUI
# dialog asking you to allow `security` access — click "Always Allow" to
# make subsequent reads silent. To remove the entry later:
#   security delete-generic-password -s "watchmycalories-upload-keystore"
#
set -euo pipefail

KEYCHAIN_SERVICE="watchmycalories-upload-keystore"

if security find-generic-password -s "$KEYCHAIN_SERVICE" -w >/dev/null 2>&1; then
  echo "Keychain already has an entry for '$KEYCHAIN_SERVICE'."
  read -r -p "Replace it? [y/N] " yn
  case "$yn" in
    [Yy]*) security delete-generic-password -s "$KEYCHAIN_SERVICE" >/dev/null ;;
    *) echo "aborted."; exit 1 ;;
  esac
fi

read -s -p "Upload keystore password: " PASS
echo
if [ -z "$PASS" ]; then
  echo "error: empty password" >&2
  exit 1
fi

security add-generic-password \
  -a "$USER" \
  -s "$KEYCHAIN_SERVICE" \
  -w "$PASS"
unset PASS

echo "Stored in Keychain under service '$KEYCHAIN_SERVICE'."
echo "First read will pop a GUI prompt; choose 'Always Allow' for hands-free builds."
