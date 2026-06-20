# Dev Deploy Smoke Test (Stage 1 burn-in entry point)

> **Archived — historical record of the completed iOS→Android port.** Some links to `PORTING_*` / `PARITY_*` working docs point to files removed once the port landed; treat them as historical.

What to check immediately after `./deploy.sh dev` lands the Stage 1 backend
changes (commit `7ff3731` and successors). The goal is to confirm that

1. iOS path is **byte-for-byte unchanged** (T1.10 hard requirement), and
2. Android path returns **503 cleanly** until the IAM grant lands.

Run these in order. If any fails, roll back per the command in
`PORTING_RUNBOOK.md` Stage 0.3 before investigating.

---

## 0 — Precondition: capture revision tag

```bash
gcloud run services describe watchmycalories-backend-dev \
  --region us-central1 \
  --project YOUR_GCP_PROJECT_ID \
  --format='value(status.traffic[0].revisionName)'
```

Record the output as `NEW_DEV_REV`. The predecessor (rollback target) is
`watchmycalories-backend-dev-00016-xmw` (already pinned in
`PORTING_RUNBOOK.md` 0.3). If for some reason the predecessor differs, also
record it.

---

## 1 — Verify the deployed image actually contains the Stage 1 code

The deploy might silently use a stale image if the build failed. Check that
the new image was pushed:

```bash
gcloud run revisions describe "$NEW_DEV_REV" \
  --region us-central1 \
  --project YOUR_GCP_PROJECT_ID \
  --format='value(spec.containers[0].image)'
```

Should print a digest URL ending in `@sha256:xxxxx`. Confirm the digest is
**not** equal to `sha256:d0def109667ee290d0dfeb187846f333939052139b88fd9f61884c4d186b2035`
(that's the pre-Stage-1 prod image; a fresh deploy must produce a different
digest).

---

## 2 — Root route reachable

The backend exposes `GET /` (`Backend/src/routes.ts:80`), not `/healthz` — use that
as the basic boot signal.

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  https://watchmycalories-backend-dev-YOUR_PROJECT_NUMBER.us-central1.run.app/
```

Expect: `200`. Any non-200 → roll back immediately, the service didn't boot.

---

## 3 — iOS path still works without `X-App-Platform` (defaults to ios)

The Stage 1 dispatch defaults to `ios` when the header is absent. Pre-port
iOS clients don't send the header on the legacy `x-backend-key` path, so this
should still 401 (correctly — without a valid key it's unauthorized) but with
the iOS-shaped error body, not the Android one.

```bash
curl -sv \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"contents":[{"parts":[{"text":"hi"}]}]}' \
  https://watchmycalories-backend-dev-YOUR_PROJECT_NUMBER.us-central1.run.app/v1beta/models/default:generateContent \
  2>&1 | grep -E "^< HTTP|< x-app-platform|^\{"
```

Expect: `HTTP/2 401`, response body shape matches what iOS-only flow used to
produce. The exact shape is captured in
`Backend/test/contract/ios/` and locked down by the iOS-non-regression
tests — but you can eyeball it for the "no missing field" / "no schema drift"
sanity check.

---

## 4 — iOS path with valid legacy `x-backend-key` still 200s

```bash
APP_KEY="$(gcloud secrets versions access latest \
  --secret=watchmycalories-dev-app-backend-api-key \
  --project=YOUR_GCP_PROJECT_ID)"

curl -s -o /tmp/r.json -w "HTTP %{http_code}, %{time_total}s\n" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "x-backend-key: $APP_KEY" \
  -H "X-App-Platform: ios" \
  -d '{"contents":[{"parts":[{"text":"Say the word ok and nothing else."}]}]}' \
  https://watchmycalories-backend-dev-YOUR_PROJECT_NUMBER.us-central1.run.app/v1beta/models/default:generateContent

head -c 200 /tmp/r.json
echo
```

Expect: `HTTP 200`, response body has `candidates[0].content.parts[0].text`
containing `ok` (or close — Gemini is non-deterministic). Latency should be
within ±10% of the iOS baseline you captured in
`Backend/test/contract/ios/baseline-latency.md` (T1.10.h). If it's >50%
worse, that's a Stage 1 regression — investigate or roll back.

---

## 5 — Android path returns 503 cleanly when IAM grant is missing

Android clients send `X-App-Platform: android` and Play Integrity headers.
Without the IAM grant on the runtime SA, the verifier should fail closed
with a recognizable error body — not 500, not stack trace leakage.

Easiest synthetic test: send a request with the platform header and an
obviously bogus Play Integrity token.

```bash
curl -sv \
  -X POST \
  -H "Content-Type: application/json" \
  -H "X-App-Platform: android" \
  -d '{"keyID":"test-key-id","attestation":"not-a-real-token","challenge":"deadbeef"}' \
  https://watchmycalories-backend-dev-YOUR_PROJECT_NUMBER.us-central1.run.app/attest/verify \
  2>&1 | grep -E "^< HTTP|^\{"
