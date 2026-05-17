# Pixel 9a Verification Runbook (Stage 2 exit gate)

Hands-on verification of the Android port against `watchmycalories-backend-dev`
on a real Pixel 9a (the test device named in `CLAUDE.md`). Closes the four
device-blocked items in `PORTING_RUNBOOK.md` Stage 2 exit gate.

**Prerequisites (must all be true before starting):**

- [ ] `./deploy.sh dev` has been run — the dev Cloud Run revision is
      `watchmycalories-backend-dev-NNNNN-xxx` where `NNNNN` ≥ 16 **and** the
      revision was built from a tree containing the Stage 1 backend commit
      `7ff3731` (Backend: dual-platform support). Verify with
      `gcloud run revisions describe watchmycalories-backend-dev-NNNNN-xxx
      --region us-central1 --project gen-lang-client-0629636941 | grep image`.
- [ ] `roles/playintegrity.user` has been granted to
      `watchmycalories-backend@gen-lang-client-0629636941.iam.gserviceaccount.com`
      (per Stage 0.2). Verify with:
      ```bash
      gcloud projects get-iam-policy gen-lang-client-0629636941 \
        --flatten="bindings[].members" \
        --filter="bindings.role:roles/playintegrity.user AND bindings.members:watchmycalories-backend@*" \
        --format="value(bindings.members)"
      ```
      Should print one match. If empty, the backend will return 503
      `play_integrity_not_configured` and this runbook cannot proceed.
- [ ] Pixel 9a is on Wi-Fi reachable from the dev backend URL
      `https://watchmycalories-backend-dev-657698311127.us-central1.run.app`
      (any public Wi-Fi will do).
- [ ] `adb` is installed on the Mac and the Pixel is in developer mode with
      USB debugging enabled (Settings → About phone → tap "Build number" 7x →
      Settings → System → Developer options → USB debugging).
- [ ] Pixel is connected and `adb devices` shows it as `device` (not
      `unauthorized` — accept the RSA fingerprint on the device if prompted).

---

## Step 1 — Build & install the debug APK

```bash
cd WatchMyCaloriesAndroid
./gradlew :app:installDebug
adb shell am start -n com.pning80.watchmycalories/.MainActivity
```

Watch logcat in another terminal for Play Integrity flow:
```bash
adb logcat -s PlayIntegrity:* GeminiRepository:* AndroidRuntime:E
```

**Pass criteria:** app reaches the Dashboard (or Onboarding then Dashboard).
Logcat shows no `AndroidRuntime: FATAL EXCEPTION` lines.

If the first launch is post-install, the `OnCreate` lifecycle hits
`PlayIntegrityManager.ensureAttested()`. Watch for one of:
- `PlayIntegrity: ... attestation succeeded` → expected; keyID + secret are
  now persisted in EncryptedSharedPreferences.
- `PlayIntegrity: Attestation failed; will fall back to legacy auth if available`
  → unexpected on a Pixel 9a (Play Services should be present). Investigate
  before continuing — Stage 1 deploy or IAM grant may be incomplete.

---

## Step 2 — Camera-to-saved-entry end-to-end

This exercises T1.5 (Gemini path), T1.8 (per-request HMAC), and T1.4 (image
persistence) in a single flow.

