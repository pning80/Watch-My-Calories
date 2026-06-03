# Porting Deviation Registry

Every documented divergence between the iOS app and the Android port. Per `PORTING_CRITERIA.md` Operating Principle 1, an Android divergence is either:
1. **Listed here** with iOS behavior, Android behavior, rationale, and sign-off — meaning it is an intentional, accepted departure, **OR**
2. **Not listed** — meaning it is a defect, treated as such at review.

No undocumented divergences are permitted to ship.

---

## Sign-off process

A row is "signed off" once the reviewer initials the **Sign-off** column with a date. Before sign-off, the row is a *proposal* — it does not grant ship permission. Reviewers who sign off accept responsibility that the rationale holds; revisiting requires a new row (do not edit a signed-off row's rationale).

---

## Active deviations

| ID | Surface / Criterion | iOS behavior | Android behavior | Rationale | Proposed by | Sign-off |
|----|---------------------|--------------|------------------|-----------|-------------|----------|
| D-001 | T1.2 `UserProfile` shape | SwiftData `@Model` class with no explicit `id` property; singleton invariant enforced by application logic (only one instance is ever inserted). | Room `@Entity` requires a `@PrimaryKey`; uses `val id: Int = 1` and the application always reads/writes that single row. | Room does not permit `@Entity` types without a primary key. The Android `id` is an internal storage detail; semantically identical singleton on both platforms. | agent | PN 2026-05-31 (accept as Room engine requirement) |
| D-012 | T2.x History banner-ad scroll behavior | iOS `HistoryView` keeps `BannerAdView()` inside the ScrollView's VStack — the banner scrolls with content and disappears off the top as the user scrolls through days. | Android `HistoryScreen.kt` places `BannerAdView()` outside the `LazyColumn`, between the `TopAppBar` and the day-card list — the banner stays pinned below the top bar while day cards scroll independently underneath it. | Material idiom favors persistent monetization surfaces above scrolling content; SwiftUI's habit of inlining ads into the scroll list isn't conventional on Android. Banner ad still loads/renders identically; only its scroll-coupling differs. Discovered 2026-05-31 during PR #29 review. Reaffirmed 2026-06-02 (kept; coherent with the pinned `TopAppBar`). | agent | PN 2026-05-31 (intentional Material idiom) |
| D-015 | T2.x Dashboard empty-state subtitle copy | "Tap the camera tab to scan your first meal." (`Components.swift:102`) | "Tap to scan your first meal." (`SharedComponents.kt` `EmptyStateCard` default). | iOS's copy refers to a "camera tab" that does not exist in either app's navigation (tabs are Today / Log Food / Scan Menu / History on Android; the iOS equivalent has no tab literally named "camera"). Android's shorter copy is accurate; matching iOS verbatim would propagate a stale, misleading instruction. Intentionally kept; flagged with that opinion rather than blindly aligned. Discovered iter-2 (2026-06-02). | agent | PN 2026-06-02 (keep Android; iOS copy is the defect) |
| D-014 | T2.x Dashboard "Remaining" stat badge fill (dark only) | iOS draws the Remaining badge in `cwSecondary` = forest `#264D33` (dark) with a `cwPrimary` mint icon (`Components.swift:208`, `StatRow:430`). | `SharedComponents.kt` draws the Remaining badge in pale sage `#B8D5C2` (`CwRemainingBadgeDark`) in dark mode only, mint icon unchanged; light mode uses `colorScheme.secondary` (iOS's pale `#D9F2DB`) and matches iOS exactly. | On the true-black dark surface a forest `#264D33` circle nearly vanishes, so the badge reads as no chip at all. The pale sage reads as a distinct chip while keeping the mint icon legible. This is the sole surviving piece of the former D-011 sage-secondary improvement — now scoped to this one badge so it no longer leaks into the ring track, kcal pill, avatar tile, or empty-state glyph (all of which converged to iOS forest). | agent | PN 2026-06-02 (decouple: forest token + badge-only sage) |

---

## Resolved (no longer divergent)

When a deviation is closed (Android brought into parity with iOS, or vice versa), move the row here with the resolution date and the commit SHA that closed it.

| ID | Surface | Resolution | Closed by SHA | Date |
|----|---------|------------|---------------|------|
| D-004 | T1.6 Settings profile inputs (Height / Weight / Age) | Metric Height, Weight (both units), and Age are now collapsible disclosure rows that expand to a platform `NumberPicker` tumbler wheel (`ProfileWheelRow` / `WheelNumberPicker` in `SettingsScreen.kt`), mirroring iOS's `DisclosureGroup` + `.wheel` (one open at a time, value muted when collapsed / accent when open). US Height keeps the inline ft/in dropdowns — iOS's US-mode height uses `.menu` pickers, not wheels (`SettingsView.swift:79-95`), so the dropdowns already matched. Wheel gives exact integer selection (the Slider could not reliably hit a specific lbs/kg across 180-350 units) and removes the metric-slider vs imperial-dropdown internal inconsistency. The `NumberPicker` is wrapped in a DayNight `ContextThemeWrapper` so its text follows light/dark. | (PR #56) | 2026-06-02 |
| D-006 | T2.x Dashboard meal-section header case | Header now mirrors iOS `MealSection` (`Components.swift:816-819`): title-case `displayName`, `titleLarge` bold, `onSurface` (cwTextPrimary). Dropped the former tiny uppercase green `labelSmall`. The earlier sign-off covered only the case; the undocumented `labelSmall` size + green color were the larger hierarchy divergence — all three resolved together. | (PR #55) | 2026-06-02 |
| D-011 | T2.x Dark-theme palette | Dark `secondary` converged to iOS forest `#264D33` (was pale sage `#B8D5C2`). The token now matches iOS on every shared surface — hero ring track, kcal pill, meal/avatar tile, dashboard empty-state glyph — and the two latent misuses it had masked were rerouted (group-card title `secondary`→`onSurface` in Dashboard + History; the avatar letter →`primary`/mint, matching iOS `cwPrimary`-on-`cwSecondary`). The badge-readability tweak that originally justified the pale sage was re-scoped to the new badge-only **D-014**. Light palette unchanged (already a match). | (PR #54) | 2026-06-02 |
| D-002 | T1.1 menu-scan entry point | New `ui/menuscanner/ScanMenuSheet.kt` mirrors iOS's 3-option modal exactly: **Scan with Camera**, **Choose from Library**, **Stored Menus**. Scan Menu tab tap now opens the sheet (matches the Log Food tab's existing pattern). The 3 iOS `AccessibilityID.ScanMenuSheet.*` IDs are wired. | (PR #22) | 2026-05-31 |
| D-003 | T1.1 camera capture flow | `CameraScreen.kt` gains a `CaptureMode { Food, Menu }` flag. Food mode keeps the existing multi-photo bundle UX. Menu mode is single-shot (routes immediately on first capture) with an "Align the menu within the frame" OCR-hint overlay. Lighter than porting a sibling `MenuCameraView` while reaching the same capability. | (PR #22) | 2026-05-31 |
| D-005 | T2.x Multi-item meal grouping basis | New shared `ui/components/MealGrouping.kt` (`groupEntriesByMealOrImage`) groups consecutive entries by `mealName` first, then `imageID`. Dashboard `MealGroupItem` and History `MealGroupCard` both use it, render the meal's `mealName` as the card title, and toggle expand-to-show-items on tap (chevron rotates). | (PR #16) | 2026-05-31 |
| D-007 | T2.x History per-entry macro display | `HistoryScreen` expanded meal entry now uses `MacroProportionalBar` (already in `SharedComponents.kt`) — no literal `"P: 10g"` gram-label text on the entry row. Matches iOS. Day-card header retains the gram chips since iOS does too. | (PR #15) | 2026-05-31 |
| D-010 | T2.x ScannedMenus list mutation surface | `ScannedMenusScreen.kt` now wraps each row in a `SwipeToDismissBox` (Material idiom — same pattern as `HistoryScreen.SwipeToDeleteRow`). Swiping left commits `onDeleteScan` which calls `viewModel.deleteMenuScan(id)`. The iOS Edit/Done toolbar toggle is intentionally skipped — Material doesn't use that pattern; swipe-to-delete is the canonical Android gesture and is always-on, not gated behind an edit-mode toggle. | (PR #18) | 2026-05-31 |
| D-008 | T2.x EstimationReview success screen | iOS `EstimationReviewView` auto-saves on a successful estimation and shows a read-only summary: checkmark + `"Logged Successfully!"` + a card per item (name + kcal + macro row) + a `"Total Added"` macro-breakdown card, with a compact centered `Done` and the nav bar hidden (no title). | `AnalysisScreen.kt` now mirrors iOS exactly (revised 2026-06-02): success auto-saves via `onSaveLog` and renders the same read-only summary (item cards + `MacroBreakdownRow` total card), no "Analysis" TopAppBar, compact centered Done. **The earlier Android-only pre-save Review & Edit step was removed** for parity — editing AI estimates is still available later from History via `EditMealGroupScreen` (which retains `EditableEstimationItem`). | agent | PN 2026-06-02 (revised: drop Review & Edit, converge to iOS) |
| D-009 | T2.x EstimationReview error / no-food — escape buttons | Both views now expose an explicit Cancel button next to Try Again — matches iOS. The Try Again button on error now also retries the estimation (via a `retryTrigger` counter that re-fires the `LaunchedEffect`); on no-food it returns to the previous screen. | (PR #14) | 2026-05-31 |
| D-013 | T2.x Interstitial ads | Removed the Android-only interstitial flow entirely — `AdManager` no longer carries `loadInterstitial`/`showInterstitialIfReady`/`interstitialAd` state, the `ManualEntryScreen.onSave` call site is gone, and `ADMOB_INTERSTITIAL_ID` was dropped from BuildConfig and `Ads/AdMob-Android.properties`. Android now matches iOS at banner + native only. | (PR #33) | 2026-05-31 |

---

## Template

```
| D-NNN | T1.x (criterion) | <what iOS does> | <what Android does> | <why this is acceptable; cost of parity vs. value> | <author> | <reviewer initials YYYY-MM-DD> |
```

Use a monotonically increasing `D-NNN` ID. Once issued, an ID is not reused even if the row moves to Resolved.
