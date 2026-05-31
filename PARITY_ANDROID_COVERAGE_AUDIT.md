# Android Instrumented Coverage Audit — vs Extended iOS XCUITest Spec

Produced 2026-05-30 as Task #6 output. Sibling to `PARITY_COVERAGE_AUDIT.md` (iOS-side audit) and `PARITY_LEDGER.md` (per-element enumeration).

**Strict coverage bar** (per user direction 2026-05-30): only `app/src/androidTest/` instrumented tests on Pixel 9a count as parity verification. `app/src/test/` Robolectric tests count as **WEAKER-EVIDENCE** — they exercise Compose code but on a JVM stub of the framework, missing device-specific layout / permission / lifecycle / animation behavior.

---

## Counts at the file level

### Instrumented (androidTest) — runs on Pixel 9a

| File | @Test count | iOS-spec surfaces covered |
|---|---:|---|
| `EndToEndFlowTest.kt` | 1 | TabNavigation round-trip + gear-icon → Settings |
| `AppDatabaseTest.kt` | 3 | Room CRUD (not in iOS XCUITest scope — unit-level) |
| `SettingsDataStoreTest.kt` | 2 | DataStore round-trip (not in iOS XCUITest scope) |
| `DashboardScreenTest.kt` | 3 | Dashboard empty state + hero card + meal sections (3/14 dashboard rows) |
| `HistoryScreenTest.kt` | 2 | History empty + macro row (2/11 history rows) |
| **Total** | **11** | **~6 UI flows + 2 layer-level checks** |

### Robolectric (test) — WEAKER-EVIDENCE

| File | @Test count | iOS-spec surfaces covered |
|---|---:|---|
| `DashboardScreenTest.kt` (test) | 9 | Dashboard render + content |
| `HistoryScreenTest.kt` (test) | 9 | History render + content |
| `SettingsScreenTest.kt` (test) | 16 | Settings rows + interactions |
| `ManualEntryScreenTest.kt` (test) | 13 | Manual Entry fields + save logic |
| `OnboardingScreenTest.kt` (test) | 17 | Onboarding steps |
| `EndToEndFlowTest.kt` (test) | 1 | Cross-screen (Robolectric version) |
| `AppDatabaseTest.kt` (test) | 3 | Room CRUD |
| Various unit tests (parser/calculator/data) | 64 | logic-only, not UI |
| **Total** | **132** | **5 screens + units** |

---

## Per-screen / per-criterion coverage diff

Using `PARITY_LEDGER.md`'s 189 consolidated rows (the iOS coverage audit categorized 300 fine-grained sub-elements; for the Android rollup we use the consolidated row counts). Each row's Android-verify column gets one of:

- **`✅ instrumented`** — covered by a real instrumented test on Pixel 9a
- **`⚠ robo-only`** — covered by Robolectric (WEAKER-EVIDENCE per strict bar)
- **`❌ GAP`** — no Android test of either type
- **`n/a`** — display-only

### Per-screen rollup

| Screen | Total interactive rows | ✅ instrumented | ⚠ robo-only | ❌ GAP | n/a |
|---|---:|---:|---:|---:|---:|
| Dashboard | 14 | 3 | 4 | 1 | 6 |
| History | 11 | 2 | 4 | 0 | 5 |
| Settings | 23 | 0 | 14 | 7 | 2 |
| Onboarding | 17 | 1 (skip via E2E) | 15 | 1 | 0 |
| Camera (food) | 12 | 0 | 0 | 11 | 1 |
| Menu Camera | 11 | 0 | 0 | 10 | 1 |
| Estimation Review | 10 | 0 | 0 | 10 | 0 |
| Menu Analysis | 9 | 0 | 0 | 9 | 0 |
| Scanned Menus | 7 | 0 | 0 | 6 | 1 |
| Photo Library Review | 9 | 0 | 0 | 9 | 0 |
| LogFoodSheet | 3 | 0 | 0 | 3 | 0 |
| ScanMenuSheet | 3 | 0 | 0 | 3 | 0 |
| About | 4 | 0 | 0 | 4 | 0 |
| ContentView Tabs | 7 | 1 (round-trip) | 0 | 6 | 0 |
| Manual Entry | 11 | 0 | 11 | 0 | 0 |
| Components (Edit/View/FullScreen/AIConsent) | 31 | 0 | 0 | 31 | 0 |
| AppMenu | 4 | 1 (E2E gear→Settings) | 0 | 3 | 0 |
| Banner/Native Ads | 3 | 0 | 0 | 3 | 0 |
| **Total** | **189** | **8** | **48** | **127** | **15** |

