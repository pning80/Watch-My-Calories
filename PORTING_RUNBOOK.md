# Porting Runbook: iOS → Android

Executable staging plan for the Android port. The authoritative rubric is `PORTING_CRITERIA.md`; this file says **what order to do it in** and **what has to be true before each stage starts**.

The stages are strictly dependency-ordered. A stage may not start until every "Prerequisites" item under it is true; a stage may not be declared done until every "Exit gate" item is evidenced.

---

## Stage 0 — Pre-flight (do first, do once)

Scope: locked-in decisions, scaffolding, baselines, external enrollment. No porting code lands in this stage.

### 0.1 — Documentation scaffolding

- [x] `PORTING_CRITERIA.md` written and amendments locked in (per-request integrity protocol, T1.10.b realism).
- [x] `CLAUDE.md` Android section reflects actual file paths.
- [x] `PORTING_RUNBOOK.md` (this file) committed.
- [x] `PORTING_DEVIATIONS.md` scaffolded.
- [x] `PORTING_MATRIX.md` scaffolded.
- [x] `shared-fixtures/` directory scaffolded with README.
- [x] `Backend/test/contract/ios/` directory scaffolded with README.
- [x] `Backend/test/helpers/` directory scaffolded with README.

### 0.2 — External enrollment (human-paced, blocks Stage 1)

**Design note:** authentication uses Application Default Credentials via the existing Cloud Run runtime service account. **No JSON key is downloaded or stored anywhere.** Play Integrity access is granted implicitly by (a) linking the Cloud project in Play Console + (b) enabling `playintegrity.googleapis.com` on that project — no separate GCP IAM role binding is required. The backend's existing Firestore/Secret-Manager auth pattern handles the rest.

