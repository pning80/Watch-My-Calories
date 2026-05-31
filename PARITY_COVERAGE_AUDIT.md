# XCUITest Coverage Audit — iOS spec vs PARITY_LEDGER.md

Produced 2026-05-30 as Task #4 output. Sibling to `PARITY_LEDGER.md` (per-element enumeration) and `PARITY_INCONSISTENCIES.md` (forthcoming).

**Coverage rollup (300 fine-grained elements; the `PARITY_LEDGER.md` table consolidates these into 189 rows):**

| Bucket | Count | % of total |
|---|---:|---:|
| **Full coverage** (test exercises trigger AND asserts outcome) | 81 | 27% |
| **Partial coverage** (test asserts existence but not interaction) | 44 | 15% |
| **SPEC-GAP** (no XCUITest exercises this element) | 132 | 44% |
| **Display-only** (no interaction to test) | 43 | 14% |

**Top-10 SPEC-GAP clusters (highest leverage to close):**

1. **Edit Meal Group flow** — 9 gaps (`EditMealGroupView` in `Components.swift` — meal name, type picker, item rows, nutrition disclosure, Save/Cancel)
2. **Edit Food Entry flow** — 8 gaps (`EditFoodEntryView` — food/meal name, calories, quantity, meal-type picker, nutrition disclosure, Save/Cancel)
3. **Menu Camera flow** — 11 gaps (entire `MenuCameraView` — capture, retake, analyze, photo-library picker, disclaimer)
4. **Menu Analysis flow** — 9 gaps (entire `MenuAnalysisView` — error/success/not-a-menu states + AI consent)
5. **Photo Library Review (food)** — 9 gaps (entire `PhotoLibraryReviewView` — picker, meal-type, use/reselect, disclaimer)
6. **Picker interactions** (Settings + Onboarding) — 11 gaps (height feet/inches, weight wheels US+metric, age wheel, theme/gender/activity-level value changes)
7. **Food Entry Group Card** — 6 gaps (expand/collapse, thumbnail tap, multi-item/single-item/sub-item context menus on Dashboard + History)
8. **AI Consent sheets** — 4 gaps (Estimation Review × Allow/Decline, Menu Analysis × Allow/Decline)
9. **Full-screen image viewer** — 4 gaps (pinch, drag pan, double-tap zoom, close button)
10. **Ad surfaces** — 3 gaps (banner ad tap, native ad container tap, native ad CTA button)

**Cost estimate** to land every SPEC-GAP test:

| Layer | New tests | Estimated LOC | Estimated time |
|---|---:|---:|---:|
| iOS XCUITest extensions | 132 | ~2,000–3,000 | 6–10 eng-hours (drafting + sim runs) |
| Android instrumented mirrors | ≥132 (≥1 per iOS test) | ~2,500–4,000 | 8–14 eng-hours (Compose UI test scaffolding + DI plumbing) |
| Test infrastructure for permission-gated flows (camera/library/health) | — | ~200 (mocks, FakeContentResolver) | 2–4 eng-hours |
| Inconsistency log + fix plan generation | — | scripted | 1–2 eng-hours |
| **Total** | **≥264 new test functions** | **~5,000 LOC** | **~17–30 eng-hours** |

**What the existing iOS test suite already gives us right now (without extending):**
- 168 elements covered (81 full + 44 partial + 43 display-only) — 56% of the ledger
- 132 elements remain untested on iOS

**Per-screen SPEC-GAP counts (full breakdown):**

| Screen | SPEC-GAP count | Total interactive | Coverage % |
|---|---:|---:|---:|
| Dashboard | 6 | 11 | 45% |
| History | 4 | 9 | 56% |
| Settings | 10 | 22 | 55% |
| Onboarding | 10 | 16 | 38% |
| Camera (food) | 3 | 11 | 73% |
| Menu Camera | 11 | 10 | 0% (entire screen) |
| Estimation Review | 4 | 10 | 60% |
| Menu Analysis | 9 | 9 | 0% (entire screen) |
| Scanned Menus | 6 | 6 | 0% (entire screen) |
| Photo Library Review | 9 | 9 | 0% (entire screen) |
| LogFoodSheet | 1 | 3 | 67% |
| ScanMenuSheet | 0 | 3 | 100% |
| About | 1 | 4 | 75% |
| ContentView Tabs | 3 | 7 | 57% |
| Manual Entry | 3 | 11 | 73% |
| Components (Edit/View/AIConsent/FullScreen) | 19 | 31 | 39% |
| AppMenu | 0 | 4 | 100% |
| Banner/Native Ads | 3 | 3 | 0% |

**Concentrated weakness:** four screens are 0% covered (Menu Camera, Menu Analysis, Scanned Menus, Photo Library Review). These dominate the bottom half of the cost estimate.

(Per-row test mapping for each row in `PARITY_LEDGER.md` is in the Explore-agent transcript and will be merged in once we agree on scope.)