(Numbers don't add to 300 because the ledger combines some rows; this rollup uses the per-screen counts from `PARITY_COVERAGE_AUDIT.md`.)

### The headline numbers

- **8 of 189** interactive surfaces have **real device verification** on Android (~4%)
- **48 of 189** have weaker-evidence Robolectric coverage (~25%)
- **127 of 189** have **no Android test at all** (~67%)
- The remaining 15 are display-only

By the user's strict bar (instrumented-only), the Android port is **96% unverified at the surface level**.

---

## Top-10 highest-value GAP clusters

Same ordering as iOS-side `PARITY_COVERAGE_AUDIT.md` but reversed: these are now Android gaps to close, ordered by ease + leverage:

1. **Settings picker / disclosure / banner-ad** — 7 GAPs (iOS Robolectric covers 14 of 23, but instrumented is 0). Quick wins: bring 16 Robolectric tests into androidTest with `createAndroidComposeRule<MainActivity>()`.
2. **Manual Entry full coverage** — 11 Robolectric → 11 instrumented mirror. Highest yield-per-test (small surface, deterministic).
3. **Onboarding 3-step flow** — 17 Robolectric → 17 instrumented mirror. Includes skip/finish/picker interactions.
4. **History day-card / macro-row / group-card interactions** — 4 GAPs not in any test bucket; 5+ Robolectric tests need upgrading.
5. **Dashboard group-card + edit context-menu** — 1 instrumented-grade GAP + multiple Robolectric upgrades.
6. **Tab-navigation modal-root cancels** — 6 GAPs (Camera Cancel, Menu Camera Cancel, Stored Menus Done, etc.) — small but invisible to users today.
7. **About screen rows** — 3 GAPs (Help/Privacy/Rate App taps). Easy.
8. **AppMenu menu items** — 3 GAPs.
9. **Estimation Review state UI** — 10 GAPs. Blocked by needing a mock-mode equivalent for Android `GeminiRepository` (similar to iOS `MockEstimationService` mode hooks added 2026-05-30).
10. **Menu Camera + Menu Analysis + Photo Library Review** — 30+ GAPs. Architectural blockers: photo-library mocking, camera permission state, real-service mocking. Same constraints as iOS side; mostly impossible to mirror until both apps expose a test seam.

---

## What's needed before any of these can be mirrored

Concrete test infrastructure that doesn't yet exist on Android:

1. **Intent-extra-driven test mode in `MainActivity.onCreate`** — equivalent of iOS `--uitesting`/`--seed-data`/etc launch args. Should read `intent.extras` keys like:
   - `wmc.test.seedFoodEntries` → seed Room with provided JSON
   - `wmc.test.seedMenuScans` → seed MenuScan rows
   - `wmc.test.aiConsent` → write to SettingsDataStore
   - `wmc.test.mockEstimationMode` → swap `GeminiRepository` for a configurable mock (success/error/noFood)
2. **Test-only mock `GeminiRepository`** — same `Mode` enum as iOS `MockEstimationService`. Should be wired into the existing manual-DI in `MainActivity:111` via an injectable factory or a singleton-with-override.
3. **`TestHooks` object** owned by androidTest source set — bundle the helpers (seed Room, set DataStore, build intent extras) into one place test files import.
4. **A base class `MainActivityComposeTest`** that wraps `createAndroidComposeRule<MainActivity>` with a fluent builder — `launchEmpty()`, `launchWithSeedData()`, `launchWithMenuScans()`, etc. — mirroring iOS `WatchMyCaloriesUITestBase`.

This is the **Task #7 prerequisite**. Without it, individual test functions are unwritable because the test can't set the app state it needs.

**Estimated cost** to add this infra: ~6–10 hours of Android-side work (MainActivity changes + mock service + base class + first mirror suite of ~20 tests). Subsequent test mirrors run at roughly the same per-test cost as iOS (~5–10 minutes each).

---

## Recommendation (input to Task #11 Fix Plan)

The cheapest path to "every iOS XCUITest has an Android instrumented mirror" is:

1. **Land the Android test infrastructure as its own PR** (similar to the iOS Principle-#7-exception PR for the iOS test-only changes). Sources: `androidTest/.../TestHooks.kt`, `androidTest/.../MainActivityComposeTest.kt`, `data/TestSeed.kt` (test-only seed helpers gated by intent extras).
2. **Convert the 132 Robolectric tests to instrumented variants** in batches (1 batch per screen). The conversions are mostly mechanical — same assertions, different rule type.
3. **Add the new tests** that mirror the iOS XCUITests added 2026-05-30 (~70 new test functions).
4. **Document deferred surfaces** (Photo Library Review, Menu Camera, Menu Analysis) as `D-NNN` deviations in `PORTING_DEVIATIONS.md` if the architecture-blocker remains, or schedule the refactor (introduce `MenuAnalysisService` protocol, photo-library mock) on its own track.

If the user wants to dip-test the framework before committing to the full ~6-10h Android-test build-out, the highest-leverage starting point is **Settings + Manual Entry** — both have full Robolectric coverage that can be ported 1:1 to instrumented, validating the test-infrastructure design with minimal new authoring.