Human-paced (Play Console UI; identity verification takes 2–14 days):
- [x] Create a Google Play developer account ($25, https://play.google.com/console/signup). Identity verification gates everything else here. **Done (user-confirmed 2026-05-28).**
- [x] In Play Console, create an app entry. Set the package name to exactly `com.pning80.watchmycalories` (case matters; cannot be changed later). No build upload required at this stage. **Done (user-confirmed 2026-05-28).**
- [x] In Play Console → app → **Test and release** → **App integrity** → **Play Integrity API** tab → **Link Cloud project**. Pick `gen-lang-client-0629636941` (project number `657698311127`). **Done (user-confirmed 2026-05-28).**

Machine-paced (gcloud — I can do these autonomously once the Play Console steps above are done; some can be done in parallel):
- [x] Enable the Play Integrity API on the GCP project: `gcloud services enable playintegrity.googleapis.com --project=gen-lang-client-0629636941`.
- [x] ~~Grant `roles/playintegrity.user` to the runtime SA~~ — **N/A. Verified 2026-05-28**: this role does not exist in GCP IAM (`gcloud iam roles list --filter="name~playintegrity"` returns empty; explicit `roles/playintegrity.user` describe returns "not found"; `add-iam-policy-binding` with it errors `INVALID_ARGUMENT: Role roles/playintegrity.user is not supported for this resource`). Per [Google's Standard API docs](https://developer.android.com/google/play/integrity/standard), Play Integrity access is granted implicitly by the Play-Console ⇄ Cloud-project link + the `playintegrity.googleapis.com` enablement. The runtime SA just needs the `https://www.googleapis.com/auth/playintegrity` OAuth scope, which `Backend/src/play-integrity.ts:131-132` already requests via ADC.
- [x] Update `Backend/deploy.sh` to pass `PLAY_INTEGRITY_PROJECT_NUMBER=657698311127` and `PLAY_INTEGRITY_PACKAGE_NAME=com.pning80.watchmycalories` as env vars to Cloud Run. **Done.** The values are set with shell-defaults at the top of `deploy.sh` so a deploy works even before `.env.*` are updated.

Known values to bake in (already true today):
- GCP project ID: `gen-lang-client-0629636941`
- GCP project number: `657698311127`
- Cloud Run runtime SA: `watchmycalories-backend@gen-lang-client-0629636941.iam.gserviceaccount.com`
- Android package name: `com.pning80.watchmycalories`

### 0.3 — Baseline capture (do before any backend code lands)

**Status (2026-05-28): complete.** All fixtures captured against pre-port revision
`watchmycalories-backend-dev-00016-xmw` via a brief rollback (currently serving
`00017-xvk`). Pre-port↔post-Stage-1 head-to-head diff recorded in
`Backend/test/contract/ios/STAGE_1_DIFF.md` — iOS-facing wire contract is byte-identical.
Helper scripts under `Backend/scripts/` (`capture-429.sh`, `capture-latency.sh`,
`redact-fixture.sh`, `mitm-capture.py`).

- [x] Capture pre-port iOS request samples from a Test iPhone 14 against `watchmycalories-backend-dev-00016-xmw`:
  - `Backend/test/contract/ios/attest-challenge.response.json` ✓
  - `Backend/test/contract/ios/attest-verify.{request,response}.json` ✓ (keyID/challenge/attestation redacted to `__PLACEHOLDERS__`)
  - `Backend/test/contract/ios/gemini-generate-content.{request,response}.json` ✓ (image bytes + `thoughtSignature` + `responseId` redacted)
  - `Backend/test/contract/ios/rate-limit-429.response.json` ✓ (`Retry-After: 563` preserved in `_meta`)
- [x] Capture verbatim binary requests for T1.10.b structural replay → **captured and used at-evidence-time, intentionally not committed.** The 5.8 MB `captured-gemini-request.bin` embeds a real JPEG and the 8 KB `captured-attest-verify-request.bin` embeds a real iPhone App Attest blob; both were used to validate the pre↔post-Stage-1 byte-shape diff (see `Backend/test/contract/ios/STAGE_1_DIFF.md`). The structural-replay test (T1.10.b proper) does not yet exist; when it lands, captures will be regenerated via `Backend/scripts/mitm-capture.py` and either downloaded from artifact storage at CI time or synthesized at test setup time (deterministic, no real device data). Method for the in-session capture: Test iPhone 14 + mitmproxy with passthrough for `*.apple.com:443` and `*.icloud.com:443` (DCAppAttestService uses OS-level cert pinning that breaks under MITM without passthrough).
- [x] Capture iOS-path p50 latency baseline → `Backend/test/contract/ios/baseline-latency.md` (revision `watchmycalories-backend-dev-00016-xmw` pinned). N=30 (gemini rate-limit cap is 100/window; pre-port + post-deploy capture both fit in budget). Results: `/attest/challenge` p50=107ms p95=130ms, `/v1beta/models/default:generateContent` p50=1229ms p95=1612ms. **Post-deploy re-capture deferred** until the Mac's rate-limit window resets (~15 min after last call); see `STAGE_1_DIFF.md`.
- [x] **Record the pre-port Cloud Run revision tags for rollback (T1.10.g).** Captured 2026-05-16:
  - **prod** `watchmycalories-backend-00010-pxt` (deployed 2026-05-16T22:30:12Z, 100% traffic; image digest `sha256:d0def109667ee290d0dfeb187846f333939052139b88fd9f61884c4d186b2035`).
    - Deeper-rollback safety: previous revision was `watchmycalories-backend-00009-59r` (2026-03-26T20:01:11Z, digest `sha256:5f23820e16e81d46e539b695d8c1dc136c1d923d9d4dd7ab1222667f5ce0538f`). Use this if 00010-pxt turns out to be a porting-touched build.
  - **dev** `watchmycalories-backend-dev-00016-xmw` (deployed 2026-05-16T22:28:23Z). Predecessor for deeper rollback: `watchmycalories-backend-dev-00015-ntd` (2026-03-26T21:11:38Z).
  - One-line rollback (prod): `gcloud run services update-traffic watchmycalories-backend --region us-central1 --to-revisions=watchmycalories-backend-00010-pxt=100 --project gen-lang-client-0629636941`.

### 0.4 — Locked design decisions (already in criteria; recorded here for the executor)

- **Per-request integrity for Android (T1.8):** HMAC-SHA256 over `counter || ":" || sha256(body)` with a per-key 32-byte secret issued at `/attest/verify`. Headers: `X-Android-Key-Id`, `X-Android-Counter`, `X-Android-Assertion`. Replay-protection via monotonic counter persisted in Firestore key doc.
- **Play Integrity authentication (T1.9.e):** Application Default Credentials via the existing Cloud Run runtime SA. **No JSON key file.** Backend grants the runtime SA `roles/playintegrity.user`; no secret-manager entry needed for these creds.
- **Platform discriminator (T1.9.a):** HTTP header `X-App-Platform: ios | android` (reuses the existing iOS header from `Services.swift:152` — no iOS app change). Missing header defaults to `ios`.
- **Firestore key collection (T1.9.c):** Single collection `attestedKeys-{env}` (no per-platform split). New nullable field `platform: 'ios' | 'android'`. New nullable field `androidAssertionSecret: string (hex)`. iOS docs without these fields read as `platform === 'ios'`, `androidAssertionSecret === undefined`.
- **MenuScan items wire format (T1.2):** Android side uses `String` with Room `TypeConverter` (preserves JSON debuggability in Room inspector). Field name `itemsData` to match iOS exactly.
- **JPEG encode quality (T1.4):** match iOS encode params. Verify by reading the iOS encode call site (`CameraManager.swift` / `EstimationReviewView.swift`) and using the integer-quantized Android equivalent. Lock the value in `data/Entities.kt` header comment when implementing T1.4.

### Stage 0 exit gate

All 0.1, 0.3 boxes checked; 0.2 fully complete; the executor of Stage 1 can read this file and not need to ask any new design questions.

---

## Stage 1 — Backend (blocks Stage 2)

Scope: implement everything in `PORTING_CRITERIA.md` T1.9 (T1.9.a through T1.9.h), gated by T1.10 non-regression checks. No client-side changes in this stage; iOS-facing wire contract preserved byte-for-byte.

**Status (2026-05-16): code-complete; deploy + burn-in pending.** Steps 1–8 below are all merged. The `Backend/` test suite is at 201/201 green with the platform dispatcher, Play Integrity verifier (with injectable decoder), Android per-request HMAC verifier, Firestore schema additions, and boot-without-Play-Integrity-env safety test all wired. What's still open: actually running `./deploy.sh dev`, the one-engineer-day iOS burn-in, and the device-backed end-to-end check from a Test iPhone 14. Deploy can land before `roles/playintegrity.user` is granted — the Android path returns 503 `play_integrity_not_configured` (already covered by tests) without affecting iOS.

### Prerequisites

- Stage 0 complete (especially 0.2 credentials and 0.3 baselines).

### Order of operations (within Stage 1)

1. Add `X-App-Platform` header inspection on `/attest/challenge` and `/attest/verify` with `'ios'` default; iOS path unchanged. (T1.9.a)
2. Add `platform` field write to Firestore key doc on iOS path (always `'ios'`); read code treats missing field as `'ios'`. (T1.9.c, T1.10.d)
3. Add the contract snapshot test from the fixtures captured in 0.3 to the existing test suite. (T1.10.c)
4. Add `Backend/test/legacy-ios-no-platform-header.test.ts` (T1.10.b structural replay; App Attest validation stubbed for this test only).
5. Add `Backend/test/boot-without-play-integrity-env.test.ts` — server boots and serves iOS path normally when Play Integrity env vars absent. (T1.10.e)
6. Land all of the above as one PR titled `backend: prep for Android (iOS non-regression)`. Run the full existing iOS test suite; all must pass unmodified. Deploy to dev. Burn-in iOS dev build for one engineer-day (T1.10.f).
7. **Second PR:** Implement the Play Integrity verifier in `Backend/src/play-integrity.ts` (T1.9.b). Wire the Android branch on `/attest/verify` to call it. Add `androidAssertionSecret` issuance on successful Android attest (T1.8 protocol step 1). New tests `play-integrity-verify.test.ts`, `platform-dispatch.test.ts` (T1.9.g).
8. **Third PR:** Implement `Backend/src/verify-request-android.ts` (T1.9.d) and gate `/v1beta/models/*` with it for Android requests. New test `verify-request-android.test.ts`. Counter persistence atomic via Firestore transaction.

### Exit gate

- All T1.9 sub-criteria pass.
- All T1.10 sub-criteria pass — every iOS test still green unmodified, contract snapshots match, latency within ±10%.
- Dev backend deployed; one engineer-day iOS dev-build burn-in observed with no regression.
- Pre-port prod revision tag recorded in the deploy PR description for one-command rollback (T1.10.g).
- A fresh Test iPhone 14 build hits dev and works end-to-end.

---

## Stage 2 — Android wire-to-backend (blocks Stage 3)

Scope: re-architect the Android Gemini path through the backend; wire `PlayIntegrityManager` and the per-request HMAC; remove Gemini SDK and API-key handling from the client. Implements `PORTING_CRITERIA.md` T1.5 + T1.8 client side + the data-model rename gated in Known Gap #8.

**Status (2026-05-16): code-complete; device verification pending.** Steps 1–6 below are all merged: SDK removed (enforced by `scripts/check-android-no-gemini-sdk.sh`), `ai/GeminiRepository.kt` is an OkHttp client with `X-App-Platform: android` + per-request HMAC headers + 401 re-attest retry, `imageId`/`itemsJson` renamed (Android-only), `data/ImageStorage.kt` writes `filesDir/{imageID}.jpg` at Save time, and the CI grep gate runs on every PR. What's still open: the force-stop and tampered-body checks on a real Pixel 9a, which need Stage 1's dev deploy first.

### Prerequisites

- Stage 1 complete and deployed to dev.
- An engineer has confirmed end-to-end Android `/attest/verify` against dev with a real Pixel 9a and a synthetic Play Integrity token.

### Order of operations

1. **Remove the Gemini SDK and the API-key hardcodes** (Known Gap #1):
   - Drop `com.google.ai.client.generativeai:generativeai:0.9.0` from `app/build.gradle.kts`.
   - Remove `"YOUR_API_KEY"` literals at `security/BackendConfig.kt:9` and `MainActivity.kt:93`.
   - Remove `BuildConfig.GEMINI_API_KEY` plumbing and the EncryptedSharedPreferences slot for the Gemini key.
2. **Rewrite `ai/GeminiRepository.kt`** to use an HTTP client (OkHttp preferred — matches Android conventions) POSTing base64 images to `{baseURL}/v1beta/models/default:generateContent`. Mirror iOS `Services.swift` request construction. Thread `isMetric` from `SettingsRepository` through ViewModels to the call site.
3. **Wire `PlayIntegrityManager`** to:
   - Call `/attest/challenge` then `/attest/verify` on first use, persist the returned `androidAssertionSecret` and keyID in EncryptedSharedPreferences.
   - On every Gemini call, attach `X-Android-Key-Id`, `X-Android-Counter` (monotonic from EncryptedSharedPreferences), `X-Android-Assertion` (HMAC-SHA256(secret, counter || ":" || sha256(body))).
   - Add emulator fallback: if Play Integrity is unsupported, fall back to legacy `x-backend-key` in dev only (mirroring iOS `AppAttestManager`).
   - On 401 with `{"error":"android_assertion_invalid"}`, re-attest and retry once.
4. **Rename data-model fields** (Known Gap #8, Stage-2 gated):
   - `data/Entities.kt:29` and `:38` — rename `imageId: String?` → `imageID: String?` on `FoodEntry` and `MenuScan`.
   - `data/Entities.kt:40` — rename `itemsJson: String` → `itemsData: String` on `MenuScan` (keep Room storage as JSON `String`; field name only to match iOS).
   - **Schema migration:** `AppDatabase.kt:8` is at `version = 2` with `fallbackToDestructiveMigration()` set. Bump to `version = 3`. No real migration code needed — Room will drop and recreate tables. Acceptable because the Android app is pre-launch; document the wipe in the PR description. (When the app ships to Play Store, remove `fallbackToDestructiveMigration` and write real migrations from that point forward.)
   - **Call-site inventory (every file that references the old names — update all in one PR):**
     - `MainActivity.kt:267, 347, 392, 393, 395, 396, 397`
     - `ui/entry/ManualEntryScreen.kt:71`
     - `ui/dashboard/DashboardScreen.kt:146, 169, 176, 183` (comments and field reads)
     - `ui/menuscanner/MenuAnalysisScreen.kt:88, 90` (`imageId =`, `itemsJson =`)
     - `ui/menuscanner/MenuScanDetailScreen.kt:33, 35` (`scan.itemsJson`)
     - `ui/history/HistoryScreen.kt:182, 190, 218, 225, 232`
   - `./gradlew test` and `./gradlew lint` must pass after the rename. If any DAO has a `@Query` referencing the old column name, update the SQL too.
5. **Image persistence (T1.4):** wire `ui/camera/CameraScreen.kt` and the analysis Save handler to write the captured `Bitmap` to `filesDir/{imageID}.jpg` at Save time (not at capture time). Set `FoodEntry.imageID` to the same `UUID.toString()` used in the filename. Render thumbnails in History from disk.
6. **CI grep gates (T1.5):** add a GitHub Actions workflow (or shell script in `Backend/scripts/` if no CI yet) that fails on `generativeai`, `"YOUR_API_KEY"`, or `GenerativeModel(` under `WatchMyCaloriesAndroid/app/src/`.

### Exit gate

- App builds, installs, runs on Pixel 9a.
- One full camera-to-saved-entry flow succeeds end-to-end against `watchmycalories-backend-dev`.
- Force-stop test (T1.4 verification): create entry → `adb shell am force-stop com.pning80.watchmycalories` → reopen → thumbnail still renders.
- Tampered-body test (T1.8 verification): manually corrupt the request body in a debug build → server returns 401 → client re-attests → retry succeeds.
- CI grep gate green.
- iOS dev build still works against dev backend (re-check; nothing in this stage should have touched the iOS path, but verify).

---

## Stage 3 — Android port polish (parallelizable within Stage 3)

Scope: visible-to-user parity items that don't depend on the backend rewrite. May be split across multiple PRs and worked on in parallel by multiple engineers/agents.

### Prerequisites

- Stage 2 complete (so the Gemini path is real and the rest can be tested end-to-end).

### Work items (each is its own PR)

- **3.1 — ~~Photo Library pre-analysis review step~~** (Known Gap #3 / T1.1). ✅ **Done.** Ported as `ui/photolib/PhotoLibraryReviewScreen.kt` with supporting `MealTypePicker.kt` and `CalorieDisclaimerSheet.kt`. Wired into `MainActivity.kt` as the `photoLibraryReview` route between the picker callback and `analysis`. EXIF date extraction uses `androidx.exifinterface`. One-time disclaimer state persisted in `SettingsDataStore.hasSeenEstimateDisclaimerFlow`. User-chosen `MealType` flows through to `FoodEntry.mealTypeRaw` at Save time.
- **3.2 — ~~Native ad in Analysis + MenuAnalysis loading state~~** (Known Gap #5 / T2.5). ✅ **Done.** `NativeAdView()` renders inside the `Loading` branch of both `ui/analysis/AnalysisScreen.kt` and `ui/menuscanner/MenuAnalysisScreen.kt`.
- **3.3 — Settings row-for-row alignment** (Known Gap #7 / T1.6). Theme labels already canonical ("System / Light / Dark" — `SettingsScreen.kt:139`). Unit toggle already canonical ("US Customary / Metric" — `SettingsScreen.kt:153`). AI Consent row exists (`SettingsScreen.kt:329–344`) and `MainActivity` gates camera/scan entry on it. Settings `BannerAdView` ported 2026-05-16 to match iOS `SettingsView.swift:51`. **Done** pending one visual-parity spot-check.
- **3.4 — ~~Accessibility identifier parity~~** (T2.4). ✅ **Done.** `utils/AccessibilityTags.kt` is a strict superset of `AccessibilityIdentifiers.swift` (65 iOS IDs + 6 Android-only test-convenience extras), enforced by `scripts/accessibility-diff.sh`. Per-screen `Modifier.testTag(...)` wiring landed in Phase A of the autonomous run (2026-05-16) — all bottom-nav tabs, onboarding flow, settings pickers, ads, camera, ManualEntry, About, Dashboard, History, Estimation/Menu analysis screens, and PhotoLibraryReview use the canonical constants.
- **3.5 — State parity matrix** (T2.3). Walk every screen × every state row; produce screenshots; record in `PORTING_MATRIX.md`.
- **3.6 — Visual diff budget** (T2.1). Capture screenshot pairs; verify color ΔE, corner-radius, vertical-rhythm budgets.
- **3.7 — ~~Real test coverage~~** (was T3 aspirational, now Stage 3 work). ✅ **Substantially done.** Unit tests cover `CalorieCalculator` (golden vectors + monotonic-multiplier + gender-flip + shared-fixture-driven suite), `NutritionCalculator` (11 cases incl. fallback + case-insensitivity + OTHER gender), `GeminiParser` (3 happy-path + 16 degradation-contract edge cases), and `MealType` (every hour 0..23 + shared-fixture-driven suite). Cross-platform parity tests load `shared-fixtures/{bmr-mifflin-st-jeor,meal-type-by-hour}/cases.json` via `app/src/test/resources/` so the iOS suite consumes the same JSON once wired. `GeminiRepository` HTTP-mock tests deliberately deferred — Robolectric + Bitmap stubbing isn't worth the cost when the parser surface is already covered.

### Exit gate

- `PORTING_MATRIX.md` shows Tier 1 = 100% pass, Tier 2 ≥ 95% pass.
- Every Tier 2 gap is recorded in `PORTING_DEVIATIONS.md` with rationale and sign-off.
- No open visual diff exceeding budget.

---

## Stage 4 — Production cutover

Scope: promote the dev-validated backend to prod; ship the Android app to internal/closed testing.

### Prerequisites

- Stage 3 exit gate met.
- Dev burn-in window ≥ 1 engineer-day with no regression.

### Order of operations

1. Run `./deploy.sh prod`. PR description references the pre-port prod revision tag (recorded in 0.3) and the dev burn-in observation.
2. Watch error rates and Cloud Run latency dashboards for 1 hour post-deploy on the iOS path specifically.
3. If iOS regression detected: redeploy the pinned pre-port revision (T1.10.g). Investigate before re-attempting.
4. Promote the Android build to Play Console internal testing.

### Exit gate

- Prod backend serves both platforms.
- iOS crash rate, latency, and error rate within ±10% of pre-deploy baseline over 24 hours.
- Android internal-testing track green for ≥ 3 days with no Sev-2+ issues.

---

## External-dependency status checklist

Use this section to track what's blocking. Update as items complete.

| Item | Owner | Status |
|---|---|---|
| Google Play developer account ($25, identity verification 2–14 days) | you | **done** (2026-05-28) |
| Play Console app entry for `com.pning80.watchmycalories` | you | **done** (2026-05-28) |
| Play Console → app → App integrity → Link Cloud project to `gen-lang-client-0629636941` | you | **done** (2026-05-28) |
| Play Integrity API enabled on GCP project | agent | **done** (2026-05-16) |
| ~~`roles/playintegrity.user` granted to runtime SA~~ — N/A; not a real GCP role, access granted via Play-Console link + API enablement | — | **n/a** (verified 2026-05-28) |
| `PLAY_INTEGRITY_PROJECT_NUMBER` / `PLAY_INTEGRITY_PACKAGE_NAME` wired into `deploy.sh` | agent | **done** (2026-05-16) |
| Pre-port prod + dev Cloud Run revision tags recorded for T1.10.g rollback | agent | **done** — see Stage 0.3 |
| Test iPhone 14 available for baseline capture (0.3) | you | **done** (2026-05-28) |
| mitmproxy / Charles set up with trusted CA for iOS capture | you | **done** (2026-05-28) — note: requires `--set ignore_hosts='(.*\.)?apple\.com:443\|(.*\.)?icloud\.com:443'` for DCAppAttestService to function |
| Pixel 9a available for Android end-to-end | you | open |
| `./deploy.sh dev` actually run with the porting-touched backend | you (or agent on your go-ahead) | open — Stage 1 code is in; iOS path is untouched, Android path returns 503 cleanly until IAM grant lands |
