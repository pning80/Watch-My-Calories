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
| T1.9/T1.10 Backend | `Backend/test/*.test.ts` (npm test — 201/201 incl. dispatch + Play Integrity + Android HMAC + iOS-non-regress) | `Backend/test/` |
| T2.4 Accessibility | `scripts/accessibility-diff.sh` (Android superset of iOS) | `scripts/accessibility-diff.sh` |
| T2.5 Ad parity | Code reference: `NativeAdView()` in the `Loading` branch of `AnalysisScreen.kt:115` and `MenuAnalysisScreen.kt:130`; `BannerAdView()` at the top of `SettingsScreen.kt` (mirrors iOS `SettingsView.swift:51`). Visual ad-loading parity still needs a screenshot pass. | Android source |

Rows below stay `⏳` for criteria that genuinely need human visual/state evidence — i.e. T2.1 visual diff, T2.2 interaction, T2.3 state matrix beyond what the unit tests cover, T1.7 Health on a real device, and the device-backed half of T1.4 / T1.8 / T2.5.

---

## Tier 1 (release-blocking)

| Screen / Surface | T1.1 Inventory | T1.2 Data fields | T1.3 Numeric logic | T1.4 Image persist | T1.5 Gemini path | T1.6 Settings parity | T1.7 Health | T1.8 Attestation client | T1.9 Backend | T1.10 iOS non-regress |
|---|---|---|---|---|---|---|---|---|---|---|
| Dashboard (HeroSummaryCard + meal sections) | ✅ inventory-match | ✅ auto | ✅ auto | n/a | n/a | n/a | ⏳ device | n/a | n/a | n/a |
| Camera capture | ✅ inventory-match (food-only — see D-003) | n/a | n/a | ✅ code | ✅ auto + code | n/a | n/a | ✅ code | ✅ auto | ✅ auto |
| Photo Library Review | ✅ inventory-match (D-001/D-002 unrelated) | n/a | n/a | ✅ code | ✅ auto + code | n/a | n/a | ✅ code | ✅ auto | ✅ auto |
| Estimation Review (Analysis) | ✅ inventory-match | ✅ auto | n/a | ✅ code | ✅ auto + code | n/a | n/a | ✅ code | ✅ auto | ✅ auto |
| Manual Entry | ✅ inventory-match | ✅ auto | ✅ auto | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| History (day cards, edit/delete) | ✅ inventory-match | ✅ auto | ✅ auto | ✅ code | n/a | n/a | n/a | n/a | n/a | n/a |
| Settings | ✅ inventory-match | ✅ auto | n/a | n/a | n/a | ✅ labels + BannerAd; ⏳ visual spot-check | n/a | n/a | n/a | n/a |
| Onboarding (3-step) | ✅ inventory-match | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| Scan Menu sheet | ⚠ D-002 (no Android sheet) | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| Menu Analysis | ✅ inventory-match | ✅ auto | n/a | ✅ code | ✅ auto + code | n/a | n/a | ✅ code | ✅ auto | ✅ auto |
| Stored Menus | ✅ inventory-match | ✅ auto | n/a | ✅ code | n/a | n/a | n/a | n/a | n/a | n/a |
| About / Privacy / Help / Rate App | ✅ inventory-match | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| LogFoodSheet (3 entry points) | ✅ inventory-match | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |

---

## Tier 2 (visual/behavioral parity — ≥95% to ship)

| Screen / Surface | T2.1 Visual diff | T2.2 Interaction | T2.3 State matrix | T2.4 Accessibility | T2.5 Ad parity |
|---|---|---|---|---|---|
| Dashboard | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ✅ code (banner) |
| Camera capture | ⏳ device | ⏳ device | ⏳ device | ✅ auto | n/a |
| Photo Library Review | ⏳ device | ⏳ device | ⏳ device | ✅ auto | n/a |
| Estimation Review (Analysis) | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ✅ code (native in Loading) |
| Manual Entry | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ⏳ device (banner) |
| History | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ✅ code (banner) |
| Settings | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ✅ code (banner — see iOS `SettingsView.swift:51`) |
| Onboarding | ⏳ device | ⏳ device | ⏳ device | ✅ auto | n/a |
| Scan Menu sheet | n/a (D-002) | n/a (D-002) | n/a (D-002) | ✅ auto (IDs reserved) | n/a |
| Menu Analysis | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ✅ code (native in Loading) |
| Stored Menus | ⏳ device | ⏳ device | ⏳ device | ✅ auto | ✅ code (banner) |
| LogFoodSheet | ⏳ device | ⏳ device | ⏳ device | ✅ auto | n/a (no ads on bottom sheets) |

---

## State Parity Sub-Matrix (T2.3 detail)

For each row that touches user-visible state, walk every state the iOS app exhibits and confirm the Android app exhibits the equivalent.

| Screen | Empty | Loading | Err: network | Err: 429 | Err: invalid JSON | Err: not-a-menu | Perm: camera | Perm: health | AI consent off | Offline + cache |
|---|---|---|---|---|---|---|---|---|---|---|
| Dashboard | ⏳ | n/a | ⏳ | n/a | n/a | n/a | n/a | ⏳ | n/a | ⏳ |
| Camera capture | n/a | n/a | n/a | n/a | n/a | n/a | ⏳ | n/a | ⏳ | n/a |
| Estimation Review | n/a | ⏳ | ⏳ | ⏳ | ⏳ | n/a | n/a | n/a | n/a | n/a |
| Menu Analysis | n/a | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | n/a | n/a | n/a | n/a |
| History | ⏳ | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | ⏳ |

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
