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
| D-005 | T2.x Multi-item meal grouping basis on Dashboard | Groups by `mealName` — all entries sharing the same `mealName` collapse under a single card titled with that name, with tap-to-expand to reveal individual items (`Mock Bento Box` → 3 sub-items). | `DashboardScreen.kt` groups by **`imageID`** instead. The seeded multi-item meal (mealName = `"Mock Bento Box"`, imageID = `null` on all 3 entries) renders as three independent cards under the LUNCH section — no group card, no expand affordance, no `mealName` shown anywhere on the dashboard. | The Android port adopted image-keyed grouping when the original assumption was that a single capture → one image → one meal name. Decoupling `mealName` from `imageID` (needed for manual-entry meals and edited group names) was not retro-fitted to the dashboard's grouping logic. Discovered 2026-05-31 while writing Android parity mirrors for `DashboardTests.testMultiItemMealGroup*`. Either align Android to group by `mealName` (matches iOS data semantics) or document a UI gap. Leaning toward fix — the iOS group card is a real UX feature, not just a cosmetic difference. | agent | _pending review_ |
| D-006 | T2.x Dashboard meal-section header case | Title case: `"Breakfast"`, `"Lunch"`, `"Dinner"`, `"Snack"` — uses `MealType.displayName` as-is. | UPPERCASE: `"BREAKFAST"`, `"LUNCH"`, `"DINNER"`, `"SNACK"` — `DashboardScreen.kt:163` applies `.uppercase()` to the display name. | Material `labelSmall` typography conventionally pairs with uppercase glyphs and increased letter-spacing for section markers. Both platforms expose the same underlying `MealType` data; only the visual treatment differs. No functional impact, no data loss. Discovered 2026-05-31 while writing Dashboard parity mirrors. Acceptable as-is; tests assert on the per-platform rendering. | agent | _pending review_ |
| D-007 | T2.x History per-entry macro display | Proportional macro bar — protein / carbs / fat as colored bar segments, no literal gram text on the entry row. | Literal text chips — `HistoryScreen.MacroChip` (L391) renders `"$label: ${grams.toInt()}g"` so each entry row shows `"P: 10g"`, `"C: 50g"`, `"F: 6g"` strings. | Functional information is the same (gram values for P/C/F) but visual treatment differs significantly: iOS emphasizes relative composition, Android emphasizes absolute values. Discovered 2026-05-31 while writing History parity mirrors; the Android test `testExpandedMealCardShowsLiteralGramLabelsOnAndroid` pins the current state so a future convergence is auditable. Worth revisiting before public release — the iOS bar is more scannable for "is this a balanced meal?" assessments. | agent | _pending review_ |
| D-008 | T2.x EstimationReview success — total-kcal summary | Success screen shows a `"Logged Successfully!"` headline + a `"Total Added"` row with the rolled-up kcal total. | `AnalysisScreen.kt` success path renders a `"Review & Edit"` heading + per-item OutlinedTextFields, no rolled-up total. The Android user reviews/edits each item before tapping Save. | Two different mental models: iOS treats success as a confirmation (you already saved, here's what got added). Android treats success as a review-and-edit step (Save commits after edits). Discovered 2026-05-31 writing `EstimationReviewParityTest`. Worth aligning — the iOS confirmation reads better post-save, while Android's editability is useful pre-save. Either platform can adopt the other's shape without changing data semantics. | agent | _pending review_ |
| D-009 | T2.x EstimationReview error / no-food — escape buttons | Both error and no-food states expose **Try Again** + **Cancel** buttons; Cancel returns the user to the camera (or dashboard). | Android error and no-food views expose **only Try Again** (`AnalysisScreen.kt:153` and `:181`). The system back gesture is the only Cancel path. | Discovered 2026-05-31 writing `EstimationReviewParityTest`. iOS's explicit Cancel is friendlier on long error states; Android's back-gesture-only reliance is consistent with platform norms but reduces discoverability. Add a Cancel button on both views to close the gap, or document as accepted divergence. | agent | _pending review_ |
| D-010 | T2.x ScannedMenus list mutation surface | `ScannedMenusView` exposes a **toolbar Edit/Done toggle** + **swipe-to-delete** on each row (standard iOS list editing). Deletion via swipe is the primary affordance. | `ScannedMenusScreen.kt` has neither — rows are tap-to-detail only; deletion happens only inside `MenuScanDetailScreen` via the trash icon → confirmation dialog. | Two different mutation idioms: iOS expects in-place list editing, Android routes mutation through the detail screen. Both reach the same end state but iOS's flow is 2 taps shorter for power users with many menus. Discovered 2026-05-31 writing `ScanMenuParityTest`. Worth aligning before public release; until then the mirror tests assert the Android shape and skip iOS's edit-mode / swipe assertions. | agent | _pending review_ |

---

## Resolved (no longer divergent)

When a deviation is closed (Android brought into parity with iOS, or vice versa), move the row here with the resolution date and the commit SHA that closed it.

| ID | Surface | Resolution | Closed by SHA | Date |
|----|---------|------------|---------------|------|
| (none yet) | | | | |

---

## Template

```
| D-NNN | T1.x (criterion) | <what iOS does> | <what Android does> | <why this is acceptable; cost of parity vs. value> | <author> | <reviewer initials YYYY-MM-DD> |
```

Use a monotonically increasing `D-NNN` ID. Once issued, an ID is not reused even if the row moves to Resolved.
