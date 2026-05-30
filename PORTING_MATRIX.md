# Porting Parity Matrix

Per-screen × per-criterion evidence ledger. Owned by the porting engineer; updated as work lands. Reviewer walks this matrix during Pass 2 (`PORTING_CRITERIA.md` Process step 4) and rejects any cell where evidence is not reproducible.

**Status legend:** `✅ pass` · `⚠️ partial (see notes)` · `❌ fail` · `⏳ pending` · `n/a` (criterion doesn't apply to this screen)

**Modifier suffix:** `✅ auto` (a CI gate enforces it on every PR) · `✅ code` (the implementation exists and was reviewed — observable in source) · `✅ inventory-match` (the screen exists on both platforms; no further evidence claim made here) · `✅ device` (verified on a real device; reference the screenshot/recording) · `⏳ device` (waiting on a real device to verify) · `⚠ <ID>` (intentional deviation — see `PORTING_DEVIATIONS.md`)

**Evidence links:** PR number, commit SHA, screenshot path under `PortingEvidence/`, test file path, or runbook step. Self-attestation ("I verified locally") is not evidence — link an artifact.

**Automated gates that fill cells globally** (any cell marked `✅ auto` means the named script's green output is the evidence — see `.github/workflows/porting-gates.yml`):

| Criterion | Automated gate | Where |
|---|---|---|
| T1.2 Data fields | `scripts/schema-diff.sh` (iOS↔Android, deviations allowlisted in `PORTING_DEVIATIONS.md`) | `scripts/schema-diff.sh` |
| T1.3 Numeric logic | `CalorieCalculatorTest`, `NutritionCalculatorTest`, `MealTypeTest` + `shared-fixtures/{bmr-mifflin-st-jeor,meal-type-by-hour}/cases.json` | `WatchMyCaloriesAndroid/app/src/test/...` |
| T1.4 Image persistence | Code reference: `data/ImageStorage.kt` (write at Save time, `filesDir/{imageID}.jpg`), `security/JpegConfig.kt` (`QUALITY = 80`). **Device verification of persistence across force-stop still required** before any cell flips from `✅ code` to `✅ device`. | Android source |
| T1.5 Gemini path | `scripts/check-android-no-gemini-sdk.sh` + `GeminiParserEdgeCasesTest` + `ai/GeminiRepository.kt` (OkHttp to Cloud Run backend with `X-App-Platform: android` + per-request HMAC) | `scripts/` + Android tests + source |
| T1.8 Attestation client | Code reference: `security/PlayIntegrityManager.kt` (Standard Play Integrity flow + `assertionHeaders(context, bodyBytes)` + 401 re-attest). **Device verification with a real Pixel 9a + tampered-body test still required.** | Android source |
| T1.9/T1.10 Backend | `Backend/test/*.test.ts` (npm test — 208/208 incl. dispatch + Play Integrity + Android HMAC + iOS-non-regress + iOS JSON-contract fixtures from real iPhone 14 captures); pre↔post-Stage-1 head-to-head diff in `Backend/test/contract/ios/STAGE_1_DIFF.md` (byte-identical iOS contract); baseline latency pinned to revision `watchmycalories-backend-dev-00016-xmw` in `Backend/test/contract/ios/baseline-latency.md` (T1.10.h) | `Backend/test/` + `Backend/test/contract/ios/` |
| T2.4 Accessibility | `scripts/accessibility-diff.sh` (Android superset of iOS) | `scripts/accessibility-diff.sh` |
| T2.5 Ad parity | Code reference: `NativeAdView()` in the `Loading` branch of `AnalysisScreen.kt:115` and `MenuAnalysisScreen.kt:130`; `BannerAdView()` at the top of `SettingsScreen.kt` (mirrors iOS `SettingsView.swift:51`). Visual ad-loading parity still needs a screenshot pass. | Android source |

Rows below stay `⏳` for criteria that genuinely need human visual/state evidence — i.e. T2.1 visual diff, T2.2 interaction, T2.3 state matrix beyond what the unit tests cover, T1.7 Health on a real device, and the device-backed half of T1.4 / T1.8 / T2.5.

---

## Tier 1 (release-blocking)

| Screen / Surface | T1.1 Inventory | T1.2 Data fields | T1.3 Numeric logic | T1.4 Image persist | T1.5 Gemini path | T1.6 Settings parity | T1.7 Health | T1.8 Attestation client | T1.9 Backend | T1.10 iOS non-regress |
|---|---|---|---|---|---|---|---|---|---|---|
| Dashboard (HeroSummaryCard + meal sections) | ✅ inventory-match | ✅ auto | ✅ auto | n/a | n/a | n/a | ⏳ device | n/a | n/a | n/a |
| Camera capture | ✅ inventory-match (food-only — see D-003; post-capture MealTypePicker via `ui/camera/CameraReviewScreen.kt`, PORT_AUDIT C1) | n/a | n/a | ✅ code | ✅ auto + code | n/a | n/a | ✅ code | ✅ auto | ✅ auto |
| Photo Library Review | ✅ inventory-match (D-001/D-002 unrelated) | n/a | n/a | ✅ code | ✅ auto + code | n/a | n/a | ✅ code | ✅ auto | ✅ auto |
| Estimation Review (Analysis) | ✅ inventory-match | ✅ auto | n/a | ✅ code | ✅ auto + code | n/a | n/a | ✅ code | ✅ auto | ✅ auto |
| Manual Entry | ✅ inventory-match | ✅ auto | ✅ auto | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| History (day cards, edit/delete) | ✅ inventory-match | ✅ auto | ✅ auto | ✅ code | n/a | n/a | n/a | n/a | n/a | n/a |
| Settings | ✅ inventory-match | ✅ auto | n/a | n/a | n/a | ✅ labels + BannerAd; sliders for H/W/A (⚠ D-004); About row redirect wired | n/a | n/a | n/a | n/a |
| Onboarding (3-step) | ✅ inventory-match | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| Scan Menu sheet | ⚠ D-002 (no Android sheet) | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| Menu Analysis | ✅ inventory-match | ✅ auto | n/a | ✅ code | ✅ auto + code | n/a | n/a | ✅ code | ✅ auto | ✅ auto |
| Stored Menus | ✅ inventory-match | ✅ auto | n/a | ✅ code | n/a | n/a | n/a | n/a | n/a | n/a |
| About / Privacy / Help / Rate App | ✅ inventory-match (exit wired from Settings) | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| LogFoodSheet (3 entry points) | ✅ inventory-match | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |

---

## Tier 2 (visual/behavioral parity — ≥95% to ship)

> **Honest baseline (2026-05-29):** The earlier "≥ 95%" claim was refuted by `PORT_AUDIT.md` and Phases B–F of the audit-correction plan landed the bulk of the visible-parity gap (Dashboard StatRow icons, AnalysisScreen MealTypePicker, swipe-to-delete on History, 4-tab bottom nav + gear-icon Settings, edge-to-edge). Phase C added the camera-flow MealTypePicker (`CameraReviewScreen`); Phase E refactored `cwCard()` / `HeroSummaryCard` / `EmptyStateCard` / `HistoryDayCard` to padding-inside-only and rolled out `Spacing.*` tokens across 13 screens — DashboardScreen and HistoryScreen LazyColumns now own the page-horizontal margin. All remaining `⏳ device` cells still need a paired iOS↔Android screenshot diff before flipping to `✅ device`; the cells below carry `✅ device (Android-only render verified)` only where we have a Pixel 9a screenshot proving the Android side renders cleanly post-fix — the iOS-comparison half is still pending.

| Screen / Surface | T2.1 Visual diff | T2.2 Interaction | T2.3 State matrix | T2.4 Accessibility | T2.5 Ad parity |
|---|---|---|---|---|---|
| Dashboard | ⚠ partial — StatRow icons match iOS (`PortingEvidence/screenshots/android/dashboard-phaseB-statrow-icons.png`, edge-to-edge in `dashboard-phaseF-edge-to-edge.png`); paired iOS diff pending | ⏳ device (4-tab bottom nav lands via `MainActivity.kt:230-273` — gear icon → Settings) | ⏳ device | ✅ auto | ✅ code (banner) |
| Camera capture | ⏳ device | ✅ code — haptic feedback on capture button (`ui/camera/CameraScreen.kt`, `view.performHapticFeedback(LONG_PRESS)`); `imePadding()` on capture button; MealTypePicker now appears between capture and analysis via `CameraReviewScreen.kt` (PORT_AUDIT C1/C2/C3) | ⏳ device | ✅ auto | n/a |
| Photo Library Review | ⏳ device | ⏳ device | ⏳ device | ✅ auto | n/a |
| Estimation Review (Analysis) | ⏳ device | ✅ code — MealTypePicker state hoisted into `AnalysisScreen.kt`, propagated via `onSaveLog(EstimationResult, MealType)`; Save row gets `Modifier.imePadding()` | ⏳ device | ✅ auto | ✅ code (native in Loading) |
| Manual Entry | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ⏳ device (banner) |
| History | ⚠ partial — empty state matches iOS book icon (`PortingEvidence/screenshots/android/history-empty-phaseF.png`); paired iOS diff pending | ✅ code — swipe-to-delete via `SwipeToDismissBox` on `FoodEntryItem` (`ui/history/HistoryScreen.kt:349-386`) | ⏳ device (empty-state verified on device — see screenshot) | ✅ auto | ✅ code (banner) |
| Settings | ⚠ partial — segmented controls + sliders render cleanly (`PortingEvidence/screenshots/android/settings-phaseF.png`); paired iOS diff pending | ✅ device | ✅ device | ✅ auto | ✅ code (banner — see iOS `SettingsView.swift:51`) |
| Onboarding | ⏳ device | ⏳ device | ⏳ device | ✅ auto | n/a |
| Scan Menu sheet | n/a (D-002) | n/a (D-002) | n/a (D-002) | ✅ auto (IDs reserved) | n/a |
| Menu Analysis | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ✅ code (native in Loading) |
| Stored Menus | ⚠ partial — empty state renders cleanly (`PortingEvidence/screenshots/android/scannedMenus-empty-phaseF.png`); paired iOS diff pending | ⏳ device | ✅ device (empty state) | ✅ auto | ✅ code (banner) |
| LogFoodSheet | ⏳ device | ⏳ device | ⏳ device | ✅ auto | n/a (no ads on bottom sheets) |

---

## State Parity Sub-Matrix (T2.3 detail)

For each row that touches user-visible state, walk every state the iOS app exhibits and confirm the Android app exhibits the equivalent.

| Screen | Empty | Loading | Err: network | Err: 429 | Err: invalid JSON | Err: not-a-menu | Perm: camera | Perm: health | AI consent off | Offline + cache |
|---|---|---|---|---|---|---|---|---|---|---|
| Dashboard | ✅ device (`dashboard-phaseF-edge-to-edge.png` — empty meal sections) | n/a | ⏳ | n/a | n/a | n/a | n/a | ⏳ | n/a | ⏳ |
| Camera capture | n/a | n/a | n/a | n/a | n/a | n/a | ✅ device (permissions check + rationale wired) | n/a | ⏳ | n/a |
| Estimation Review | n/a | ⏳ | ⏳ | ⏳ | ⏳ | n/a | n/a | n/a | n/a | n/a |
| Menu Analysis | n/a | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | n/a | n/a | n/a | n/a |
| History | ✅ device (`history-empty-phaseF.png`) | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | ⏳ |
| Stored Menus | ✅ device (`scannedMenus-empty-phaseF.png`) | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |

---

## Evidence directory

Place evidence artifacts under `PortingEvidence/`:

```
PortingEvidence/
├── screenshots/
│   ├── ios/<screen>-<state>.png
│   └── android/<screen>-<state>.png
├── recordings/
│   └── <screen>-<flow>.mp4
└── traces/
    └── <criterion>-<test-run>.txt
```

The matrix cell links to the corresponding file path or PR/commit. Never embed binaries in this file.
