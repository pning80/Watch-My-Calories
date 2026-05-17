#!/bin/bash
# PORTING_CRITERIA.md T1.5 grep gate.
#
# Fails if any of the banned patterns appear under the Android app source:
#   - `generativeai` — Google AI SDK package; must be gone post-port
#   - `YOUR_API_KEY` — placeholder API key from the pre-port code
#   - `GenerativeModel(`  — SDK constructor invocation
#
# Intended to run in CI on every PR touching Android. Locally:
#   ./scripts/check-android-no-gemini-sdk.sh
# Exit codes:
#   0 — clean (no offenders found)
#   1 — at least one offender; output lists file:line
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$ROOT/WatchMyCaloriesAndroid/app/src"
GRADLE_FILE="$ROOT/WatchMyCaloriesAndroid/app/build.gradle.kts"

if [ ! -d "$SRC_DIR" ]; then
    echo "ERROR: Android source directory not found: $SRC_DIR"
    exit 2
fi

OFFENDERS=0

# Source-tree checks
for pattern in 'generativeai' '"YOUR_API_KEY"' 'GenerativeModel('; do
    matches=$(grep -rn --include='*.kt' --include='*.java' -F "$pattern" "$SRC_DIR" || true)
    if [ -n "$matches" ]; then
        echo "FAIL: banned pattern '$pattern' found in Android source:"
        echo "$matches" | sed 's/^/  /'
        OFFENDERS=$((OFFENDERS + 1))
    fi
done

# Gradle-dep check (the SDK dependency itself)
if grep -n -F 'generativeai' "$GRADLE_FILE" > /dev/null 2>&1; then
    echo "FAIL: 'generativeai' dependency still present in $GRADLE_FILE:"
    grep -n -F 'generativeai' "$GRADLE_FILE" | sed 's/^/  /'
    OFFENDERS=$((OFFENDERS + 1))
fi

if [ "$OFFENDERS" -gt 0 ]; then
    echo ""
    echo "$OFFENDERS check(s) failed. See PORTING_CRITERIA.md T1.5."
    exit 1
fi

echo "OK: no banned patterns found in Android source."
