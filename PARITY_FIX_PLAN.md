# Parity Fix Plan — actionable closure of every inconsistency

Produced 2026-05-30 as Task #11 output. The user-facing deliverable that completes the goal:
> "use the iOS app as the reference and UI test cases as the vehicle to verify the Android app on every screen and every possible tap, and the output is the list of inconsistencies and a plan to fix every single issue."

The list is in `PARITY_INCONSISTENCIES.md`. This file is the plan.

---

## Recommended sequencing

Phases ordered so that each unlocks the next without rework. PR boundaries called out where the work crosses the iOS/Android boundary or the porting/iOS-test boundary.

### Phase 1 — Land iOS XCUITest extension as standalone PR (~6 hrs, mostly done)

**Status: ~70% complete as of 2026-05-30.** Code lives on disk; smoke tests confirm patterns work. Remaining: full-suite green pass + a few cleanup items.

**Deliverables:**

1. Test-only iOS source changes (gated by `--uitesting`):
   - `MockEstimationService.Mode { success, error, noFood }` + launch-arg-driven init
   - 6 new launch args in `WatchMyCaloriesApp`: `--mock-estimation-error`, `--mock-estimation-no-food`, `--ai-consent-accepted`, `--seed-menu-scans`, `--seed-multi-item-meal`, `--seed-with-image`
   - 3 new seed functions: `seedMenuScans()`, `seedMultiItemMeal()`, `seedWithImage()`
2. ~70 new XCUITest functions in 8 files closing ~100 of 132 SPEC-GAPs:
   - `SettingsTests.swift` (+9) — picker value-change, disclosure expansion (metric mode), Manage Privacy Choices, keyboard Done
   - `OnboardingTests.swift` (+7) — picker interactions on goal step, advance buttons
   - `ManualEntryTests.swift` (+4) — Protein/Carbs/Fat fields, full-nutrition save
   - `EstimationReviewTests.swift` (+8) — error state (3), no-food state (2), AI consent (2), helpers
   - `ScanMenuTests.swift` (+9) — scan-menu sheet taps, scanned-menus list with seed, edit mode, swipe delete, detail screen
   - `DashboardTests.swift` (+10) — multi-item meal group, full-screen image, pull-to-refresh, edit/view context menu
   - `HistoryTests.swift` (+5) — group cards, full-screen image, context menus in history view
   - `AppMenuTests.swift` (+6) — About screen version/help/privacy/rate links, Settings banner ad
   - `TabNavigationTests.swift` (+2) — Camera Cancel, Stored Menus Done

**PR title:** `iOS: extend XCUITest spec for parity audit (Principle-#7 exception)`

**Reviewer guidance:** This PR explicitly violates the letter of `PORTING_CRITERIA.md` Operating Principle #7. The user granted the exception on 2026-05-30. No production behavior changes — every change is gated by `--uitesting` ProcessInfo arg. The PR description must note the exception.

### Phase 2 — Address the two iOS bugs surfaced by Phase 1 (~1.5 hrs)

Both bugs from `PARITY_INCONSISTENCIES.md` Class F. Each lands as a separate iOS-only PR per the principle.

**PR title:** `iOS: add review_error accessibility identifier (parity audit follow-up)`

- Add `.accessibilityIdentifier(AccessibilityID.EstimationReview.errorView)` to the error-state VStack inside `EstimationReviewView.swift:87+` so XCUITest queries by ID work. Update `WatchMyCaloriesUITests/EstimationReviewTests.swift::waitForEstimationResult` to actually return `"error"`.

**PR title:** `iOS: button-style audit for accessibilityIdentifier preservation`

- Inventory every `.buttonStyle(.borderedProminent)` that also has `.accessibilityIdentifier`. Either swap to a custom button modifier that preserves the ID in the XCUIElement tree, or move the ID to a sibling element that XCUITest can find.

### Phase 3 — Android test infrastructure as standalone PR (~6–10 hrs)

This is the **prerequisite** for every test in Phases 4+. Without it the test mirrors are unwritable.

**Deliverables (in `WatchMyCaloriesAndroid/`):**

1. **Intent-extra-driven test mode in `MainActivity.onCreate`** that reads keys like:
   - `wmc.test.seedFoodEntries` (JSON array)
   - `wmc.test.seedMenuScans` (JSON array)
   - `wmc.test.aiConsent` (`accepted`/`declined`/`notAsked`)
   - `wmc.test.mockEstimationMode` (`success`/`error`/`noFood`)
   - `wmc.test.uitesting` (boolean — skip onboarding by default, swap GeminiRepository)
2. **`MockGeminiRepository`** in androidTest source set with the same `Mode` enum as iOS. Wired into `MainActivity:111` via an injectable factory (refactor `geminiRepository` from `remember { GeminiRepository(context) }` to read from a `RepositoryFactory.create(context)` that honors test intent extras).
3. **`TestHooks.kt`** in androidTest source set:
   - Helpers to construct the intent extras
   - Helpers to seed Room DB directly via `db.foodEntryDao().insert(...)`
   - Helpers to wait for DataStore writes to settle
4. **`MainActivityComposeTest`** base class wrapping `createAndroidComposeRule<MainActivity>()` with `launchEmpty()`, `launchWithSeedData()`, `launchWithMenuScans()`, `launchWithMultiItemMeal()`, `launchWithImage()`, `launchWithEstimationError()`, etc., mirroring `WatchMyCaloriesUITestBase` 1:1.

**PR title:** `Android: instrumented test infrastructure for parity audit`

**Reviewer guidance:** This PR touches `MainActivity.kt` (production code) but the changes are gated by `intent.extras.getBoolean("wmc.test.uitesting")` — defaults to false, no production behavior change. The `MockGeminiRepository` lives in `androidTest/` so it's not in production APK.