1. Tap the Log Food button (bottom nav, plus icon).
2. Tap **Scan Food**. Grant camera permission if prompted.
3. Frame any food item and tap the shutter.
4. Tap **Analyze N** (where N is the number of photos captured — should be 1
   if you didn't take multiples).
5. Wait for the Loading state to clear (≤30s under normal Gemini latency).
6. Review the estimation card. Tap **Save to Log**.
7. Verify you land on the Dashboard with the new entry visible under the
   correct meal section (Breakfast / Lunch / Dinner / Snack per the entry's
   hour-of-day).

**Pass criteria:**
- Step 5 produces a populated review card (food name, calories, macros).
- Step 7 shows the entry on the Dashboard.
- Logcat shows a successful POST to `/v1beta/models/default:generateContent`
  with `X-Android-Key-Id`, `X-Android-Counter`, `X-Android-Assertion` headers
  (you may need to verbose-log OkHttp temporarily — see step 4 below for the
  tampered-body test which uses the same instrumentation).

**Fail modes:**
- Step 5 stuck on Loading > 60s → backend or attestation issue. Check Cloud
  Run logs: `gcloud run services logs read watchmycalories-backend-dev
  --region us-central1 --project gen-lang-client-0629636941 --limit 20`.
- "Unauthorized (401): play_integrity_not_configured" → IAM grant missing.
- "Unauthorized (401): android_assertion_invalid" → the re-attest-retry path
  should fire and recover; if you see a permanent 401, the HMAC counter is
  diverging. Capture the request bytes and report.

---

## Step 3 — Force-stop persistence test (T1.4)

This verifies that the entry's image is actually persisted to disk, not just
held in memory.

```bash
# After step 2 completes successfully:
adb shell am force-stop com.pning80.watchmycalories
adb shell am start -n com.pning80.watchmycalories/.MainActivity
```

Navigate to History → tap the most recent day card → expand it.

**Pass criteria:** the entry from step 2 is visible. If the entry has a
thumbnail in the UI (currently the dashboard shows initials, not photos —
but the file should exist), verify the on-disk file:

```bash
# Pull the app's private storage to find the JPEG
adb shell run-as com.pning80.watchmycalories ls files/
```

Should show one or more `<uuid>.jpg` files. Each maps to a `FoodEntry.imageID`
in the Room DB.

**Fail modes:**
- Empty `files/` directory → `ImageStorage.saveJpeg` was not called or
  failed silently. Re-trace step 2 and check logcat for `ImageStorage` lines.

---

## Step 4 — Tampered-body 401-then-retry test (T1.8)

This verifies the per-request HMAC catches body tampering and the re-attest
retry path recovers.

Easiest approach: use the GeminiRepository's existing 401 handling by
artificially corrupting the body before the HMAC is computed.

**Manual approach (recommended for first verification):**

Edit `ai/GeminiRepository.kt` locally to corrupt one byte of `bodyBytes`
*after* the HMAC headers are computed but *before* the body is sent:

```kotlin
// In executeWithAttestation, after attestHeaders is computed:
val tamperedBody = bodyBytes.copyOf().also { it[0] = (it[0] + 1).toByte() }
.post(tamperedBody.toRequestBody("application/json".toMediaType()))
```

Rebuild, install, repeat Step 2. Expected sequence in logcat:

1. First POST → backend returns 401 with `android_assertion_invalid`
   (HMAC over the tampered body doesn't match the headers).
2. `PlayIntegrityManager.invalidateAndReattest()` fires — clears
   EncryptedSharedPreferences and re-attests.
3. Second POST goes out with fresh credentials. **This will ALSO fail** with
   401 because the body is still tampered. The 401 retry only fires once;
   second 401 propagates as an exception.

**Pass criteria:** logcat shows the two-401 sequence with re-attestation
between them. The user sees an "Analysis Failed" error screen with the 401
message — which is correct behavior for a genuinely tampered body.

**After verification:** revert the tamper edit. Don't commit it.

---

## Step 5 — End-to-end Android `/attest/verify` against dev (Stage 0.2 prereq)

This is implicitly covered by Step 1 if Play Integrity succeeded. To
explicitly verify:

```bash
adb shell run-as com.pning80.watchmycalories \
  cat /data/data/com.pning80.watchmycalories/shared_prefs/play_integrity_prefs.xml
```

Expected: an encrypted XML blob (EncryptedSharedPreferences uses AES256-GCM
on the values; you'll see opaque ciphertext per key, but the file should
exist and be non-empty). If the file is empty or missing, Play Integrity
attestation never succeeded.

Cross-reference Firestore:
```bash
gcloud firestore documents list attestedKeys-dev \
  --project=gen-lang-client-0629636941 --limit=5 \
  --filter="platform=android"
```

Should show ≥ 1 document with `platform: 'android'` after Step 1's first
launch. If none exist, the backend `/attest/verify` Android path never wrote
the doc (likely IAM or Play Integrity API issue).

---

## Step 6 — Sign off

Once Steps 1–5 all pass, update `PORTING_RUNBOOK.md` Stage 2 exit gate:
flip the four device-verification boxes to checked, with a one-line note of
the date and the dev revision tested against.

Update `PORTING_MATRIX.md`: flip the relevant `✅ code` cells under T1.4 /
T1.8 / T1.5 to `✅ device — Pixel 9a 2026-MM-DD (rev N)`.

If any step failed, **do not** sign off. File the failure with
- the step number that failed
- the exact logcat lines around the failure
- the Cloud Run revision tested
- whether the failure repro'd on a second attempt
in a new GitHub issue tagged `porting/stage-2-exit-gate`.
