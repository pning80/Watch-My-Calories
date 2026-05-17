#!/bin/bash
# Accessibility-identifier parity check (PORTING_CRITERIA.md T2.4).
#
# Verifies that Android `utils/AccessibilityTags.kt` is a strict superset of
# iOS `AccessibilityIdentifiers.swift` — every string value declared on iOS
# must also be declared on Android. Android-only extras are allowed.
#
# Compares the literal string *values* (e.g., "tab_dashboard"), not the
# constant *names* (Swift uses camelCase, Kotlin uses SCREAMING_SNAKE_CASE).
# This is the right level of comparison because the values are what UI tests
# read — the constant names are a per-language detail.
#
# Usage:
#   ./scripts/accessibility-diff.sh
# Exit codes:
#   0 — Android is a superset (test-shareable)
#   1 — at least one iOS identifier is missing on Android (prints diff)
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IOS_FILE="$ROOT/WatchMyCalories/WatchMyCalories/AccessibilityIdentifiers.swift"
ANDROID_FILE="$ROOT/WatchMyCaloriesAndroid/app/src/main/java/com/pning80/watchmycalories/utils/AccessibilityTags.kt"
OUT_DIR="$ROOT/shared-fixtures/accessibility-diff"

mkdir -p "$OUT_DIR"

# Extract all double-quoted string literals from a Swift / Kotlin source file
# that look like accessibility identifiers (underscore-separated lowercase).
# Both iOS Swift `static let foo = "value"` and Android Kotlin `const val FOO = "value"`
# put the literal as the last "..." on the line, so a single regex covers both.
extract_id_values() {
    local file="$1"
    grep -oE '"[a-z][a-zA-Z0-9_]*"' "$file" | tr -d '"' | sort -u
}

ios_ids=$(extract_id_values "$IOS_FILE")
android_ids=$(extract_id_values "$ANDROID_FILE")

printf '%s\n' "$ios_ids" > "$OUT_DIR/ios.txt"
printf '%s\n' "$android_ids" > "$OUT_DIR/android.txt"

if [ -z "$ios_ids" ]; then
    echo "ERROR: extracted no identifiers from $IOS_FILE — extractor regex may be broken."
    exit 2
fi
if [ -z "$android_ids" ]; then
    echo "ERROR: extracted no identifiers from $ANDROID_FILE — extractor regex may be broken."
    exit 2
fi

# Set difference: identifiers in iOS that are NOT in Android. Anything in this
# set is a parity violation. `comm -23` shows lines only in the first file.
missing_on_android=$(comm -23 "$OUT_DIR/ios.txt" "$OUT_DIR/android.txt")

# Android extras (lines only in Android) — fine; report for visibility.
android_extras=$(comm -13 "$OUT_DIR/ios.txt" "$OUT_DIR/android.txt")

if [ -n "$missing_on_android" ]; then
    echo "FAIL: $(echo "$missing_on_android" | wc -l | tr -d ' ') iOS identifier(s) missing on Android:"
    echo "$missing_on_android" | sed 's/^/  /'
    echo ""
    echo "Add these to WatchMyCaloriesAndroid/.../utils/AccessibilityTags.kt"
    echo "See PORTING_CRITERIA.md T2.4."
    exit 1
fi

ios_count=$(echo "$ios_ids" | wc -l | tr -d ' ')
android_count=$(echo "$android_ids" | wc -l | tr -d ' ')
extras_count=$(if [ -z "$android_extras" ]; then echo 0; else echo "$android_extras" | wc -l | tr -d ' '; fi)

echo "OK: Android is a superset of iOS ($android_count Android IDs, $ios_count iOS IDs, $extras_count Android-only extras)"
if [ -n "$android_extras" ]; then
    echo "Android-only extras (allowed):"
    echo "$android_extras" | sed 's/^/  /'
fi
echo "Per-side dumps written to: $OUT_DIR"
