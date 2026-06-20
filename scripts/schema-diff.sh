#!/bin/bash
# Schema-diff tool for PORTING_CRITERIA.md T1.2.
#
# Compares the data-model field set on iOS (SwiftData @Model classes in
# WatchMyCalories/WatchMyCalories/DataModels.swift) vs Android (Room
# @Entity classes in WatchMyCaloriesAndroid/.../data/Entities.kt) and
# fails if any model has a field-name mismatch.
#
# This is a first-pass diff — field names only, not types or optionality.
# It catches the casing-drift bugs (imageId vs imageID, itemsJson vs
# itemsData) that the original audit found.
#
# Usage:
#   ./scripts/schema-diff.sh
# Exit codes:
#   0 — field sets match
#   1 — at least one mismatch (prints diff)
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IOS_FILE="$ROOT/WatchMyCalories/WatchMyCalories/DataModels.swift"
ANDROID_FILE="$ROOT/WatchMyCaloriesAndroid/app/src/main/java/com/pning80/watchmycalories/data/Entities.kt"
OUT_DIR="$ROOT/shared-fixtures/schema-diff"

mkdir -p "$OUT_DIR"

# Extract stored field names from a SwiftData @Model class.
#
# iOS distinguishes stored from computed properties:
#   var foo: Bar              ← stored (no body)
#   var foo: Bar = 0          ← stored, with default
#   var foo: Bar { ... }      ← computed (has body) — IGNORE
#
# We accept lines whose property declaration ends without an opening brace
# (after stripping the type annotation up to `=` or end-of-line).
extract_ios_fields() {
    local model="$1"
    sed -n "/^[[:space:]]*[a-z]* class ${model}[[:space:]:{]/,/^}/p" "$IOS_FILE" |
        grep -E '^[[:space:]]*var[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*:' |
        grep -vE '\{[[:space:]]*$' |    # exclude opening brace at end of line (computed)
        grep -vE ':[[:space:]]*[A-Za-z_]+[?!]?[[:space:]]*\{' |  # exclude `var foo: Bar {` on one line
        sed -E 's/^[[:space:]]*var[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*:.*/\1/' |
        sort -u
}

# Kotlin data classes — block between `data class <Name>(` and the next `)`.
# Lines may start with `@PrimaryKey ` before `val foo:`.
extract_android_fields() {
    local entity="$1"
    sed -n "/^data class ${entity}(/,/^)/p" "$ANDROID_FILE" |
        grep -E 'val[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*:' |
        sed -E 's/^[[:space:]]*(@PrimaryKey[[:space:]]+)?val[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*:.*/\2/' |
        sort -u
}

MODELS="UserProfile FoodEntry MenuScan"
MISMATCHES=0

# Documented, sign-off-approved divergences from PORTING_DEVIATIONS.md.
# Format: "<model>:<field>:<side>" where <side> is "ios" (allowed iOS-only field)
# or "android" (allowed Android-only field). The diff treats matched entries as
# expected and does not count them as mismatches.
ALLOWLIST=(
    "UserProfile:id:android"  # D-001 — Room @PrimaryKey requirement; iOS singleton-by-convention
)

is_allowlisted() {
    local model="$1" field="$2" side="$3"
    for entry in "${ALLOWLIST[@]}"; do
        [ "$entry" = "${model}:${field}:${side}" ] && return 0
    done
    return 1
}

for model in $MODELS; do
    ios_fields=$(extract_ios_fields "$model")
    android_fields=$(extract_android_fields "$model")

    printf '%s\n' "$ios_fields" > "$OUT_DIR/${model}.ios.txt"
    printf '%s\n' "$android_fields" > "$OUT_DIR/${model}.android.txt"

    if [ -z "$ios_fields" ] || [ -z "$android_fields" ]; then
        echo "--- $model: extraction returned no fields (extractor regex needs fixing) ---"
        echo "  iOS fields:     $(echo "$ios_fields" | tr '\n' ' ')"
        echo "  Android fields: $(echo "$android_fields" | tr '\n' ' ')"
        MISMATCHES=$((MISMATCHES + 1))
        continue
    fi

    # Compute set differences, then filter the allowlist.
    only_in_ios=$(comm -23 "$OUT_DIR/${model}.ios.txt" "$OUT_DIR/${model}.android.txt")
    only_in_android=$(comm -13 "$OUT_DIR/${model}.ios.txt" "$OUT_DIR/${model}.android.txt")

    undocumented_ios=""
    while IFS= read -r f; do
        [ -z "$f" ] && continue
        is_allowlisted "$model" "$f" "ios" || undocumented_ios+="${f}"$'\n'
    done <<< "$only_in_ios"

    undocumented_android=""
    while IFS= read -r f; do
        [ -z "$f" ] && continue
        is_allowlisted "$model" "$f" "android" || undocumented_android+="${f}"$'\n'
    done <<< "$only_in_android"

    if [ -n "$undocumented_ios" ] || [ -n "$undocumented_android" ]; then
        echo "--- $model: undocumented field-name mismatch ---"
        if [ -n "$undocumented_ios" ]; then
            echo "  iOS only (not in the ALLOWLIST in scripts/schema-diff.sh):"
            echo "$undocumented_ios" | sed 's/^/    /' | sed '/^[[:space:]]*$/d'
        fi
        if [ -n "$undocumented_android" ]; then
            echo "  Android only (not in the ALLOWLIST in scripts/schema-diff.sh):"
            echo "$undocumented_android" | sed 's/^/    /' | sed '/^[[:space:]]*$/d'
        fi
        echo ""
        MISMATCHES=$((MISMATCHES + 1))
    fi
done

if [ "$MISMATCHES" -gt 0 ]; then
    echo "$MISMATCHES model(s) failed schema check. See PORTING_CRITERIA.md T1.2."
    echo "Per-model dumps written to: $OUT_DIR"
    exit 1
fi

echo "OK: field-name sets match across $MODELS"
echo "Per-model dumps written to: $OUT_DIR"
