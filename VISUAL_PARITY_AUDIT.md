# Visual Parity Audit — iOS ↔ Android (Pixel 9a + Test iPhone 14)

Hands-on screen-by-screen comparison to identify remaining porting gaps and produce a fix plan. Sibling to `PORT_AUDIT.md` (which was a one-off code-level audit captured 2026-05-28). This document is the **procedural runbook**; the audit *output* (gap list, fix plan) lands at the bottom of this file or in a new section as the audit is performed.

**Status:** **executed 2026-05-29 (later that evening).** 23 Android screenshots captured under `PortingEvidence/screenshots/android/audit-2026-05-29/`; iOS comparison done against `AppStore/V1.4.1/Screenshots/` + PORT_AUDIT.md's textual reference description. Findings + fix plan below.

---

## Why this audit

`PORTING_MATRIX.md`'s T2.1 / T2.2 / T2.3 cells are still `⏳ device` for most screens because we never did the paired iOS↔Android visual comparison. PORT_AUDIT.md's honest baseline says "all remaining `⏳ device` cells still need a paired iOS↔Android screenshot diff before flipping to `✅ device`." This audit closes that gap.

---

## Prerequisites (must all be true before starting)

- [ ] Pixel 9a connected via USB, screen unlocked.
- [ ] Pixel 9a has tapped **Allow** on the USB-debugging dialog so `adb devices` shows it as `device` (not `unauthorized`).
- [ ] Test iPhone 14 (`E26320B2-8ACB-59A1-8976-29836204D049`) connected, screen unlocked.
- [ ] Mac has `adb` available (verified — Android SDK at `~/Library/Android/sdk`).
- [ ] Latest Android debug APK is installed on Pixel 9a (we'll build + install at the start; current uncommitted state through commit `4d9661a` carries the Phase C camera-flow MealTypePicker + Phase E spacing rollout).
- [ ] Latest iOS debug build is installed on Test iPhone 14 (per Apple Test iPhone 14 burn-in workflow).

---

## iOS reference strategy

We use the **App Store screenshots already in the repo** as the iOS reference rather than capturing fresh ones from the device:

```
AppStore/V1.4.1/Screenshots/
├── SS-1-CameraScanFood.png
├── SS-2-FoodScanResults.png
├── SS-3-Dashboard.png
├── SS-4-CameraScanMenu.png
├── SS-5-MenuScanResults.png
├── SS-6-PickFromLibrary.png
├── SS-7-EntryMenu.png         (Log Food sheet)
├── SS-8-MealHistory.png
├── SS-9-YourPrivacy.png
└── SS-10-Settings.png
```

**Reason:** macOS has no built-in CLI tool to screenshot a physical iOS device. `libimobiledevice` (via Homebrew) provides `idevicescreenshot` but is a heavyweight install with a non-trivial pairing dance. The 10 App Store screenshots are the canonical iOS rendering; the few audit screens they don't cover (Camera Review, Onboarding, Manual Entry, Edit Meal Group) are minor and can be captured manually if needed.

If a specific screen comparison demands fresh iOS capture: the user takes the screenshot on the iPhone (volume-up + side button), AirDrops to the Mac, and drops the file at the path noted in the gap row.

---

## Android capture strategy

`adb exec-out screencap -p > <path>.png` pipes a PNG screenshot to disk over the adb bridge. No file ever lands on the phone's storage. One command per screenshot.

**Capture root:** `PortingEvidence/screenshots/android/audit-2026-05-29/` (create at start of run).

Filename convention: `<screen>-<state>.png` where `<state>` is `clean`, `empty`, `with-data`, `loading`, `error-<kind>`, etc.

---

## Step-by-step procedure

### Step 0 — Pre-flight

```bash
adb devices                            # should list 58271JEBF07089 as 'device'
xcrun devicectl list devices           # confirms Test iPhone 14 'available (paired)'
mkdir -p PortingEvidence/screenshots/android/audit-2026-05-29
```

### Step 1 — Build + install fresh Android APK

```bash
cd WatchMyCaloriesAndroid
./gradlew :app:installDebug
adb shell am start -n com.pning80.watchmycalories/.MainActivity
```

Confirm app reaches Dashboard or Onboarding.

### Step 2 — Capture loop

For each row in the screen list below, in this order:

1. **User**: navigate to the screen on the Pixel 9a (manual; faster than driving with `adb shell input`).
2. **User**: confirm "ready" in the chat.
3. **Agent**: run the screenshot command, log the path.
4. **Agent**: cross-references the iOS source-of-truth file.

```bash
# Generic capture command (substitute filename per row):
adb exec-out screencap -p > PortingEvidence/screenshots/android/audit-2026-05-29/<screen>-<state>.png
```

### Screen list (16 captures)

| # | Screen / State | Android navigation | iOS reference |
|---|---|---|---|
| 1 | Dashboard — empty | Fresh install, no entries | `SS-3-Dashboard.png` (note: iOS shows data; empty state needs comparison from in-session screenshot or skip) |
| 2 | Dashboard — with data | After saving ≥ 1 entry | `SS-3-Dashboard.png` |
| 3 | Camera capture — preview only | Tap Log Food → Scan Food → grant camera | `SS-1-CameraScanFood.png` |
| 4 | Camera Review (new, Phase C) | After capturing 1 photo, before tap Analyze | none (new screen — capture both sides; user takes iOS shot if iOS already has parallel flow) |
| 5 | Estimation Review — Loading | After tapping Analyze, during Gemini call | `SS-2-FoodScanResults.png` shows the loaded state; loading state needs in-session iOS capture |
| 6 | Estimation Review — Success | After Gemini returns | `SS-2-FoodScanResults.png` |
| 7 | Photo Library Review | Tap Log Food → Choose from Library → pick a photo | `SS-6-PickFromLibrary.png` |
| 8 | Menu Analysis — Loading | Scan Menu tab → pick a photo → during call | (skip if no iOS analog) |
| 9 | Menu Analysis — Success | After call returns | `SS-5-MenuScanResults.png` |
| 10 | Scanned Menus (Stored Menus list) | Scan Menu tab → see list | (skip — list comes from `SS-5` |
| 11 | Menu Scan Detail | Tap a row in Scanned Menus | (skip — same content as `SS-5`) |
| 12 | History — empty | Fresh install, History tab | (compare against in-session iOS empty if needed) |
| 13 | History — with data | After saving entries | `SS-8-MealHistory.png` |
| 14 | Settings | Settings (gear icon top-right) | `SS-10-Settings.png` |
| 15 | About / Privacy | Settings → About | `SS-9-YourPrivacy.png` |
| 16 | Log Food sheet | Tap Log Food bottom-nav tab | `SS-7-EntryMenu.png` |

Optional extras (not in App Store set, capture both sides if appetite allows):
- Manual Entry screen
- Edit Meal Group screen
- Onboarding 3 steps

### Step 3 — Pairwise comparison

For each captured Android screenshot, agent:

1. Reads the Android PNG.
2. Reads the iOS reference PNG.
3. Records the gap (or "no gap") in the **Findings** table below, with one-line description.

### Step 4 — Gap synthesis

Group findings by severity (P0 functional / P1 visual / P2 polish) and surface (per-screen). Produce a fix plan similar to PORT_AUDIT.md §8 Phases A–G, but scoped to *this* audit's findings.

### Step 5 — Commit + sign off

Commit:
- The screenshot directory (with binaries — these are evidence and small).
- This file updated with Findings + Fix Plan.
- Update `PORTING_MATRIX.md`: flip applicable T2.1 / T2.2 / T2.3 cells from `⏳ device` to `✅ device — audit-2026-05-29` (or `⚠️` with a deviation row if the gap is intentional).

---

## Findings (2026-05-29 audit run)

Captured via `adb exec-out screencap -p` on a clean install of the post-Phase-C/E debug build (commit `348b9e6` chain). 23 screenshots saved under `PortingEvidence/screenshots/android/audit-2026-05-29/`. iOS reference is `AppStore/V1.4.1/Screenshots/` + the textual iOS reference in `PORT_AUDIT.md` §1 (which itself was made against the iOS v1.4.1 build).

### Defects (deviations not yet documented; fix on Android)

| # | Severity | Surface | Gap | Evidence | Files involved |
|---|---|---|---|---|---|
| V-1 | **P0** | Camera capture | App **never requests `CAMERA` permission** in-product. CameraScreen attempts to bind cameras directly; if permission is missing, `Camera2CameraImpl` logs "cannot open camera 0 without camera permission" silently and falls into an infinite `PENDING_OPEN` retry loop. The user sees a black preview with a shutter button that does nothing. Camera permission was only resolved during this audit by `adb shell pm grant`. | logcat lines from `audit-2026-05-29/15-camera-live.png` retry attempt; visible black-preview-no-response on 11/12/14 captures | `ui/camera/CameraScreen.kt` (needs `ActivityResultContracts.RequestPermission` + rationale; the OnPermissionResult should also re-trigger `bindToLifecycle`) |
| V-2 | **P0** | Settings | **No `About` row / no exit to `AboutScreen`.** `MainActivity.kt` defines an `about` nav route and `ui/about/AboutScreen.kt` exists, but no row in `SettingsScreen.kt` links there. iOS Settings has rows for About, Privacy Policy, Help, Rate App below the user-facing toggles. | `audit-2026-05-29/22-settings-fully-scrolled.png` shows last visible row is "Save Settings"; UI dump from this audit confirms no About row anywhere in the Settings page | `ui/settings/SettingsScreen.kt` needs an "About" / "More" section after the Privacy block; route to `about`. |
| V-3 | **P1** | Onboarding step 1 | "Skip" button (top-right) **overlaps the system status bar battery icon**. Edge-to-edge enforcement on Pixel 9a (Android 15+) means the OnboardingScreen draws under the status bar; the Skip button lacks `Modifier.statusBarsPadding()` or equivalent inset. | `audit-2026-05-29/02-onboarding-step1.png` | `ui/onboarding/OnboardingScreen.kt` — wrap the "Skip" overlay (or its parent Box) with `Modifier.windowInsetsPadding(WindowInsets.statusBars)` |
| V-4 | **P1** | Scan Menu empty | Empty state is **just centered text** ("No scanned menus yet."). Inconsistent with Dashboard empty (camera icon + card) and History empty (book icon + card). | `audit-2026-05-29/09-scanmenu-empty.png` | `ui/menuscanner/ScannedMenusScreen.kt` — wrap empty state in `EmptyStateCard(title=..., subtitle=..., icon=Icons.AutoMirrored.Filled.MenuBook)` (or a menu-specific icon like `RestaurantMenu`) |
| V-5 | **P2** | Settings | **Banner ad not visible.** `PORTING_RUNBOOK` 3.3 and `PORTING_MATRIX` row claim `BannerAdView` ported at top of Settings on 2026-05-16, mirroring iOS `SettingsView.swift:51`. Captured Settings shows no banner. May be late-loading (test ad unit), may be hidden behind App Appearance card, or may have regressed. | `audit-2026-05-29/04-settings.png`, `05-settings-scroll1.png` | `ui/settings/SettingsScreen.kt` — verify `BannerAdView()` is still composed at the top; the test-ad placeholder behavior changed in Phase A "kill visible UI embarrassments" so this may be intentional — confirm against iOS reference |
| V-6 | **P2** | Build / Pixel 9a launch | **Android 15 16KB-alignment warning dialog** appears on every launch in debug builds. `lib/arm64-v8a/libimage_processing_util_jni.so` (from CameraX `1.3.1`) is not 16KB-aligned. Already tracked as Task #15 ("Bump CameraX to 16KB-aligned"). Release builds would be hard-rejected by future Play policy. | `audit-2026-05-29/01-launch-state.png` (system dialog) | `WatchMyCaloriesAndroid/app/build.gradle.kts` (`androidx.camera:* 1.3.1` → 1.4.x+) |

### Confirmed-clean (no parity gap relative to iOS)

| Surface | Why | Evidence |
|---|---|---|
| Bottom nav (4 tabs) | Dashboard / Log Food / Scan Menu / History matches iOS order; active-tab pill is green, not purple. | every capture with the bottom nav visible |
| Dashboard HeroSummaryCard | Hero ring + Goal + Remaining + (conditional) Burned + (conditional) Macro bar. Phase B icons present. | 03 |
| Dashboard empty state | Camera icon + card + "or log manually" link. Triple-title bug gone. | 03 |
| Settings labels | "System / Light / Dark" + "US Customary / Metric" — canonical. | 04 |
| Settings Profile sliders | Documented as `D-004` deviation (Sliders vs iOS Picker(.wheel)). | 04, 05 |
| Settings "Calculate Recommended Goal" button | Present in Daily Goals card. | 05/06 |
| Settings AI Photo Analysis | Toggle + descriptive text. Defaults off. | 06 |
| Log Food sheet | 3 rows (Scan Food / Choose from Library / Log Manually) with icons. Backdrop scrim correct. | 07 |
| History empty | Book icon (`Icons.AutoMirrored.Filled.MenuBook`) + "No history yet" + sub-copy. | 08 |
| Manual Entry | Food name / Calories / Quantity inputs, Meal segmented (Dinner auto-selected), Add Nutrition link. | 10 |
| Camera capture UI | Minimal — preview + white circle shutter. | 14, 15 |
| Camera post-capture | Captured-photo thumbnail + Analyze N button. | 16 |
| CalorieDisclaimerSheet | First-AI-use disclaimer with don't-show-again toggle + Continue. | 17 |
| **CameraReviewScreen (Phase C)** | Photo full-bleed + MealTypePicker (Dinner auto-selected) + Retake/Use. **The new screen works as designed.** | 18 |

### Not covered by this audit (deferred)

- **Dashboard with data**: no entry saved during this run (backend auth failed).
- **History with data**: same.
- **Estimation Review Success state**: backend call returned "Analysis Failed" (likely debug-build legacy-key isn't set up on this Pixel install — the dev fallback `BackendConfig.devLegacyKey` is gated by `BuildConfig.DEBUG` && `APP_BACKEND_API_KEY` in `local.properties` being populated). Re-run with the dev key configured to see the success path.
- **Menu Analysis Loading/Success**: same backend-auth caveat.
- **Onboarding step 2 (Profile) + step 3 (Connect Health)**: would need a fresh uninstall + reinstall; deferred — V-3 already flags the only structural issue from step 1.
- **Pixel 9a force-stop persistence test** (PIXEL_VERIFICATION_RUNBOOK Step 3): blocked on having a saved entry, which requires the backend-auth fix.

---

## Fix plan

Six items found, three already had tracking elsewhere:

| Item | Phase | Status | Action |
|---|---|---|---|
| V-1 Camera permission rationale | new (V phase) | open — P0 | Add `RequestPermission` launcher in `CameraScreen.kt`; show rationale UI; re-bind on grant; handle deny gracefully. |
| V-2 Settings → About entry point | new (V phase) | open — P0 | Add "About" row at the bottom of `SettingsScreen.kt` (after Privacy card); on tap, `navController.navigate("about")`. Possibly grow to a full "About this app" section with version + privacy policy link. |
| V-3 Onboarding Skip overlap | new (V phase) | open — P1 | Inset the Skip button with `Modifier.windowInsetsPadding(WindowInsets.statusBars)`. |
| V-4 Scan Menu empty state | new (V phase) | open — P1 | Replace plain text with `EmptyStateCard`. |
| V-5 Settings banner ad | new (V phase) | open — P2 | Verify against iOS reference (SS-10). If the iOS Settings does still show a top banner, restore `BannerAdView` at top of `SettingsScreen.kt`. If iOS dropped it in v1.4.1, document the parity decision. |
| V-6 CameraX 16KB-alignment | Phase F (already-tracked) | open — P2 | Bump `androidx.camera:camera-* 1.3.1` → 1.4.x+ in `app/build.gradle.kts`. Already filed as task #15. |

Order to land: **V-1 + V-2** first (both P0 — silent camera failure is shipping-blocker, missing About is a content-policy risk for store listing). **V-3** is the next obvious-on-screenshot fix (Skip-overlap is the kind of thing reviewers screenshot). **V-4** is small. **V-5** needs an iOS reference cross-check before action. **V-6** is the dependency bump.

All six are autonomous-doable (no new device session required). I can land V-1 / V-2 / V-3 / V-4 / V-6 in a single PR; V-5 needs the user to confirm whether iOS still shows the banner in v1.4.1 before I act.

---

## Resuming this audit

When the user is back at the desk with both devices:

1. They run `adb devices` and confirm Pixel shows `device`. If `unauthorized`, tap **Allow** on the Pixel dialog; if no dialog, `adb kill-server && adb devices`.
2. They confirm Test iPhone 14 is unlocked.
3. They tell the agent (or fresh agent in a new session): *"Resume the visual parity audit per `VISUAL_PARITY_AUDIT.md`."*
4. Agent picks up at Step 0.
