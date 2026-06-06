# Porting Completion Report — iOS → Android UI Parity

**Status: COMPLETE.** As of 2026-06-05 (parity-loop iter-91), every screen, sub-state,
mock-able mode, and ring edge case has been pixel-diffed against iOS in **both** dark and
light themes. All genuine divergences are fixed and merged; the only open items are
deliberate, documented deviations awaiting reviewer sign-off (below).

The audit ran as a repeating iOS→Android loop (iters 39–91), landing PRs **#59–#121** via the
iOS XCUITest `ParitySnapshotTests` harness (the spec side) vs the Pixel 9a (the port). iOS app
+ backend wire contract stayed frozen throughout; the sole iOS exception was test-only harness
extensions.

## Coverage (all pixel-diffed, both themes)

- **Dashboard** — seeded, empty, multi-item, expanded; hero two-ring (consumed + burned),
  incl. the 0-consumed burned-only ring edge case.
- **History** — list, empty, day-expanded (meal sections).
- **Settings** — appearance/profile/goal/privacy; Theme/Unit/Gender/Activity menu pickers; wheels.
- **Analysis** — success ("Logged"), error, no-food.
- **Scan Menu** — sheet, Stored/Scanned list, saved Menu-Scan detail; live Menu Analysis
  result + error + not-a-menu.
- **Onboarding** — Welcome, Privacy, Goal.
- **About**, **Log Food sheet**, **Manual Entry**, **Edit Food** (single), **Edit Meal Group**,
  **Camera Review**, **Photo-Library Review** (source-audited; iOS sim-blocked), **Calorie
  Disclaimer**.
- In-app brand mark (now the iOS MiniAppIcon), app-menu overflow (ellipsis.circle), list-row
  chevron (unified ChevronRight), menu-scan date format.

## Deviation registry (`PORTING_DEVIATIONS.md`)

- **Signed-off (PN):** D-001..D-017 (the early structural/idiom set).
- **Fully resolved by fixes:** D-025 (chevron glyph, iter-81); plus the genuine-divergence
  parts of D-018 (segmented/nutrition idioms, iter-75), D-029 (Settings menu pickers, iter-70),
  D-032 (ScannedMenus glyph/chevron iter-82 + date format iter-84), D-033 (MenuAnalysis macro
  columns iter-88 + not-a-menu tint iter-89), D-019 (segmented/Total/nutrition/field-order
  iters-79/80/85).

### Accepted — signed off `PN 2026-06-06` (was proposed)

All 15 below were reviewed one-by-one and **accepted** on 2026-06-06; each row in
`PORTING_DEVIATIONS.md` now carries `PN 2026-06-06 (accepted)`. They are deliberate
keep-Android decisions with iOS-vs-Android detail + rationale on each row. Most fall into
three established classes:
**iter-10 header** (Material TopAppBar vs iOS sheet/centered-title), **Form-vs-Material**
(discrete Cards/OutlinedFields vs iOS Form rows), and **incidental-iOS-blue** (Android on-brand
green vs iOS un-tinted blue).

| Row | Screen | Class |
|-----|--------|-------|
| D-018 | Single-entry edit — no "Group Title" field; shared add/edit form | Form/idiom |
| D-019 | Edit Meal Group — full-screen + bottom button + "Food Name" label | iter-10 header |
| D-020 | "Calculate Recommended Goal" filled button vs iOS text-link | Form-vs-Material |
| D-021 | ScannedMenus empty-state container (shared EmptyStateCard) | Form-vs-Material |
| D-022 | History empty-state copy + book icon (Android clearer) | copy/idiom |
| D-023 | Dashboard entry: tap-to-edit + Edit/Delete menu (no "View") | interaction |
| D-024 | History list mutation (swipe-to-delete + menu) | Material idiom |
| D-026 | Onboarding "Connect Health" control (OutlinedButton, green) | Form-vs-Material |
| D-027 | In-app mark = iOS art; launcher icon stays platform-specific | brand asset |
| D-028 | About — full-screen header + discrete ListItem cards | iter-10 + Form |
| D-029 | Settings Height-US/Target-Calories boxed inputs | Form-vs-Material |
| D-030 | Analysis error/no-food — consistent Material treatment | idiom (iOS inconsistent) |
| D-031 | Saved Menu-Scan detail header (TopAppBar + left-bold name) | iter-10 header |
| D-032 | ScannedMenus header + Form-rows-vs-Cards (residual) | iter-10 + Form |
| D-033 | Live Menu Analysis header/name + Scan-Again button (residual) | iter-10 + Form |

To accept a row, append `| PN <YYYY-MM-DD> (note) |` (replacing `(proposed …)`). To revisit one,
add a new D-row rather than editing a signed-off row.

## Test harness

- iOS `ParitySnapshotTests`: ~25 `testSnap*` methods (Dashboard/History/Settings/Analysis/
  ScanMenu/Onboarding/About/LogFood/ManualEntry/Edit*/CameraReview/Disclaimer/SettingsBottom).
  Not in CI — run locally.
- Android `:app:testDebugUnitTest` + the three porting gates (schema-diff, accessibility-diff,
  no-Gemini-SDK) run in CI on every PR.
- UI-test parity mocks mirror iOS: `wmc.test.uitesting` + `seed*` / `mockEstimationMode` /
  `mockMenuAnalysisMode` / `startAt{Analysis,MenuAnalysis}` / burned-calories=456
  (mirrors the iOS simulator HealthKit mock).
