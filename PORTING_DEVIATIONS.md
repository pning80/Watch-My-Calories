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
| D-001 | T1.2 `UserProfile` shape | SwiftData `@Model` class with no explicit `id` property; singleton invariant enforced by application logic (only one instance is ever inserted). | Room `@Entity` requires a `@PrimaryKey`; uses `val id: Int = 1` and the application always reads/writes that single row. | Room does not permit `@Entity` types without a primary key. The Android `id` is an internal storage detail; semantically identical singleton on both platforms. | agent | _pending review_ |
| D-002 | T1.1 menu-scan entry point | Tapping a menu-scan affordance presents `ScanMenuSheet` (modal bottom sheet) with three actions: **Scan** (open in-app camera), **Choose from Library**, **Stored Menus**. | The `Scan Menu` bottom-nav tab (added in Phase D) navigates directly to `ScannedMenusScreen`. That screen has a single FAB which opens only the photo-library picker — there is **no in-app camera capture for menus**, and there is no consolidated sheet mirroring iOS's three actions. | The Android port treats menu scanning as a lower-frequency flow than food logging and routes through the photo picker only. Adding an in-app camera capture for menus would require either reusing `CameraScreen` (currently food-only) with a mode flag, or porting iOS `MenuCameraView` as a sibling screen (see D-003). Worth revisiting before public release if usage data shows users wanting it. The three iOS `AccessibilityID.ScanMenuSheet.*` IDs (`scanMenuSheet_scan`, `chooseFromLibrary`, `storedMenus`) remain in Android's `AccessibilityTags.ScanMenuSheet` registry as a future hook. | agent | _pending review_ |
| D-003 | T1.1 camera capture flow | Two separate camera screens: `CameraView.swift` for food capture (multi-photo bundling) and `MenuCameraView.swift` for menu capture (single photo, OCR-friendly framing). | Single `ui/camera/CameraScreen.kt` used for food capture only. Menu capture is library-only per D-002. | Splitting iOS's two camera screens reflects different capture UX (food bundles vs single-frame menu). Android has not yet ported the menu camera; consolidating to one CameraScreen avoids dead-weight code while D-002 is open. If D-002 is closed (menu camera added back), this row should be revisited — either rename it to "single-camera architecture" (one component with a mode arg) or close it by porting `MenuCameraView` as a separate screen. | agent | _pending review_ |
| D-004 | T1.6 Settings profile inputs (Height / Weight / Age) | Wheel pickers (`Picker(.wheel)`) — one discrete value visible at a time, scrolled via the tumbler. | Material 3 `Slider` widgets in `SettingsScreen.kt` (`ProfileSliderRow`) — continuous-feel drag, current value rendered next to the label. | Material guidelines on Android push toward Sliders for bounded continuous values like body metrics; the iOS wheel picker has no first-class Material equivalent and a faux-wheel composable would feel non-native. The semantic surface (clamped integer min/max, single-value selection, metric/imperial unit switch via Settings) is identical on both sides — only the gesture and visual differ. Recorded after PORT_AUDIT.md flagged the choice as "acceptable divergence but document." | agent | _pending review_ |
| D-006 | T2.x Dashboard meal-section header case | Title case: `"Breakfast"`, `"Lunch"`, `"Dinner"`, `"Snack"` — uses `MealType.displayName` as-is. | UPPERCASE: `"BREAKFAST"`, `"LUNCH"`, `"DINNER"`, `"SNACK"` — `DashboardScreen.kt:163` applies `.uppercase()` to the display name. | Material `labelSmall` typography conventionally pairs with uppercase glyphs and increased letter-spacing for section markers. Both platforms expose the same underlying `MealType` data; only the visual treatment differs. No functional impact, no data loss. Discovered 2026-05-31 while writing Dashboard parity mirrors. Acceptable as-is; tests assert on the per-platform rendering. | agent | _pending review_ |

---

## Resolved (no longer divergent)

When a deviation is closed (Android brought into parity with iOS, or vice versa), move the row here with the resolution date and the commit SHA that closed it.

| ID | Surface | Resolution | Closed by SHA | Date |
|----|---------|------------|---------------|------|
| D-005 | T2.x Multi-item meal grouping basis | New shared `ui/components/MealGrouping.kt` (`groupEntriesByMealOrImage`) groups consecutive entries by `mealName` first, then `imageID`. Dashboard `MealGroupItem` and History `MealGroupCard` both use it, render the meal's `mealName` as the card title, and toggle expand-to-show-items on tap (chevron rotates). | (PR #16) | 2026-05-31 |
| D-007 | T2.x History per-entry macro display | `HistoryScreen` expanded meal entry now uses `MacroProportionalBar` (already in `SharedComponents.kt`) — no literal `"P: 10g"` gram-label text on the entry row. Matches iOS. Day-card header retains the gram chips since iOS does too. | (PR #15) | 2026-05-31 |
| D-010 | T2.x ScannedMenus list mutation surface | `ScannedMenusScreen.kt` now wraps each row in a `SwipeToDismissBox` (Material idiom — same pattern as `HistoryScreen.SwipeToDeleteRow`). Swiping left commits `onDeleteScan` which calls `viewModel.deleteMenuScan(id)`. The iOS Edit/Done toolbar toggle is intentionally skipped — Material doesn't use that pattern; swipe-to-delete is the canonical Android gesture and is always-on, not gated behind an edit-mode toggle. | (PR #18) | 2026-05-31 |
| D-008 | T2.x EstimationReview success — total-kcal summary | `AnalysisScreen.kt` now shows a post-save confirmation screen with `"Logged Successfully!"` + `"Total Added"` + total kcal value + Done button — matches iOS exactly. The pre-save Review & Edit step is preserved as an Android extra (gives users the ability to correct AI estimates before logging). | (PR #14) | 2026-05-31 |
| D-009 | T2.x EstimationReview error / no-food — escape buttons | Both views now expose an explicit Cancel button next to Try Again — matches iOS. The Try Again button on error now also retries the estimation (via a `retryTrigger` counter that re-fires the `LaunchedEffect`); on no-food it returns to the previous screen. | (PR #14) | 2026-05-31 |

---

## Template

```
| D-NNN | T1.x (criterion) | <what iOS does> | <what Android does> | <why this is acceptable; cost of parity vs. value> | <author> | <reviewer initials YYYY-MM-DD> |
```

Use a monotonically increasing `D-NNN` ID. Once issued, an ID is not reused even if the row moves to Resolved.
