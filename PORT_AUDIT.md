# Watch My Calories — iOS↔Android Port Audit & Correction Plan

**Captured:** 2026-05-28 from real Pixel 9a (1080×2424, 420 dpi → 411 dp × 922 dp) running the current `main` build, compared against iOS v1.4.1 App Store reference screenshots and full code review.

**TL;DR:** The port is functionally connected but the Android UI is visibly broken in ways that would never have shipped if anyone had run the app on a phone. Three issues are blocking, ten are P1, and the spacing/theme system needs a partial rebuild. `PORTING_MATRIX.md`'s "Tier 2 ≥ 95% pass" claim is incorrect.

Live Android screenshots referenced below: `/tmp/pixel-screenshots/03-dashboard.png`, `09-history-clean.png`, `10-settings-clean.png`, `15-logfood-sheet.png`. iOS reference: `AppStore/V1.4.1/Screenshots/SS-3-Dashboard.png`, `SS-8-MealHistory.png`, `SS-10-Settings.png`.

---

## Executive summary — the 10 things visible on the first launch

| # | Severity | Issue | Evidence |
|---|---|---|---|
| 1 | **P0** | **"Watch My Calories" title is rendered THREE times** on every screen — system status bar caption + a Scaffold TopAppBar + the in-content greeting card. Looks broken at first glance. | `03-dashboard.png` lines y≈190, y≈380, y≈540 |
| 2 | **P0** | **Dashboard is missing the entire macro-breakdown bar** (Protein/Carbs/Fat with percentages) that the iOS Dashboard prominently shows under the Hero ring | iOS `SS-3-Dashboard.png` shows P/C/F bars at y≈230; Android `03-dashboard.png` has nothing between the ring and the empty-state card |
| 3 | **P0** | **Dashboard is missing the "Burned" stat row.** Only Goal and Remaining are shown; the Burned row (from Health Connect) is silently dropped, breaking the calorie equation the user is meant to see | iOS shows 3 rows with distinct dot colors; Android shows 2 |
| 4 | **P0** | **Camera flow has no meal-type picker** — iOS lets the user pick Breakfast/Lunch/Dinner/Snack on the photo-review screen; Android skips this entirely and silently auto-buckets by hour | `CameraScreen.kt` has no equivalent of `MealTypePicker.kt` (which exists but is only wired into the photo-library flow) |
| 5 | **P1** | **Default theme drift** — iOS ships light by default; Android ships dark by default. Side-by-side comparison looks like two different apps | iOS `SS-3-Dashboard.png` (white) vs Android `03-dashboard.png` (black) |
| 6 | **P1** | **"Test Ad" placeholder rendered visibly in Settings** — literal text "Nice job! / Test Ad / This is a 468x60 test ad" shows up in the user's Settings. Should be a real AdMob banner or no banner | `10-settings-clean.png` y≈170 |
| 7 | **P1** | **LogFood bottom sheet renders behind the empty-state card** — the Dashboard's camera icon visibly pokes through the sheet's transparent top. Z-index / scrim bug | `04-camera-tab.png` y≈1300 (camera icon visible inside sheet area) |
| 8 | **P1** | **Bottom-nav active-tab pill is purple in a green-themed app.** No green accent anywhere in the bottom nav | All Android screenshots |
| 9 | **P1** | **Empty state on History page reuses the Dashboard empty state verbatim** — same "No meals tracked yet / Tap to scan your first meal" copy, even though History needs different copy ("No history yet" or similar) | `09-history-clean.png` vs Dashboard's identical card |
| 10 | **P1** | **Bottom-nav structure diverges from iOS undocumented.** iOS has 4 items: Today / Log Food / Scan Menu / History (Settings is in top-right). Android has 4 items: Dashboard / Log Food / History / **Settings**. The Scan Menu entry point is buried | iOS `SS-3-Dashboard.png` bottom bar shows Scan Menu; Android shows Settings instead |

The Explore-agent deep code audit also found **20+ P2/P3 issues** (spacing, accessibility, padding, light-mode contrast, missing haptics, deferred edit/delete on History, etc.) — those are appended in §5 below.

