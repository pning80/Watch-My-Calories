# Porting Criteria: iOS → Android

Canonical rubric for verifying that the Android app at `WatchMyCaloriesAndroid/` is a faithful port of the iOS app at `WatchMyCalories/`. Used as the review gate for porting PRs.

## Operating Principles

1. **iOS is the source of truth.** Any Android divergence is either (a) listed in the Deviation Registry (`PORTING_DEVIATIONS.md`) with rationale, or (b) treated as a defect.
2. **A criterion passes only with evidence** — a test, a screenshot, or a recorded playthrough. Self-attestation does not count.
3. **Tier 1 must be 100%. Tier 2 is ≥95% with each gap documented. Tier 3 is aspirational.**
4. The platforms share a single architecture: both call the Cloud Run backend proxy, both attest (App Attest on iOS, Play Integrity on Android), and the prompt sent to Gemini is identical (including unit-adaptation to the user's preference).
5. **No stub-shaped code counts as done.** A class or function whose signature looks right but whose runtime path cannot succeed end-to-end (e.g., a manager that calls an endpoint the server cannot honor, a function parameter that is always hardcoded by its callers, a UI button with a no-op handler) is a defect, not a pass. "Compiles and doesn't crash" is not evidence.
6. **The two codebases must be kept honest about their state.** If `CLAUDE.md` describes files or layouts that no longer exist, that is a defect against this criteria document because reviewers rely on it. Stale guidance is fixed in the same PR that creates the drift.
7. **No iOS app code changes during the port.** The Android port must conform to iOS, not the other way around. Touching anything under `WatchMyCalories/` (the iOS Xcode project — Swift sources, asset catalogs, `project.yml`, entitlements, plists) as part of a porting PR is out of scope and rejected at review. The narrow exceptions are: (a) generating read-only fixtures consumed by both test suites under `shared-fixtures/` from iOS exports, and (b) editing repo-level docs (`CLAUDE.md`, `PORTING_*.md`, `README.md`). If a genuine iOS bug is discovered while porting, file it and fix it in a separate PR on its own merit — do not bundle it into the porting work, and do not let "the Android version revealed it" justify a coupled change.
8. **The backend (`Backend/`) is shared infrastructure, not iOS app code.** Principle 7 does **not** forbid changes there — it forbids changes to the iOS *app*. The backend may and must grow to serve Android (new attestation path, new env vars, new tests) provided **the iOS-facing wire contract is preserved byte-for-byte**: same endpoint URLs, same request/response JSON shapes, same headers, same error model, same status codes for the same conditions. The non-regression requirement is asymmetric — **when in doubt, iOS wins**. Operationalized in **T1.10 iOS Non-Regression Guarantees (Backend)**, which is a release blocker: any backend change that ships without satisfying T1.10 is rejected even if every Android-side criterion passes.

## Resolved Decisions

The following were previously open; resolutions are now baked into the criteria below.

1. **Meal-time windows** — resolved as non-issue. iOS `7..<10` and Android `in 7..9` produce identical hour assignments. No change required.
2. **`imageIndex` response field** — does not exist on either platform; CLAUDE.md description was stale. No change required.
3. **Android Gemini integration** — Android will match iOS architecturally: route through the Cloud Run backend proxy with Play Integrity attestation. The Google AI SDK path and any user-provided-key UI are removed. The Play Integrity backend endpoint must be implemented and verified end-to-end.
4. **Native ad during analysis** — Android will port the iOS behavior: `NativeAdView` rendered during the loading state of both `AnalysisScreen` and `MenuAnalysisScreen`.

## Tier 1 — Hard Requirements (block release)

### T1.1 Screen & Flow Inventory Parity

Every iOS screen has an Android counterpart with the same purpose and accessible from the same navigation path.

| iOS surface | Android status |
|---|---|
| Dashboard (HeroSummaryCard + meal sections) | exists — verify field-for-field |
| Camera capture (multi-photo, meal-type picker) | verify multi-image + picker-before-submit |
| Photo Library Review (picker → estimation) | picker launcher exists in `MainActivity.kt:96` but is not connected to the `LogFoodSheet` "Choose from Library" callback — review/edit step before analysis is also missing |
| Estimation Review (loading w/ native ad, error, success, edit, save) | exists — verify native-ad-while-loading |
| Manual Entry (with optional macros disclosure) | exists — verify validation rules |
| History (day cards, expandable, edit/delete) | exists |
| Settings (every row in iOS Settings) | row-for-row audit required |
| Onboarding (3-step, Skip from any step) | exists — verify Skip behavior |
| Scan Menu sheet + Menu Analysis + Stored Menus | exists |
| About + Privacy + Help + Rate App | exists |
| LogFoodSheet (3 entry points) | "Choose from Library" UI present, no handler |

**Verification:** screenshots per screen, attached to the parity matrix.

### T1.2 Data Model Parity (field-exact)

Every field on iOS SwiftData models must exist on Android Room entities with the same name (camelCase preserved **exactly**, including capitalization of acronyms — `imageID`, not `imageId`), type semantics, optionality, and unit (always metric).

- `UserProfile` — `height` (cm), `weight` (kg), `age`, `genderRaw`, `activityLevelRaw`, `targetCalories`, `createdAt`. Singleton invariant enforced.
- `FoodEntry` — `id` (UUID), `name`, `calories`, `quantity`, `timestamp`, `protein?`, `carbs?`, `fat?`, `imageID?`, `mealName?`, `mealTypeRaw`.
- `MenuScan` — `id`, `restaurantName?`, `imageID?`, `timestamp`, serialized items array.

**Cross-platform type mapping (binding rules, no exceptions):**

| iOS (SwiftData) | Android (Room) | Notes |
|---|---|---|
| `UUID` | `String` storing `UUID.toString()` (lowercase, hyphenated) | Field name and on-disk filename must round-trip; an entry created on either platform must locate its JPEG on either platform. |
| `Date` | `Long` (epoch millis, UTC) | Identical hour assignment for meal-type bucketing. |
| `Data` (serialized JSON) | `String` (JSON) | Pick one wire format per entity (default: JSON `String`); document the choice in the entity header. Storing iOS `Data` and Android `String` for the "same" field is a defect. |
| Optional `T?` | Nullable `T?` | Same null semantics — never substitute an empty string or sentinel. |

**Known live defects in this area (as of last audit):**
- `Entities.kt:29,38` use `imageId: String?` — must be renamed to `imageID` to match iOS.
- `MenuScan.itemsJson: String` (Android) vs `MenuScan.itemsData: Data` (iOS) — Android must rename to `itemsData` and store the same JSON payload (as a `ByteArray` if needed to mirror iOS `Data`, or as a Room `TypeConverter`-backed `String` field still named `itemsData`). Per Operating Principle 7, iOS does not change.

**Verification:** schema diff between platforms (export both as JSON) **plus** a round-trip test: create a `FoodEntry` on iOS with a known UUID, copy the SQLite/SwiftData export to a fixture, materialize it as a Room row on Android, and assert every field round-trips byte-for-byte (modulo the documented type mapping).

### T1.3 Business-Logic Parity (numerically identical)

Identical outputs on both platforms for:
- Mifflin–St Jeor BMR across the matrix of (gender ∈ {Male, Female, Other}) × age × weight × height.
- TDEE multipliers (1.2 / 1.375 / 1.55 / 1.725).
- `remaining = max(0, (target + burned) − consumed)`.
- Daily macro totals (sum of entries).
- Meal-type auto-assignment for every hour 0–23.

**Verification:** golden-vector fixtures shared by both test suites (deferred until criteria stabilize).

### T1.4 Image Persistence

iOS stores capture JPEGs in `Documents/{UUID}.jpg` keyed by `FoodEntry.imageID`. Android must persist images to `filesDir/{UUID}.jpg` under the same key (with `{UUID}` produced by `UUID.toString()`, lowercase, hyphenated — see T1.2 type mapping). History thumbnails must survive process death.

In-memory-only handling of capture images (e.g., a `Bitmap` passed via navigation arguments or `ViewModel` state that is discarded on process death) is a defect. The persistence write must happen at the same point in the flow as on iOS — when the user confirms "Save" on the estimation review screen, not earlier and not later — so an abandoned analysis does not leak files.

**Verification:** create entry → force-stop the app via `adb shell am force-stop com.pning80.watchmycalories` → reopen → thumbnail still rendered from disk; **and** abandon an analysis without saving → confirm no orphan JPEG remains under `filesDir/`.

### T1.5 Gemini Integration (request + response)

- Both platforms call the **same Cloud Run backend endpoint** (`{BackendConfig.baseURL}/v1beta/models/default:generateContent`). No direct Google AI SDK calls from the client.
- The `com.google.ai.client.generativeai` dependency is **removed** from `WatchMyCaloriesAndroid/app/build.gradle.kts`. Presence of that line is, by itself, a T1.5 failure regardless of whether the SDK is currently invoked.
- No client source file may contain a literal Gemini API key, including placeholders like `"YOUR_API_KEY"`. (Current offenders: `security/BackendConfig.kt:9`, `MainActivity.kt:93`.)
- Request payload is identical: base64 image(s) + prompt, including unit-system adaptation in the prompt text.
- Unit-system adaptation in the prompt is **driven by the user's actual `UnitSystem` setting at the call site**. A function that accepts an `isMetric` (or equivalent) parameter but whose every caller passes a hardcoded `true`/`false` violates this criterion — the parameter must be threaded from `SettingsRepository`/`SettingsStore` through the ViewModel to the request builder.
- Backend selects the model; clients do not pin a model. (Current backend default: `gemini-3.1-flash-lite`.)
- Response shape is identical: `{ items: [{ name, quantity, calories, protein?, carbs?, fat?, confidence? }], mealName? }` for food, `{ restaurantName?, items: [...] }` for menu.
- Both platforms degrade identically on: missing optional macros, negative numbers (clamp to 0), malformed JSON, `not_a_menu` sentinel.
- Retry: 3 attempts, exponential backoff 1s / 2s / 4s.
- Rate-limit (HTTP 429): surface `Retry-After` countdown to the user on both platforms.

**Verification:** mocked-response unit tests using shared fixtures; live end-to-end test on Pixel 9a + Test iPhone 14 hitting the same dev backend; grep gate in CI that fails the build if `generativeai`, `"YOUR_API_KEY"`, or a direct `GenerativeModel(` constructor appears under `WatchMyCaloriesAndroid/app/src/`.

### T1.6 Settings Parity (row-for-row)

Every iOS Settings row exists on Android with the same effect when toggled. Specifically:
- Unit System change propagates to all weight/height displays within one frame, no restart.
- AI Consent OFF hides camera/scan entry points or shows a re-consent dialog (mirror iOS behavior).
- Theme labels are **identical wording on both platforms**. iOS canonical labels (from `SettingsStore.swift` `AppTheme` raw values) are **System / Light / Dark**. Android must use these exact strings — not "Auto", not "Follow system".
- Unit System labels are also identical to iOS raw values: **US Customary / Metric**. Android's current "Metric System" toggle label is a defect.

### T1.7 Permissions & Health Integration

- HealthKit `activeEnergyBurned` ↔ Health Connect `ActiveCaloriesBurnedRecord`. Today's total in kcal, updated within ~5s of foregrounding.
- Observer/subscription equivalence — iOS uses `HKObserverQuery` with background delivery; Android updates on foreground at minimum, background if Health Connect supports it on the test device.
- HeroSummaryCard "effective goal" = `target + burnedToday` on both.
- Denied/unavailable states fall through to `burned = 0` without an error UI.

### T1.8 Attestation Posture (client-side)

- iOS production: App Attest only (legacy `x-backend-key` disabled).
- Android production: Play Integrity. Legacy key fallback permitted only in dev builds with strict rate limiting, mirroring iOS dev policy — `PlayIntegrityManager` must implement the emulator/unsupported-device fallback path that iOS's `AppAttestManager` already has, not just hard-fail.
- Both: attest successfully on first backend call from a clean install; cache the attested key (no re-attest per request); fall back gracefully on simulator/emulator (UI-testing guard).
- **Per-request integrity (locked design): HMAC-over-body with a per-key secret issued at attest time.** iOS signs every Gemini request body via App Attest assertion (`Services.swift` → `AppAttestManager.assertionHeaders()`). Android must achieve equivalent per-request integrity by the following protocol — chosen because it mirrors iOS behaviorally, avoids Google Play Integrity API quota on every call, and reuses the existing `Backend/src/hmac-secret.ts` patterns:
  1. **Secret issuance.** On successful `/attest/verify` for an Android request, the server generates a fresh 32-byte random secret, stores it on the Firestore key doc (field: `androidAssertionSecret`, alongside `publicKeyPem`/`counter`/`hmac`/`platform`), and returns it once in the verify response body: `{ "success": true, "androidAssertionSecret": "<hex>" }`. The secret is never returned again; loss requires re-attest.
  2. **Client storage.** Android stores the secret in `EncryptedSharedPreferences` alongside the keyID. Treated as equally sensitive to the attested key.
  3. **Per-request headers (Android).** Every call to `/v1beta/models/*` includes:
     - `X-Android-Key-Id: <keyID>`
     - `X-Android-Counter: <monotonically increasing integer, persisted in EncryptedSharedPreferences>`
     - `X-Android-Assertion: <hex(HMAC-SHA256(secret, counter || ":" || sha256(request-body))) >`
  4. **Server verification.** Server retrieves the secret by `X-Android-Key-Id`, recomputes the HMAC, rejects on mismatch. Server rejects if `X-Android-Counter` ≤ last-stored counter (replay protection), then persists the new counter atomically.
  5. **Failure modes.** Any of: missing headers, unknown keyID, HMAC mismatch, counter regression → HTTP 401 with body `{"error":"android_assertion_invalid"}`. Client re-attests on 401, retries once.
- A path where Android sends *no* per-request integrity proof (only the at-attest token) is a regression from iOS and fails this criterion.
- The end-to-end verification gate is: a clean-install Android build hits the dev backend, attests successfully via Play Integrity, the attested key is persisted, the first Gemini request is accepted with the per-request integrity proof, and a second request from a tampered body is rejected. Until that flow passes on a real Pixel 9a, T1.8 is FAIL regardless of how complete the client `PlayIntegrityManager` looks.
- The Google AI SDK dependency is removed from the Android app; there is no path from the client to Gemini that bypasses the backend (covered redundantly by T1.5 — both must hold).
- The server-side counterpart is specified in **T1.9**.

### T1.9 Backend Compatibility (server-side)

Per Operating Principle 8, the backend (`Backend/`) may grow to serve Android, but must preserve the iOS-facing contract byte-for-byte. The following are required server-side deliverables; the iOS app does not change, and existing iOS-path tests must continue to pass unmodified.

**T1.9.a — Platform discriminator.** `/attest/challenge` and `/attest/verify` accept an `X-App-Platform: ios | android` header (this is the existing header iOS already sends from `Services.swift`; reusing it means **zero iOS changes** are required). Missing header defaults to `ios` so iOS clients already in the field continue to work. Dispatch by header value to the appropriate verifier.

**T1.9.b — Play Integrity verifier.** A new server module (e.g., `Backend/src/play-integrity.ts`) implements:
- JWT parse of the Play Integrity token (not CBOR — the current Apple verifier in `attestation.ts` will reject the token because it expects `fmt === 'apple-appattest'`).
- Call to Google's Play Integrity API to validate the token signature and decode verdicts.
- Verdict checks: `appIntegrity.appRecognitionVerdict === "PLAY_RECOGNIZED"`, `deviceIntegrity.deviceRecognitionVerdict` includes `"MEETS_DEVICE_INTEGRITY"`.
- Package-name allowlist: `requestDetails.requestPackageName === PLAY_INTEGRITY_PACKAGE_NAME`.
- Nonce binding: `requestDetails.requestHash` (or `nonce`, depending on token version) matches the challenge issued by `/attest/challenge`.

**T1.9.c — Firestore key schema additions.** The doc shape used by `attested-keys.ts` / `firestore-key.ts` gains a `platform: 'ios' | 'android'` field. Storage continues to use the existing `attestedKeys-{env}` collection — do not split collections by platform; filter by the new field instead. Apple-specific counter-replay protection in `assertion.ts` stays on the iOS path; the Android path uses Play Integrity token timestamp + token ID for freshness.

**T1.9.d — Per-request integrity for Android.** The server side of T1.8's locked protocol: a new verifier `Backend/src/verify-request-android.ts` that, on every `/v1beta/models/*` call carrying `X-Android-Key-Id`:
- Loads the key doc, reads `androidAssertionSecret` and the last stored `counter`.
- Recomputes `HMAC-SHA256(secret, counter || ":" || sha256(request-body))` and compares (constant-time) against `X-Android-Assertion`.
- Rejects if `X-Android-Counter` is not strictly greater than the stored counter.
- On success, persists the new counter atomically and proceeds to the existing Gemini forwarding path.
- On failure, returns `401 {"error":"android_assertion_invalid"}` — no information leakage about which check failed.

The verifier runs *parallel to* the iOS assertion verifier in `verify-request.ts`; dispatch by presence of `X-Android-Key-Id` vs the iOS-equivalent assertion header. Both verifiers gate the same Gemini forwarding path.

**T1.9.e — New env vars and secrets.** Documented in `.env.dev` / `.env.prod` templates and `deploy.sh`:
- `PLAY_INTEGRITY_PROJECT_NUMBER` — numeric GCP project number (e.g. `657698311127`). Used in the Play Integrity API request path.
- `PLAY_INTEGRITY_PACKAGE_NAME` — e.g. `com.pning80.watchmycalories`. Used in the verdict allowlist check.

**Authentication uses Application Default Credentials (ADC), not a downloaded service-account JSON key.** The existing Cloud Run runtime service account (`watchmycalories-backend@<project>.iam.gserviceaccount.com`) is granted `roles/playintegrity.user`, and the backend obtains tokens via ADC at runtime. No JSON key is downloaded, committed, or stored in Secret Manager — this avoids long-lived key material entirely and matches the existing pattern for Firestore/Secret Manager access. This requires the Play Console app to be linked to the same GCP project; if not, cross-project credentials would be needed instead (and the design should be revisited).

**T1.9.f — New npm dependency.** Either `googleapis` (broad) or a focused Play Integrity client. Pin to a specific version; document choice in `Backend/CHANGELOG.md`.

**T1.9.g — Tests.** New files under `Backend/test/`: `play-integrity-verify.test.ts`, `verify-request-android.test.ts`, `platform-dispatch.test.ts`. Every existing iOS-path test must continue to pass with no modifications. Shared fixtures (challenge issuance, rate-limiter reset) factored into `Backend/test/helpers/` to avoid duplication.

**T1.9.h — Non-changes (assert these explicitly stayed identical).**
- `/v1beta/models/default:generateContent` request and response shapes — Android sends the same payload iOS sends; no special-casing.
- `Retry-After` header on 429 responses — Android client parses it identically to iOS.
- CORS posture — already `cors()` open; no change.
- Rate-limit buckets in `rate-limiters.ts` — keyed by IP and attested-key; no platform-specific limits in this round (revisit in T3 if Android volume warrants).
- Existing prod legacy-key rejection (`BACKEND_ENV=prod` disables `x-backend-key`) — still applies to Android in prod.

**Verification:** all new tests green; existing iOS tests still green unmodified; manual end-to-end check from both a Test iPhone 14 (App Attest path) and a Pixel 9a (Play Integrity path) hitting the same dev backend in sequence and both succeeding.

### T1.10 iOS Non-Regression Guarantees (Backend)

The backend extensions for Android are accepted **only if iOS is provably not regressed**. This criterion is a release blocker — any failure of T1.10 blocks deploy of the Android-supporting backend to prod, even if every other criterion passes. The rule is asymmetric: when in doubt, the iOS app wins.

**T1.10.a — Existing iOS test suite passes unmodified.** Every test currently under `Backend/test/` (counted at the SHA the porting work branched from) must remain at its pre-port content and pass against the post-port server. If a test is "improved" or "cleaned up" during the porting PR, the PR is rejected — that change belongs in a separate PR on iOS-only grounds. Deletions or edits to existing tests require a written justification in the PR description.

**T1.10.b — In-field iOS clients (no `X-App-Platform` header) still work.** Real iOS builds already installed on users' phones predate this work and will never send `X-App-Platform`. The dispatch logic must treat a missing header as `ios`, exercise the App Attest path, and produce a response indistinguishable from the pre-port server.

Full byte-for-byte replay of a captured iOS request is **not** feasible — App Attest assertions are bound to a monotonic counter and short-lived challenge, so a captured request fails verification when replayed later regardless of server-side changes. T1.10.b therefore decomposes into two complementary tests:

1. **Structural replay (`Backend/test/legacy-ios-no-platform-header.test.ts`).** Replay a captured pre-port iOS request (verbatim bytes, no `X-App-Platform`) against the post-port server **with App Attest assertion validation stubbed to always-accept for the test**. Assert: same status code, same response body shape and field set, same response headers iOS depends on (`Content-Type`, `Retry-After`, any custom `X-*` iOS reads). The stub isolates the question this test is meant to answer — "did our dispatch/routing/serialization changes regress iOS?" — from the question App Attest already answers.

2. **Live equivalence (`Backend/test/contract/ios/live-equivalence.md` runbook).** A real Test iPhone 14 makes a fresh request against the *pre-port baseline server* (`watchmycalories-backend-dev` pinned at the baseline Cloud Run revision) and against the *post-port server* (same dev URL after deploy). Capture both responses. Assert response-shape equivalence with a diff that ignores time-varying fields (challenge tokens, timestamps, request IDs). Both flows must succeed end-to-end including App Attest verification.

The capture for test 1 is a one-time pre-flight artifact: run a Test iPhone 14 against the current server with mitmproxy/Charles in trusted-CA mode, save the request as `Backend/test/contract/ios/captured-attest-verify-request.bin` and `captured-gemini-request.bin` (with sensitive fields like keyID redacted to placeholders the test substitutes back in). **This capture happens before any backend porting code is committed** — otherwise the baseline is contaminated.

**T1.10.c — Contract snapshot.** Pin the iOS-facing wire contract as committed JSON fixtures under `Backend/test/contract/ios/`:
- `attest-challenge.response.json`
- `attest-verify.request.json` / `attest-verify.response.json`
- `gemini-generate-content.request.json` / `gemini-generate-content.response.json`
- `rate-limit-429.response.json` (with `Retry-After` header captured)
A snapshot test asserts the live server still produces these shapes. Snapshot updates require explicit reviewer sign-off and a note in `Backend/CHANGELOG.md` — an "innocent" snapshot bump in a porting PR is a red flag, not a routine fix.

**T1.10.d — Firestore schema backward-compatibility.** The `platform` field added in T1.9.c is optional/nullable on read; iOS docs already written before the field existed must read back successfully and be treated as `platform === 'ios'`. No backfill migration is run. Adding required fields, renaming existing fields, or changing field types on the existing iOS doc shape is forbidden.

**T1.10.e — Env-var backward-compatibility.** All new env vars in T1.9.e have safe defaults when absent: if `PLAY_INTEGRITY_PROJECT_ID` / `PLAY_INTEGRITY_PACKAGE_NAME` / Play Integrity creds are missing, the server still boots and serves the iOS App Attest path normally — Android attest requests fail cleanly with a specific 5xx, iOS is unaffected. A test (`Backend/test/boot-without-play-integrity-env.test.ts`) asserts this. (Rationale: lets us deploy the server upgrade and the secret rollout independently without coupling.)

**T1.10.f — Staged rollout to dev before prod.** The Android-supporting backend deploys first to `watchmycalories-backend-dev`. iOS dev builds (debug) must exercise the dev server for at least one engineer-day with no regression observed before `./deploy.sh prod` is run. The prod deploy PR description must reference the dev burn-in window and the commit SHA the iOS dev build was tested at.

**T1.10.g — Rollback plan.** The pre-Android backend image tag is recorded in the prod deploy PR description so it can be redeployed in one command if a regression is found. Cloud Run revision-pinning is sufficient; no manual rebuild required.

**T1.10.h — Performance non-regression on the iOS path.** Adding header inspection, an Android dispatch branch, and a new Firestore field must not measurably slow the iOS path. p50 latency of `/attest/verify` and `/v1beta/models/default:generateContent` on iOS requests must stay within ±10% of the pre-port baseline, measured against the dev backend. Out-of-band acceptable if the cause is identified (e.g., cold-start regression from a new heavy dep — flag and decide).

**Verification:** all sub-criteria a–h have concrete artifacts (named test files, committed fixtures, PR-description fields). The reviewer's job in T1.10 is a literal checklist walk-down; if any artifact is missing, T1.10 is FAIL.

## Tier 2 — Visual & Behavioral Parity

### T2.1 Visual Diff Budget

Side-by-side screenshots, captured at the same logical pixel dimensions per screen.
- Color palette: max ΔE 2000 of ~3 between corresponding tokens.
- Corner radii within ±2pt (24pt cards, 12–16pt buttons).
- Vertical rhythm within ±4pt at each section boundary.
- Typography hierarchy matches (title → headline → body → caption); exact fonts differ (system fonts per platform) but **weights and relative sizes** must match.

**Verification:** screenshot pairs for each screen in primary state plus key alternates (empty, loading, error).

### T2.2 Interaction Parity

- Camera capture: equivalent haptic/audible feedback intent.
- Pull-to-refresh: present on the same screens (Dashboard, History) if iOS has it; otherwise neither.
- Long-press / swipe-to-delete: gesture set matches iOS on entry rows.
- Modal sheet behavior: backdrop-tap-dismiss matches iOS.

### T2.3 State Parity Matrix

For each screen, all reachable and rendered:

| State | Trigger |
|---|---|
| Empty | Fresh install, no entries |
| Loading | In-flight Gemini call (must show native ad on EstimationReview / MenuAnalysis) |
| Error — network | Airplane mode mid-call |
| Error — rate limit | Mocked 429 with `Retry-After` |
| Error — invalid JSON | Mocked malformed response |
| Error — not-a-menu | Mocked sentinel |
| Permission denied — camera | OS-level deny |
| Permission denied — health | OS-level deny |
| AI consent off | Toggle in Settings, then try camera |
| Offline + cached data | Open app with prior entries while offline |

### T2.4 Accessibility Parity

- Android must expose the same identifiers as iOS's `AccessibilityIdentifiers.swift` (via `testTag` or `contentDescription`) so cross-platform UI tests can share assertions. The Android-side registry already exists at `utils/AccessibilityTags.kt` — extend it; do not introduce a parallel constants file.
- The identifier set in `AccessibilityTags.kt` must be a strict superset-or-equal of `AccessibilityIdentifiers.swift`. A diff test in CI is acceptable evidence.
- System text scaling must not clip on any screen iOS supports.
- TalkBack labels equivalent to VoiceOver labels (e.g., HeroSummaryCard reads "consumed N calories of M target, P remaining" or equivalent on both).

### T2.5 Ad Parity

- Banner ad placement on the same screens (Dashboard, History, LogFoodSheet, ManualEntry).
- Non-personalized only, with consent posture matching iOS.
- Native ad rendered during Gemini analysis on both `AnalysisScreen` and `MenuAnalysisScreen` (Android currently has `NativeAdView.kt` as a component but does not wire it into any screen — this is the porting task).

## Tier 3 — Aspirational

- Crash rate ≤ iOS release crash rate over 7 days post-port.
- Cold-start within 1.25× iOS baseline on Pixel 9a.
- 60fps minimum on the transitions iOS achieves.
- Real test coverage above boilerplate: unit tests for `CalorieCalculator`, `NutritionCalculator`, `GeminiParser`, `MealType.fromHour`; one happy-path integration test per major flow.

## Process

1. **Per-screen parity matrix** (`PORTING_MATRIX.md` or equivalent) — one row per iOS screen × Tier 1+2 criteria, binary pass/fail with evidence link. Live document, owned by the porting engineer.
2. **Deviation Registry** (`PORTING_DEVIATIONS.md`) — every documented divergence with iOS behavior, Android behavior, rationale, sign-off. No undocumented divergences.
3. **Golden-vector fixtures** under `shared-fixtures/` for numeric logic, consumed by both test suites.
4. **Two-pass review:**
   - Pass 1 (engineer): walk the matrix, fill in evidence.
   - Pass 2 (reviewer): independently walk the matrix on a real Pixel 9a + Test iPhone 14, rejecting any criterion where the evidence is not reproducible.
5. **Sign-off gate:** Tier 1 = 100%, Tier 2 ≥ 95% with every gap in the Deviation Registry, no open visual diff exceeding the budget.

## Known Gaps to Close (this round)

Audited against `WatchMyCaloriesAndroid/` on the current branch. Each gap is paired with the criterion it violates. Status snapshot as of 2026-05-16.

1. **~~Re-architect Android Gemini integration to use the Cloud Run backend.~~** [T1.5] ✅ **Done.** SDK dependency dropped, hardcoded keys removed, `ai/GeminiRepository.kt` is an OkHttp client POSTing to the backend. Enforced by `scripts/check-android-no-gemini-sdk.sh`.

2. **Make Play Integrity actually verify on the backend.** [T1.8, T1.9, gated by T1.10] **Code complete; pending IAM grant + deploy.**
   - **Server-side ✅:** `X-App-Platform` dispatch (T1.9.a), `Backend/src/play-integrity.ts` (T1.9.b), Firestore `platform` + `androidAssertionSecret` fields (T1.9.c), `Backend/src/verify-request-android.ts` per-request HMAC verifier (T1.9.d), env-vars wired through `deploy.sh` and Application Default Credentials via the runtime SA (T1.9.e — no JSON key), `google-auth-library` dep added (T1.9.f), tests `play-integrity-verify.test.ts` / `verify-request-android.test.ts` / `platform-dispatch.test.ts` / `boot-without-play-integrity-env.test.ts` / `firestore-key-schema-additions.test.ts` all green (T1.9.g, T1.10.e), every existing iOS-path test still green (T1.9.h).
   - **Client-side ✅:** `security/PlayIntegrityManager.kt` attests on first use, persists `(keyID, secret, counter)` in EncryptedSharedPreferences, attaches `X-Android-Key-Id` / `X-Android-Counter` / `X-Android-Assertion` per request via `assertionHeaders(context, bodyBytes)`. `GeminiRepository` adds them, falls back to dev-only legacy `x-backend-key`, and re-attests on 401 `android_assertion_invalid`.
   - **Still open:** the `roles/playintegrity.user` IAM grant on the runtime SA (gated on Play Console linking the Cloud project) and an actual `./deploy.sh dev` run + Pixel-9a end-to-end verification.

3. **~~Photo Library: port the pre-analysis review step.~~** [T1.1] ✅ **Done.** `ui/photolib/PhotoLibraryReviewScreen.kt` with EXIF auto-detected meal type, `MealTypePicker`, and `CalorieDisclaimerSheet`; wired as the `photoLibraryReview` route between picker and analysis.

4. **~~Image persistence on Android.~~** [T1.4] ✅ **Done.** `data/ImageStorage.kt` writes `filesDir/{imageID}.jpg` (JPEG quality 80 — `security/JpegConfig.kt` keeps the constant single-sourced) at Save time in both `AnalysisScreen` and `MenuAnalysisScreen`. Force-stop persistence check still needs a Pixel 9a.

5. **~~Native ad in `AnalysisScreen` / `MenuAnalysisScreen` loading state.~~** [T2.5] ✅ **Done.** `NativeAdView()` renders inside the `Loading` branch of `ui/analysis/AnalysisScreen.kt:115` and `ui/menuscanner/MenuAnalysisScreen.kt:130`.

6. **~~Use the unit-system parameter that's already there.~~** [T1.5] ✅ **Resolved on re-audit.** `isMetric` is threaded from `SettingsDataStore.isMetricFlow` through `MainActivity` to both analysis screens; prompt adapts. Kept here as a regression guard.

7. **~~Settings row-for-row audit and label alignment.~~** [T1.6] ✅ **Done.** Theme labels canonical ("System / Light / Dark" — `SettingsScreen.kt:139`), unit toggle canonical ("US Customary / Metric" — `:153`), AI Consent row present and gating, and `BannerAdView` ported (2026-05-16) above the App Appearance section to match iOS `SettingsView.swift:51`. Last residual is a visual-parity spot-check on a real Pixel.

8. **~~Data model field-name and type drift.~~** [T1.2] ✅ **Done.** Android renamed `imageId` → `imageID` and `itemsJson` → `itemsData`. Singleton `UserProfile.id` Room requirement documented as deviation `D-001` in `PORTING_DEVIATIONS.md`. `scripts/schema-diff.sh` enforces parity with the deviation allowlist.

9. **~~CLAUDE.md is stale.~~** [Operating Principle 6] ✅ **Done.** Android section rewritten 2026-05-16: removed Google AI SDK / Hilt / `imageId` / `itemsJson` / stale `local.properties` mentions; describes the OkHttp + Play Integrity + EncryptedSharedPrefs + Image Storage layout that actually exists; new "Cross-cutting porting gates" section lists the three bash gates + npm test + `./gradlew testDebugUnitTest`.
