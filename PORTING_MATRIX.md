# Porting Parity Matrix

Per-screen × per-criterion evidence ledger. Owned by the porting engineer; updated as work lands. Reviewer walks this matrix during Pass 2 (`PORTING_CRITERIA.md` Process step 4) and rejects any cell where evidence is not reproducible.

**Status legend:** `✅ pass` · `⚠️ partial (see notes)` · `❌ fail` · `⏳ pending` · `n/a` (criterion doesn't apply to this screen)

**Evidence links:** PR number, commit SHA, screenshot path under `PortingEvidence/`, test file path, or runbook step. Self-attestation ("I verified locally") is not evidence — link an artifact.

**Automated gates that fill cells globally** (any cell marked `✅ auto` means the named script's green output is the evidence — see `.github/workflows/porting-gates.yml`):

| Criterion | Automated gate | Where |
|---|---|---|
| T1.2 Data fields | `scripts/schema-diff.sh` (iOS↔Android, deviations allowlisted in `PORTING_DEVIATIONS.md`) | `scripts/schema-diff.sh` |
| T1.3 Numeric logic | `CalorieCalculatorTest`, `NutritionCalculatorTest`, `MealTypeTest` + `shared-fixtures/{bmr-mifflin-st-jeor,meal-type-by-hour}/cases.json` | `WatchMyCaloriesAndroid/app/src/test/...` |
| T1.5 Gemini path | `scripts/check-android-no-gemini-sdk.sh` + `GeminiParserEdgeCasesTest` | `scripts/` + Android tests |
| T1.9/T1.10 Backend | `Backend/test/*.test.ts` (npm test — 201/201 incl. dispatch + Play Integrity + Android HMAC + iOS-non-regress) | `Backend/test/` |
| T2.4 Accessibility | `scripts/accessibility-diff.sh` (Android superset of iOS) | `scripts/accessibility-diff.sh` |

Rows below stay `⏳` for criteria that need human visual/state evidence (T2.1 visual diff, T2.2 interaction, T2.3 state matrix beyond what the unit tests cover, T1.7 Health, T1.8 client-side attestation flow on a real device).

---

## Tier 1 (release-blocking)

| Screen / Surface | T1.1 Inventory | T1.2 Data fields | T1.3 Numeric logic | T1.4 Image persist | T1.5 Gemini path | T1.6 Settings parity | T1.7 Health | T1.8 Attestation client | T1.9 Backend | T1.10 iOS non-regress |
|---|---|---|---|---|---|---|---|---|---|---|
| Dashboard (HeroSummaryCard + meal sections) | ⏳ | ✅ auto | ✅ auto | n/a | n/a | n/a | ⏳ | n/a | n/a | n/a |
| Camera capture | ⏳ | n/a | n/a | ⏳ | ✅ auto | n/a | n/a | ⏳ | ✅ auto | ✅ auto |
| Photo Library Review | ⏳ | n/a | n/a | ⏳ | ✅ auto | n/a | n/a | ⏳ | ✅ auto | ✅ auto |
| Estimation Review | ⏳ | ✅ auto | n/a | ⏳ | ✅ auto | n/a | n/a | ⏳ | ✅ auto | ✅ auto |
| Manual Entry | ⏳ | ✅ auto | ✅ auto | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| History (day cards, edit/delete) | ⏳ | ✅ auto | ✅ auto | ⏳ | n/a | n/a | n/a | n/a | n/a | n/a |
| Settings | ⏳ | ✅ auto | n/a | n/a | n/a | ⏳ | n/a | n/a | n/a | n/a |
| Onboarding (3-step) | ⏳ | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| Scan Menu sheet | ⏳ | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| Menu Analysis | ⏳ | ✅ auto | n/a | ⏳ | ✅ auto | n/a | n/a | ⏳ | ✅ auto | ✅ auto |
| Stored Menus | ⏳ | ✅ auto | n/a | ⏳ | n/a | n/a | n/a | n/a | n/a | n/a |
| About / Privacy / Help / Rate App | ⏳ | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |
| LogFoodSheet (3 entry points) | ⏳ | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |

---

## Tier 2 (visual/behavioral parity — ≥95% to ship)

| Screen / Surface | T2.1 Visual diff | T2.2 Interaction | T2.3 State matrix | T2.4 Accessibility | T2.5 Ad parity |
|---|---|---|---|---|---|
| Dashboard | ⏳ | ⏳ | ⏳ | ✅ auto | ⏳ (banner) |
| Camera capture | ⏳ | ⏳ | ⏳ | ✅ auto | n/a |
| Photo Library Review | ⏳ | ⏳ | ⏳ | ✅ auto | n/a |
| Estimation Review | ⏳ | ⏳ | ⏳ | ✅ auto | ⏳ (native, loading) |
| Manual Entry | ⏳ | ⏳ | ⏳ | ✅ auto | ⏳ (banner) |
| History | ⏳ | ⏳ | ⏳ | ✅ auto | ⏳ (banner) |
| Settings | ⏳ | ⏳ | ⏳ | ✅ auto | ⏳ (banner — open question) |
| Onboarding | ⏳ | ⏳ | ⏳ | ✅ auto | n/a |
| Scan Menu sheet | ⏳ | ⏳ | ⏳ | ✅ auto | n/a |
| Menu Analysis | ⏳ | ⏳ | ⏳ | ✅ auto | ⏳ (native, loading) |
| Stored Menus | ⏳ | ⏳ | ⏳ | ✅ auto | n/a |
| LogFoodSheet | ⏳ | ⏳ | ⏳ | ✅ auto | ⏳ (banner) |

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