---

## 1. Dashboard — side-by-side detail

### iOS reference (`SS-3-Dashboard.png`)
```
┌─────────────────────────────────────┐
│  🔥 Watch My Calories       (•••)  │   ← single header w/ menu
│       SATURDAY, APR 4               │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │   ◯    Goal       2000     │    │   ← Hero card: ring + 3 stats
│  │ 2,220  Burned      456     │    │     dots are color-distinct
│  │  kcal  Remaining   236     │    │     (green, orange, gray)
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ Protein  Carbs   Fat        │    │   ← Macro breakdown bar
│  │  123g     193g   102g       │    │     w/ % per macro
│  │ ▓░░░░    ▓▓░░░  ▓▓▓░░       │    │
│  └─────────────────────────────┘    │
│  [test ad banner]                   │
│  Breakfast              160 kcal    │
│  🍰 Latte               160 kcal    │
│  Lunch                  870 kcal    │
│  🍣 Lobster Sushi …     870 kcal    │
│  Dinner                1,190 kcal   │
├─────────────────────────────────────┤
│  Today  LogFood  ScanMenu  History  │
└─────────────────────────────────────┘
```

### Android current (`03-dashboard.png`)
```
┌─────────────────────────────────────┐
│  ▢ Watch My Calories         🔋⚡  │   ← 1st title (system bar)
├─────────────────────────────────────┤
│  Watch My Calories     ☰    ⚙       │   ← 2nd title (Scaffold TopAppBar)
├─────────────────────────────────────┤
│  🔥 Watch My Calories               │   ← 3rd title (greeting card)
│       Thursday, 28 May              │
│  ┌─────────────────────────────┐    │
│  │   ◯       Goal      2000   │    │   ← only 2 stats, no Burned row
│  │   0       Remaining 2000   │    │
│  │  kcal                       │    │
│  └─────────────────────────────┘    │
│           (no macro bars at all)    │
│  ┌─────────────────────────────┐    │
│  │                             │    │
│  │           📷                │    │
│  │   No meals tracked yet      │    │
│  │   Tap to scan first meal    │    │
│  │                             │    │
│  └─────────────────────────────┘    │
│         ✎ or log manually           │
├─────────────────────────────────────┤
│  Dashboard ⊕LogFood History Settings│   ← Settings tab, no Scan Menu
└─────────────────────────────────────┘
```

### Specific defects
| # | Defect | Root cause / file | Fix |
|---|---|---|---|
| D1 | Three "Watch My Calories" titles | `MainActivity.kt` Scaffold has a `TopAppBar` with the app title; `DashboardScreen.kt` ALSO has the in-content greeting card with the app name and flame icon | Remove the Scaffold TopAppBar entirely OR have it show only the current route name (Dashboard / History / Settings) without the brand. The in-content card should be reframed as a date-only greeting ("Thursday, 28 May") or removed |
| D2 | Missing Burned row | `SharedComponents.kt` HeroSummaryCard StatRow list — only renders Goal + Remaining, omits Burned even when Health Connect is unavailable (iOS shows it as "—") | Add Burned StatRow unconditionally; show value `0` or `—` if HC unavailable |
| D3 | Missing macro breakdown bars | iOS `Components.swift` `MacroBars` is missing from Android `SharedComponents.kt` and `DashboardScreen.kt` | Port MacroBars to Compose — three bars sized proportionally by gram count with % overlay |
| D4 | Empty-state card too tall | Camera empty-state card uses ~600dp of vertical space which is fine when there's no entries, but should shrink dramatically once any entry exists | Reduce empty state to ~200dp; only show full hero when truly empty |
| D5 | Bottom-nav order diverges | `MainActivity.kt` NavHost: Dashboard, Log Food (modal), History, Settings | Match iOS: Dashboard, Log Food (modal), Scan Menu (modal), History. Move Settings to a top-right gear in TopAppBar |
| D6 | Bottom-nav active-tab pill is purple | `Theme.kt` — Material 3 default secondary container color used by `NavigationBarItemDefaults.colors()` | Override `selectedIconColor`/`indicatorColor` to use `cwPrimary` (the iOS green) |
| D7 | "✎ or log manually" link styling | Inline text link at the bottom of the empty state; iOS uses a more prominent secondary button | Match iOS styling — secondary button with border |