```

Expect: `HTTP/2 503` with body containing `play_integrity_not_configured`
(when IAM grant is missing) OR `HTTP/2 401` with `play_integrity_failed`
(when IAM grant is present but the token is bogus, which is the post-IAM-grant
expected behavior — both indicate the Android path is wired and not
crashing).

Verify there's no stack trace in the response body:
```bash
curl -s \
  -X POST \
  -H "Content-Type: application/json" \
  -H "X-App-Platform: android" \
  -d '{"keyID":"x","attestation":"y","challenge":"z"}' \
  https://watchmycalories-backend-dev-YOUR_PROJECT_NUMBER.us-central1.run.app/attest/verify \
  | grep -iE "Error:|at .+\(.+:[0-9]+\)|StackTrace" \
  && echo "FAIL: stack trace leaked" || echo "ok: no stack trace in body"
```

---

## 6 — Confirm Firestore schema is backward compatible

The Stage 1 schema additions (`platform`, `androidAssertionSecret`) are
nullable. Existing iOS docs without them must still load.

```bash
# Pick a random existing iOS doc
SAMPLE_DOC="$(gcloud firestore documents list attestedKeys-dev \
  --project=YOUR_GCP_PROJECT_ID \
  --limit=1 --format='value(name)')"

echo "Sample iOS doc: $SAMPLE_DOC"

# Trigger an iOS-flavored request against this key's user (find a Test iPhone
# 14 already-attested keyID — easiest is to just exercise the iOS app once
# against the new dev backend; if it works end-to-end, schema-compat is fine).
```

If you have a Test iPhone 14: open the app against dev (debug build), try a
food scan. End-to-end success ⇒ schema-compat verified. Failure ⇒ check
Cloud Run logs for the keyID load path:

```bash
gcloud run services logs read watchmycalories-backend-dev \
  --region us-central1 \
  --project YOUR_GCP_PROJECT_ID \
  --limit 30 \
  --filter='textPayload:"attested-keys"'
```

Look for `loaded` lines. A success log proves the platform-less iOS doc
parsed correctly.

---

## 7 — Latency parity check against baseline (T1.10.h)

Compare the latency you observed in Step 4 to
`Backend/test/contract/ios/baseline-latency.md`:

| Endpoint | Pre-Stage-1 p50 | Now p50 | Δ |
|---|---|---|---|
| `/v1beta/models/default:generateContent` | from baseline file | from Step 4 | should be ≤ +10% |

Run Step 4 ~10× and average to reduce noise. If Δ > 10%, that's a Stage 1
regression worth investigating before promoting to prod.

---

## 8 — Watch error rate for 1 hour (T1.10.f burn-in start)

Open in another terminal/tab:
```bash
gcloud run services logs tail watchmycalories-backend-dev \
  --region us-central1 \
  --project YOUR_GCP_PROJECT_ID \
  --filter='severity>=WARNING'
```

Let it run for the duration of an engineer-day's casual iOS use. Acceptable:
zero unexpected ERROR-severity lines from your own iOS test traffic.
Expected (harmless): occasional `Unauthorized request attempt with invalid
legacy key` lines if anyone hits the public URL without auth.

---

## 9 — Sign off in the runbook

If Steps 1–7 pass and Step 8 burn-in is clean:

- Update `PORTING_RUNBOOK.md` Stage 1 exit gate: flip "Dev backend deployed"
  and "one engineer-day iOS dev-build burn-in observed" boxes.
- Update the external-dependency table: flip the "Dev deploy actually run"
  row to **done — YYYY-MM-DD on revision NEW_DEV_REV**.

If any step fails, roll back **before** investigating:

```bash
gcloud run services update-traffic watchmycalories-backend-dev \
  --region us-central1 \
  --to-revisions=watchmycalories-backend-dev-00016-xmw=100 \
  --project YOUR_GCP_PROJECT_ID
```

…then file a `porting/stage-1-deploy-failure` issue with the failed step's
logs and the request bytes that exposed it.
