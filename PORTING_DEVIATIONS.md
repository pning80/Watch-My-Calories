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
| D-004 | T1.6 Settings profile inputs (Height / Weight / Age) | Wheel pickers (`Picker(.wheel)`) — one discrete value visible at a time, scrolled via the tumbler. | Material 3 `Slider` widgets in `SettingsScreen.kt` (`ProfileSliderRow`) — continuous-feel drag, current value rendered next to the label. | Material guidelines on Android push toward Sliders for bounded continuous values like body metrics; the iOS wheel picker has no first-class Material equivalent and a faux-wheel composable would feel non-native. The semantic surface (clamped integer min/max, single-value selection, metric/imperial unit switch via Settings) is identical on both sides — only the gesture and visual differ. Recorded after PORT_AUDIT.md flagged the choice as "acceptable divergence but document." | agent | PN 2026-05-31 (accept as Material norm) |
| D-006 | T2.x Dashboard meal-section header case | Title case: `"Breakfast"`, `"Lunch"`, `"Dinner"`, `"Snack"` — uses `MealType.displayName` as-is. | UPPERCASE: `"BREAKFAST"`, `"LUNCH"`, `"DINNER"`, `"SNACK"` — `DashboardScreen.kt:163` applies `.uppercase()` to the display name. | Material `labelSmall` typography conventionally pairs with uppercase glyphs and increased letter-spacing for section markers. Both platforms expose the same underlying `MealType` data; only the visual treatment differs. No functional impact, no data loss. Discovered 2026-05-31 while writing Dashboard parity mirrors. Acceptable as-is; tests assert on the per-platform rendering. | agent | PN 2026-05-31 (accept as Material norm) |
| D-011 | T2.x Dark-theme palette | iOS uses two-tone dark (`systemBackground` + `secondarySystemBackground`) with system-managed material layering that softens pure black. The brand mint `#66CC99`, accent `#FF9E1C`, and forest secondary `#264D33` are shared light/dark. | Android dark palette diverges intentionally per Material 3 dark-theme guidance: (a) `background = #0F1411` (soft near-black with green tint, not OLED `#000`) so shadows render and cards lift; (b) `surfaceTint = #66CC99` overrides M3's default purple so all elevation overlays carry brand color; (c) full `surfaceContainer*` slots defined so Cards/Sheets/Menus pick brand greens instead of M3 defaults; (d) `primary` desaturated `#66CC99 → #7FD9A8` ("use lighter, less saturated tones" — Material); (e) `tertiary` softened `#FF9E1C → #FFB95A`; (f) `secondary` flipped from `#264D33` (a too-dark forest, was a light-theme container value reused in dark mode) to `#B8D5C2` (pale sage) so badges read against dark surfaces. Light palette unchanged. | agent | PN 2026-05-31 (intentional M3 improvement) |
| D-012 | T2.x History banner-ad scroll behavior | iOS `HistoryView` keeps `BannerAdView()` inside the ScrollView's VStack — the banner scrolls with content and disappears off the top as the user scrolls through days. | Android `HistoryScreen.kt` places `BannerAdView()` outside the `LazyColumn`, between the `TopAppBar` and the day-card list — the banner stays pinned below the top bar while day cards scroll independently underneath it. | Material idiom favors persistent monetization surfaces above scrolling content; SwiftUI's habit of inlining ads into the scroll list isn't conventional on Android. Banner ad still loads/renders identically; only its scroll-coupling differs. Discovered 2026-05-31 during PR #29 review. | agent | PN 2026-05-31 (intentional Material idiom) |

---

## Resolved (no longer divergent)

When a deviation is closed (Android brought into parity with iOS, or vice versa), move the row here with the resolution date and the commit SHA that closed it.

| ID | Surface | Resolution | Closed by SHA | Date |
|----|---------|------------|---------------|------|
| D-002 | T1.1 menu-scan entry point | New `ui/menuscanner/ScanMenuSheet.kt` mirrors iOS's 3-option modal exactly: **Scan with Camera**, **Choose from Library**, **Stored Menus**. Scan Menu tab tap now opens the sheet (matches the Log Food tab's existing pattern). The 3 iOS `AccessibilityID.ScanMenuSheet.*` IDs are wired. | (PR #22) | 2026-05-31 |
| D-003 | T1.1 camera capture flow | `CameraScreen.kt` gains a `CaptureMode { Food, Menu }` flag. Food mode keeps the existing multi-photo bundle UX. Menu mode is single-shot (routes immediately on first capture) with an "Align the menu within the frame" OCR-hint overlay. Lighter than porting a sibling `MenuCameraView` while reaching the same capability. | (PR #22) | 2026-05-31 |
| D-005 | T2.x Multi-item meal grouping basis | New shared `ui/components/MealGrouping.kt` (`groupEntriesByMealOrImage`) groups consecutive entries by `mealName` first, then `imageID`. Dashboard `MealGroupItem` and History `MealGroupCard` both use it, render the meal's `mealName` as the card title, and toggle expand-to-show-items on tap (chevron rotates). | (PR #16) | 2026-05-31 |
| D-007 | T2.x History per-entry macro display | `HistoryScreen` expanded meal entry now uses `MacroProportionalBar` (already in `SharedComponents.kt`) — no literal `"P: 10g"` gram-label text on the entry row. Matches iOS. Day-card header retains the gram chips since iOS does too. | (PR #15) | 2026-05-31 |
| D-010 | T2.x ScannedMenus list mutation surface | `ScannedMenusScreen.kt` now wraps each row in a `SwipeToDismissBox` (Material idiom — same pattern as `HistoryScreen.SwipeToDeleteRow`). Swiping left commits `onDeleteScan` which calls `viewModel.deleteMenuScan(id)`. The iOS Edit/Done toolbar toggle is intentionally skipped — Material doesn't use that pattern; swipe-to-delete is the canonical Android gesture and is always-on, not gated behind an edit-mode toggle. | (PR #18) | 2026-05-31 |
| D-008 | T2.x EstimationReview success — total-kcal summary | `AnalysisScreen.kt` now shows a post-save confirmation screen with `"Logged Successfully!"` + `"Total Added"` + total kcal value + Done button — matches iOS exactly. The pre-save Review & Edit step is preserved as an Android extra (gives users the ability to correct AI estimates before logging). | (PR #14) | 2026-05-31 |
| D-009 | T2.x EstimationReview error / no-food — escape buttons | Both views now expose an explicit Cancel button next to Try Again — matches iOS. The Try Again button on error now also retries the estimation (via a `retryTrigger` counter that re-fires the `LaunchedEffect`); on no-food it returns to the previous screen. | (PR #14) | 2026-05-31 |
| D-013 | T2.x Interstitial ads | Removed the Android-only interstitial flow entirely — `AdManager` no longer carries `loadInterstitial`/`showInterstitialIfReady`/`interstitialAd` state, the `ManualEntryScreen.onSave` call site is gone, and `ADMOB_INTERSTITIAL_ID` was dropped from BuildConfig and `Ads/AdMob-Android.properties`. Android now matches iOS at banner + native only. | (PR #33) | 2026-05-31 |

---

## Template

```
| D-NNN | T1.x (criterion) | <what iOS does> | <what Android does> | <why this is acceptable; cost of parity vs. value> | <author> | <reviewer initials YYYY-MM-DD> |
```

Use a monotonically increasing `D-NNN` ID. Once issued, an ID is not reused even if the row moves to Resolved.