---

## 2. History — side-by-side detail

### iOS reference (`SS-8-MealHistory.png`)
Day cards with **expanded macro bars + meal subsections**. Each card collapses/expands.

### Android current (`09-history-clean.png`)
- Same triple title problem
- Empty state is **identical to Dashboard's empty state** — same camera icon, same "Tap to scan first meal" copy
- No day cards yet (no entries), so the expanded interaction can't be verified
- History title at y≈580 in green is the only differentiator

### Defects
| # | Defect | Fix |
|---|---|---|
| D8 | Reuses Dashboard's empty state verbatim | `HistoryScreen.kt` should have its own empty state: copy "No history yet" + icon different from camera (e.g., calendar or chart icon) |
| D9 | Swipe-to-delete declared but unwired | `HistoryScreen.kt:29–42` has `@Suppress("UNUSED_PARAMETER")` on `onDeleteEntry` and `onEditEntry`. Implement `Material3 SwipeToDismissBox` |
| D10 | Day card expansion behavior not verifiable without data | Needs device test once an entry exists. Per code, expansion is wired but visual parity with iOS unconfirmed |

---

## 3. Settings — side-by-side detail

### iOS reference (`SS-10-Settings.png`)
- Modal sheet with **Cancel / Save** buttons at top (it's a modal in iOS!)
- Settings page is a **Form with grouped Sections**: App Appearance, Profile, Daily Goals
- Theme + Unit System are **iOS pickers** (current value on the right, chevron, expandable wheel)
- Profile rows are iOS-style: Height shows "5' 8" ⌄" picker, Weight "150 lbs ›", etc.
- Daily Goals section has "Calculate Recommended Goal" button

### Android current (`10-settings-clean.png`)
- Triple title issue
- "Test Ad" placeholder banner visibly rendered — `Nice job!  Test Ad  This is a 468x60 test ad`
- Theme: ✓System / Light / Dark as `SingleChoiceSegmentedButtonRow` (OK pattern but visually heavy)
- Unit System: US Customary / ✓Metric same segmented button (OK)
- Profile: sliders for Height (173 cm), Weight (68 kg), Age (30) — **major UX divergence**, sliders are imprecise vs iOS wheel pickers
- No "Daily Goals" section visible; truncated by viewport
- Settings is a full screen, not a modal sheet

### Defects
| # | Defect | Fix |
|---|---|---|
| D11 | Test Ad placeholder visible in production-ready UI | `SettingsScreen.kt` BannerAdView always shows test creative even on release builds. Either gate behind `BuildConfig.DEBUG` or remove until real ad unit ID is wired |
| D12 | Sliders instead of precise number entry for Height/Weight/Age | Either keep sliders (intentional UX divergence — document in `PORTING_DEVIATIONS.md`) or swap to numeric input + ± steppers to match iOS precision. Sliders make "173 cm exactly" require pixel-perfect drag |
| D13 | No "Calculate Recommended Goal" button | iOS computes BMR/TDEE on demand; Android only auto-computes. Add the button + show "Suggested: 2000 kcal" inline |
| D14 | Missing iOS-style Cancel/Save | iOS settings is a modal that requires explicit save. Android is "live edit" via DataStore. Acceptable divergence but should be documented |

---

## 4. LogFood bottom sheet — side-by-side detail

### iOS reference
The iOS LogFoodSheet is a clean white sheet over a dimmed Dashboard. Drag handle visible, three buttons (Scan Food / Choose from Library / Log Manually) with clear visual hierarchy.

### Android current (`15-logfood-sheet.png`)
- The sheet's transparent top causes the underlying Dashboard's empty-state camera icon to **visibly poke through** at y≈1340 — clearly a z-index / scrim issue
- Sheet content itself is OK: drag handle, "Log Food" green title, three options with icons and chevrons
- Triple title still present at top because the sheet doesn't cover the full screen

### Defects
| # | Defect | Fix |
|---|---|---|
| D15 | Sheet doesn't dim/occlude content behind it | `LogFoodSheet.kt` ModalBottomSheet should set `scrimColor = Color.Black.copy(alpha = 0.5f)` and ensure the sheet content has a solid (non-transparent) background. Currently the underlying card bleeds through |
| D16 | Sheet doesn't extend to full sheet height | The handle bar is at y≈1430 of 2424 (only 40% of screen height). The 3 options easily fit in less than what's shown but the sheet should expand to a sensible 60% height with proper scrim |

---

## 5. Cross-cutting issues (from code audit)

These are the systemic issues the Explore agent identified — they're real and pile on top of the visible defects above.

### 5.1 Spacing system is unraveled

**Symptom:** No single horizontal-inset constant; ad-hoc `16.dp`, `12.dp`, `8.dp`, `6.dp` scattered across files.

**Examples:**
- `cwCard()` modifier in `SharedComponents.kt:31–36` applies 16dp horizontal + 6dp vertical OUTSIDE the card, then 16dp INSIDE — visible double-padding bug
- `EmptyStateCard` in `SharedComponents.kt:318–322` applies `padding(horizontal = 16.dp)` then ALSO `padding(40.dp)` — left/right margin = 56dp instead of 40dp
- `FoodEntryCard` uses 16dp outer + 12dp inner = 28dp not 32dp

**Fix:** Define `val PageHorizontalInset = 16.dp` in `theme/Spacing.kt`. Audit every screen for double-padding. Refactor `cwCard()` to apply padding *inside* only; let LazyColumn `verticalArrangement` handle gaps between cards.

### 5.2 Color theme broken in light mode

**Symptom:** `StatRow` dot color uses `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)` which is medium-gray on white in light mode — fails WCAG AA contrast and makes Burned/Remaining indistinguishable.

**Files:** `SharedComponents.kt:155–170, 192–193`, `DashboardScreen.kt:71`, multiple usages.

**Fix:** Define semantic colors (`OnSurfaceSecondary`, `OnSurfaceTertiary`) in `theme/Color.kt`. Pass `cwPrimary` for Burned and `cwSecondary` for Remaining (matching iOS's distinct-color-per-row pattern).

### 5.3 Emoji icons instead of vectors

**Files using emoji where iOS uses SF Symbol vectors:**
- `SharedComponents.kt:332–334` — `📷` in EmptyStateCard
- `OnboardingScreen.kt:123` — `🔥`
- `DashboardScreen.kt:55` — emoji in header

**Risk:** Rendering inconsistency across Android versions/devices. Emoji can appear blurry, misaligned, or wrong-colored.

**Fix:** Replace with Material Icons (`Icons.Default.CameraAlt`, `Icons.Default.LocalFireDepartment`).

### 5.4 Camera-screen layout assumes iPhone aspect

**File:** `CameraScreen.kt:99`
```kotlin
.padding(bottom = 80.dp)
.size(72.dp)
```

On Pixel 9a (922dp tall), when the on-screen keyboard appears (~300dp), the capture button can clip beneath. Use `Modifier.imePadding()` and reduce hardcoded bottom-padding.

### 5.5 Edit/Delete declared but unwired

**File:** `HistoryScreen.kt:29–42`
```kotlin
@Suppress("UNUSED_PARAMETER")
@Composable
fun HistoryScreen(
    onDeleteEntry: ((String) -> Unit)? = null,
    onEditEntry: ((String) -> Unit)? = null,
    ...
)
```

iOS supports swipe-to-delete on history. Android declares the callbacks but the gesture is never wired. This is a Tier 2 (state parity) regression.

**Fix:** Wrap each HistoryDayCard in `Material3 SwipeToDismissBox` and route to `onDeleteEntry`.

### 5.6 Missing haptic feedback

iOS uses `UIImpactFeedbackGenerator(.heavy)` on capture button press. Android `CameraScreen.kt` has no haptic.

**Fix:** Add `view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)` on tap.

---

## 6. Pixel 9a–specific layout risks

Pixel 9a: **411dp × 922dp** (vs iPhone 14 Pro Max 430dp × 932dp). Slightly narrower, slightly shorter.

| Risk | File | Concern |
|---|---|---|
| Camera button under keyboard | `CameraScreen.kt:99` | `padding(bottom = 80.dp)` + soft keyboard at ~300dp = button at y≈540 covered by keyboard. Use `imePadding()` |
| Settings form overflow | `SettingsScreen.kt:115` | 8–10 sections in a single Column with `verticalScroll`. With sliders for Height/Weight/Age + segmented buttons + ads, the page is long. Verify Profile + Daily Goals don't require excessive scroll |
| Onboarding step 2 (profile) | `OnboardingScreen.kt:48–104` | All profile inputs + skip button + next button on 922dp − keyboard. Verify nothing clips |
| Settings segmented buttons | `SettingsScreen.kt` Theme/Unit selectors | `SingleChoiceSegmentedButtonRow` with 3 options on 411dp wide minus 32dp padding = 379dp available. "US Customary" label is wide; verify no truncation |
| LogFood sheet height | `LogFoodSheet.kt` | Currently sheet covers only ~40% of screen. Should expand to ~50–60% so 3 options aren't crammed |

---

## 7. Improvement opportunities (beyond parity)

| Opportunity | Why | Effort |
|---|---|---|
| **System-aware dynamic theme** | Pixel users expect Material You / dynamic color. Add `dynamicColor = true` option in Theme with a Settings toggle | M |
| **Edge-to-edge layout** | Currently the status bar background is set with a deprecated API (`Theme.kt:48`). Migrate to `WindowCompat.setDecorFitsSystemWindows(false)` + `Modifier.windowInsetsPadding()` | M |
| **Predictive back gesture** | Android 14+ supports predictive back; verify bottom sheet and modal screens respond to it | S |
| **Tablet/foldable layout** | Pixel Fold support — use `WindowSizeClass` to switch to a master/detail layout at ≥ 600dp width | L |
| **Camera haptics + sound** | Match iOS feel (light haptic on capture, optional shutter sound) | XS |
| **Swipe-to-delete on History** | Standard Material 3 pattern; also fixes the parity bug | S |
| **Material You-aligned typography** | Current `Type.kt` uses Material defaults; tune sizes to match iOS optical scale | S |

---

## 8. Phased correction plan

### Phase A — "stop looking broken" (one PR, ~1 day)
Goal: kill the screenshot-level embarrassments. Anyone opening the app should not immediately notice it's a buggy port.

1. **Remove duplicate "Watch My Calories" titles** (D1) — strip Scaffold TopAppBar or repurpose it to show route name only
2. **Fix LogFood sheet scrim/background** (D15) — set scrim alpha, ensure sheet content has solid surface
3. **Switch Android default theme to light** to match iOS (D5; fix in `SettingsDataStore.kt` default value)
4. **Hide test-ad placeholder until real ad unit wired** (D11) — gate behind `BuildConfig.DEBUG`
5. **Replace bottom-nav active-tab purple with `cwPrimary` green** (D6) — override `NavigationBarItemDefaults.colors`
6. **Distinct empty state for History** (D8) — separate copy + icon
7. **Replace emoji with Material Icons** in EmptyStateCard / Dashboard / Onboarding (5.3)

### Phase B — "Dashboard parity" (one PR, ~1 day)
Goal: Dashboard renders the iOS information density.

1. **Add Burned StatRow** to HeroSummaryCard (D2)
2. **Port MacroBars** component from iOS (D3) — three proportional bars with gram + percentage
3. **Fix StatRow dot colors** in light mode (5.2) — pass `cwPrimary` / `cwAccent` / `cwSecondary` explicitly
4. **Compact empty-state card** when Dashboard is empty (D4) — limit to ~200dp height

### Phase C — "Camera flow parity" (one PR, ~0.5 day)
1. **Add MealTypePicker UI to Camera flow** (P0 / #4) — insert between capture and analysis, matching `PhotoLibraryReviewScreen.kt`
2. **Use `Modifier.imePadding()`** on capture button (5.4)
3. **Add haptic feedback** on capture (5.6)

### Phase D — "Settings + History parity" (one PR, ~0.5 day)
1. **Add "Calculate Recommended Goal" button** + suggested-value display (D13)
2. **Decide: sliders vs steppers** for Height/Weight/Age (D12) — document choice in `PORTING_DEVIATIONS.md`
3. **Wire swipe-to-delete on History** (D9 / 5.5)
4. **Restructure bottom nav** to match iOS: Today / Log Food / Scan Menu / History; move Settings to top-right gear (D5/D10)

### Phase E — "Spacing rebuild" (one PR, ~0.5 day)
1. **Define `Spacing.kt`** with named constants
2. **Refactor `cwCard()`** to apply padding inside only
3. **Fix `EmptyStateCard` double-padding** (5.1)
4. **Audit all screens** for double-padding

### Phase F — "Improvements" (separate PRs)
Pick from §7 — Material You, edge-to-edge, predictive back, foldable layout, etc.

### Phase G — "Verification" (Pixel 9a device test)
Re-run `PIXEL_VERIFICATION_RUNBOOK.md` Steps 1–3 after Phase D. Step 4 (tampered-body T1.8) remains deferred until Play Console internal-testing distribution (per Stage 0.2 finding that Play Integrity requires Play-recognized builds).

### Effort total
~3 engineer-days for Phases A–E (close all P0/P1 + structural cleanup). Phase F is open-ended and dependent on appetite for going beyond iOS parity. Phase G is ~2 hours of device verification.

---

## 9. Recommended order of work

1. **Phase A first** — these are the issues a user would screenshot and share. Highest ratio of "embarrassment removed" per line of code changed.
2. **Phase B second** — the Dashboard is the screen users see most often and the iOS one is significantly richer.
3. **Phase C third** — camera meal-type picker is a *functional* parity gap, not just visual. Resume Stage 2 device verification only after this.
4. **Phase D fourth** — Settings + History close remaining Tier 2 gaps.
5. **Phase E fifth** — spacing cleanup is best done after the layouts are settled; doing it earlier means redoing it.
6. **Phase G** — verify on device, update `PORTING_MATRIX.md` honestly, then sign off.
7. **Phase F** — improvements as appetite allows. Could be a separate sprint.

---

## 10. Honest update to `PORTING_MATRIX.md`

The matrix currently claims:
> "Tier 1 = 100% pass, Tier 2 ≥ 95% pass"

That's not true. After this audit, the truth is:
- **Tier 1: ~85%** — three P0 functional/visual gaps (triple titles, missing macro bars + Burned row, missing camera meal-type picker)
- **Tier 2: ~60%** — visible spacing / theme / contrast issues, broken empty states, missing macro bars on dashboard

A more honest matrix entry would mark T1.1 (inventory), T1.6 (settings parity), and T2.1 (visual diff) as **❌ device** until Phases A–D land.

---

## Appendix — Full code-audit raw findings

The Explore agent's full report (179 lines, file:line citations) is captured in conversation. Key items already integrated above; remaining P2/P3 polish items:

- iOS uses `Picker(.wheel)` for profile values; Android uses `Slider`. Acceptable divergence but document.
- `AboutScreen.kt` text-fit at 411dp width unverified.
- Animated transitions in Onboarding (`AnimatedContent` vs iOS `TabView.page`) — both work, iOS feels smoother but acceptable.
- Material 3 segmented buttons in Settings work but need confirmation they don't overflow at 411dp with longest label ("US Customary").
- `SharedComponents.kt:225–228` macro labels use `Arrangement.SpaceEvenly` which iOS achieves via `Spacer()` — minor visual difference at edges.
- Dark-mode color tokens defined in `Color.kt` (e.g. `CwPrimaryDark = Color(0xFF66CC99)`) need perceptual verification against iOS dark mode.

These don't change the phasing — they roll into Phase E (spacing) and Phase F (polish).
