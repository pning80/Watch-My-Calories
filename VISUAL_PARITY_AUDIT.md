# Visual Parity Audit — iOS ↔ Android (Pixel 9a + Test iPhone 14)

Hands-on screen-by-screen comparison to identify remaining porting gaps and produce a fix plan. Sibling to `PORT_AUDIT.md` (which was a one-off code-level audit captured 2026-05-28). This document is the **procedural runbook**; the audit *output* (gap list, fix plan) lands at the bottom of this file or in a new section as the audit is performed.

**Status:** **paused 2026-05-29 — awaiting user availability with both devices.**

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

## Findings (filled in during the audit)

| # | Screen | Severity | Gap | Files involved | Notes |
|---|---|---|---|---|---|
| _to be filled_ | | | | | |

---

## Fix plan (filled in after Findings)

_to be drafted once findings are in._

---

## Resuming this audit

When the user is back at the desk with both devices:

1. They run `adb devices` and confirm Pixel shows `device`. If `unauthorized`, tap **Allow** on the Pixel dialog; if no dialog, `adb kill-server && adb devices`.
2. They confirm Test iPhone 14 is unlocked.
3. They tell the agent (or fresh agent in a new session): *"Resume the visual parity audit per `VISUAL_PARITY_AUDIT.md`."*
4. Agent picks up at Step 0.
