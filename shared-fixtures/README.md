# Shared Fixtures

Golden-vector fixtures consumed by **both** the iOS test suite and the Android test suite. Owned by no single platform — changing a fixture means changing both consumers in the same PR.

## Why this exists

Both platforms must produce numerically identical outputs for BMR, TDEE, remaining-calories math, macro totals, and meal-type bucketing. The only honest way to verify this is to share the *input vectors and expected outputs* across both test suites, so a drift on either platform fails the corresponding test there.

## Layout

```
shared-fixtures/
├── README.md (this file)
├── bmr-mifflin-st-jeor/cases.json   — input matrix + expected BMR per row
├── meal-type-by-hour/cases.json     — hour 0–23 → expected MealType
├── schema-diff/                     — exported model schemas (field lists), per platform
│   ├── FoodEntry.ios.txt   / FoodEntry.android.txt
│   ├── MenuScan.ios.txt    / MenuScan.android.txt
│   └── UserProfile.ios.txt / UserProfile.android.txt
└── accessibility-diff/              — testTag / accessibility-identifier sets, per platform
    ├── ios.txt
    └── android.txt
```

## Format conventions

- **JSON** unless the fixture is intentionally not JSON (e.g., a malformed payload to test parser resilience — `.txt`).
- **Numbers are not floats in the spec.** Use integer cents-style scaling where precision matters; for calorie math, integers (rounded to 1 kcal) are fine; for ratios, document the precision in the file header.
- **Field names match iOS naming exactly.** `imageID`, not `imageId`. `itemsData`, not `itemsJson`. (iOS field naming is the reference; Android matches it.)
- **Every fixture has a top-level `description` and `source` field** so a reader can tell whether it's hand-authored or captured from a real run.

## How to add a fixture

1. Add the file under the appropriate subdirectory.
2. Update both test suites in the same PR — iOS under `WatchMyCalories/WatchMyCaloriesTests/` (when added), Android under `WatchMyCaloriesAndroid/app/src/test/`.
3. Run both suites locally to confirm they consume the fixture correctly.
4. If the fixture asserts numerically identical behavior, prove it with both platforms' test runs in the PR description.

## How to update an existing fixture

Updating a fixture is **always** a behavior change on both platforms by definition. The PR must:
- Update the fixture.
- Update both test suites (even if only to bump the assertion).
- Explain in the PR description why both platforms moved together.

A "fix on one platform only" by editing a shared fixture is a process violation — an intentional divergence should instead be allowlisted in `scripts/schema-diff.sh`.