### Phase 4 — Mirror existing Robolectric to instrumented (~4 hrs)

The 48 Class-B `WEAK` rows. Each Robolectric test in `app/src/test/.../ui/{dashboard,history,settings,onboarding,entry}/*Test.kt` gets an instrumented twin in `app/src/androidTest/.../ui/{...}/`.

**PR title:** `Android: upgrade Compose UI tests to instrumented (parity audit)`

**Order, by leverage:**

1. `Manual Entry` (11 rows) — smallest, fastest. Validates the new infra.
2. `Settings` (14 rows) — establishes picker/toggle/textfield patterns.
3. `Onboarding` (15 rows) — exercises multi-step nav.
4. `Dashboard` (4 rows beyond the 3 already instrumented).
5. `History` (4 rows).

### Phase 5 — Mirror new iOS XCUITests to Android (~6 hrs)

The ~70 new iOS test functions added in Phase 1. Each gets a 1:1 Android counterpart using the Phase 3 infrastructure.

**PR title:** `Android: instrumented mirrors for extended iOS spec (parity audit)`

**Order:** same as Phase 1's file ordering (Settings extension first; then Onboarding; etc.)

### Phase 6 — Run + iterate (~3 hrs)

1. `./gradlew connectedAndroidTest` on Pixel 9a; capture HTML report.
2. For each `FAIL` row, classify:
   - **Android-bug** — Android code path diverges from iOS; file a bug, fix in a porting PR, re-run.
   - **Test-bug** — Android test queries wrong tag or asserts wrong semantic; fix the test.
   - **Documented divergence** — add a new `D-NNN` row to `PORTING_DEVIATIONS.md` and update the test to assert the Android variant.
3. Re-run until 100% pass.

### Phase 7 — Architectural-blocker decision (out of band, ~12 hrs if pursued)

The Class-C `BLOCK-IOS` rows (~30) require either:

**Option A — defer with `D-NNN` deviations** (~0 hrs)

Add deviation rows to `PORTING_DEVIATIONS.md`:
- `D-005` MenuAnalysisView state coverage deferred; relies on real Gemini path
- `D-006` MenuCameraView state coverage deferred; no simulator camera
- `D-007` PhotoLibraryReview state coverage deferred; PhotosPicker / system picker can't be mocked

**Option B — refactor to enable testing** (~12 hrs)

- Introduce `protocol MenuAnalysisService` in iOS `Services.swift`; inject via `AppEnvironment` like `EstimationService`. Add `MockMenuAnalysisService` with modes.
- Mirror on Android: extract menu-analysis into a separate repository class with mockable interface.
- Add a synthesized photo-library path for both platforms (test-only, generates a fake selected image).
- Then write the 30 tests.

Recommendation: **defer (Option A)** unless menu analysis is a high-traffic flow with real users hitting state issues. The deferral is cleaner — these states will be exercised in production smoke tests rather than UI tests.

### Phase 8 — Final inconsistency log update + sign-off

After Phases 1–6 (+ Phase 7 decision) land:

1. Re-run iOS XCUITest suite, capture xcresult.
2. Re-run Android instrumented suite, capture HTML report.
3. Regenerate `PARITY_INCONSISTENCIES.md` Class A/B/D counts → should be near zero.
4. Update `PORTING_MATRIX.md` cells from `⏳ device` → `✅ device · audit-2026-05-30` for every row that's now both-platforms-instrumented-green.
5. Final reviewer sign-off.

---

## Effort summary (totals)

| Phase | Status | Effort |
|---|---|---:|
| 1. iOS XCUITest extension | 70% done (this session) | ~2 hr remaining |
| 2. iOS bug fixes from Phase 1 | not started | ~1.5 hr |
| 3. Android test infrastructure | not started (prereq for 4+5) | ~6–10 hr |
| 4. Robolectric → instrumented uplift | not started | ~4 hr |
| 5. New iOS tests mirrored to Android | not started | ~6 hr |
| 6. Run + iterate | not started | ~3 hr |
| 7. Architectural-blocker decision (Option A = defer) | not started | 0–12 hr |
| 8. Final log + sign-off | not started | ~1 hr |
| **Total remaining to full closure (Option A defer)** | | **~23 hr** |
| **Total remaining if architectural refactor pursued (Option B)** | | **~35 hr** |

---

## Critical-path summary

The **single biggest blocker** to closing every inconsistency is **Phase 3 (Android test infrastructure)** — without intent-driven seed/mock/state-setup on Android, the 218 instrumented tests we need to write are not writable. Phase 3 should be the first Android-side PR after Phase 1 ships.

Phases 4 + 5 can run in parallel after Phase 3 lands. Phase 6 must follow Phases 4+5. Phase 2 (iOS bug fixes) is independent and can land anytime.

If the user wants a dip-test of the framework before committing to the full ~23–35 hr build-out, the **smallest meaningful PR is Phase 1 + a 5-test Android mirror of Manual Entry** (~8 hr total) — that exercises the entire pipeline end-to-end on one small surface, surfaces any unforeseen infrastructure issues, and produces a real inconsistency-log row for one screen.

---

## Open questions for the user

1. **Architectural refactor (Phase 7)** — do you want Menu Analysis / Menu Camera / Photo Library tested via refactor (Option B, ~12 hr), or accepted as documented deviations (Option A, 0 hr)?
2. **Phasing** — land Phase 1 alone as a PR for review before Phase 3 starts, or batch Phases 1+3 into one bigger PR?
3. **Coverage bar relaxation** — the strict "instrumented-only" bar costs ~30 hr of test conversion vs ~6 hr if Robolectric were accepted as evidence for non-system-dependent screens (Dashboard, History, Settings, Onboarding, Manual Entry). Do you want to relax for these specifically?
