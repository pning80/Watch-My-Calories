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
| D-002 | T1.1 menu-scan entry point | Tapping a menu-scan affordance presents `ScanMenuSheet` (modal bottom sheet) with three actions: **Scan** (open in-app camera), **Choose from Library**, **Stored Menus**. | Overflow menu in the `TopAppBar` carries a "Scanned Menus" item that navigates to `ScannedMenusScreen`. That screen has a single FAB which opens only the photo-library picker — there is **no in-app camera capture for menus**, and there is no consolidated sheet mirroring iOS's three actions. | The Android port treats menu scanning as a lower-frequency flow than food logging and routes through the photo picker only. Adding an in-app camera capture for menus would require either reusing `CameraScreen` (currently food-only) with a mode flag, or porting iOS `MenuCameraView` as a sibling screen (see D-003). Worth revisiting before public release if usage data shows users wanting it. The three iOS `AccessibilityID.ScanMenuSheet.*` IDs (`scanMenuSheet_scan`, `chooseFromLibrary`, `storedMenus`) remain in Android's `AccessibilityTags.ScanMenuSheet` registry as a future hook. | agent | _pending review_ |
| D-003 | T1.1 camera capture flow | Two separate camera screens: `CameraView.swift` for food capture (multi-photo bundling) and `MenuCameraView.swift` for menu capture (single photo, OCR-friendly framing). | Single `ui/camera/CameraScreen.kt` used for food capture only. Menu capture is library-only per D-002. | Splitting iOS's two camera screens reflects different capture UX (food bundles vs single-frame menu). Android has not yet ported the menu camera; consolidating to one CameraScreen avoids dead-weight code while D-002 is open. If D-002 is closed (menu camera added back), this row should be revisited — either rename it to "single-camera architecture" (one component with a mode arg) or close it by porting `MenuCameraView` as a separate screen. | agent | _pending review_ |

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
