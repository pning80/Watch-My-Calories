# Pre-port iOS Capture Runbook (Stage 0.3)

Step-by-step guide for capturing the iOS-facing wire contract against the
pre-port dev backend, so that T1.10.b structural-replay and T1.10.c contract
snapshots have real fixtures to assert against.

**This must run BEFORE the Stage 1 backend PR lands** — otherwise the
"baseline" the snapshots compare against has already drifted to include the
new Android dispatch code, defeating the purpose of the regression guard.

You will need:
- A **Test iPhone 14** (per the deploy-target preference recorded in memory)
- A signed Debug build of the iOS app installed on the device
- **mitmproxy** installed on your Mac (`brew install mitmproxy`)
- Both Mac and iPhone on the same Wi-Fi network
- ~30 minutes of focused time

The outputs land under `Backend/test/contract/ios/` and feed two tests:
1. `legacy-ios-no-platform-header.test.ts` (T1.10.b structural replay)
2. The contract-snapshot test that asserts post-port responses match these
   shapes (T1.10.c)

---

## Step 1 — Install mitmproxy + the trust cert on iPhone

1. **On the Mac:** `brew install mitmproxy` (if not already installed).
2. **Start mitmproxy** in a terminal: `mitmweb --listen-port 8080`. A browser
   tab opens at `http://localhost:8081` showing the live flow.
3. **Find your Mac's local IP**: `ipconfig getifaddr en0` (or `en1` on Wi-Fi).
   Note this — call it `MAC_IP` below.
4. **On iPhone**: Settings → Wi-Fi → tap (i) next to your network →
   **Configure Proxy** → Manual → Server: `MAC_IP`, Port: `8080`. Save.
5. **On iPhone Safari**: visit `http://mitm.it` → tap "Apple" → install the
   profile (Settings → General → VPN & Device Management → mitmproxy → Install).
6. **Trust the cert**: Settings → General → About → Certificate Trust Settings →
   enable "mitmproxy" full trust. iOS requires this two-step trust dance; without
   it, HTTPS interception fails on iOS 10.3+.
7. **Verify**: open Safari to `https://www.apple.com`. Should load. mitmweb in
   the browser should show the request. If you see TLS errors instead, the cert
   isn't trusted — repeat step 6.

## Step 2 — Drive the app to generate the requests we need

Open the Watch My Calories Debug build. Perform the following sequence; each
step generates a request the snapshot test will lock down.

### 2.a — `/attest/challenge` + `/attest/verify`

These fire on **first launch** of a clean install (or after clearing app data).
The simplest way to force them: delete + reinstall the app, then launch.

Watch mitmweb for two requests:
- `GET https://watchmycalories-backend-dev-657698311127.us-central1.run.app/attest/challenge`
- `POST https://watchmycalories-backend-dev-657698311127.us-central1.run.app/attest/verify`

For each, in mitmweb:
- Right-click → **Export** → **Raw** → save the request bytes
- Also save the response body

### 2.b — `/v1beta/models/default:generateContent` (Gemini food path)

Trigger by capturing a food photo and tapping **Save** on the estimation review.

Watch for `POST https://watchmycalories-backend-dev-657698311127.us-central1.run.app/v1beta/models/default:generateContent`.

### 2.c — 429 rate-limit response

Run a small script that hammers the Gemini route until you get a 429. From the
repo root:

```bash
cd Backend
APP_KEY="$(gcloud secrets versions access latest --secret=watchmycalories-dev-app-backend-api-key --project=gen-lang-client-0629636941)"
for i in {1..150}; do
  curl -s -o /tmp/r.json -w "%{http_code}\n" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "x-backend-key: $APP_KEY" \
    -H "X-App-Platform: ios" \
    -d '{"contents":[{"parts":[{"text":"hi"}]}]}' \
    "https://watchmycalories-backend-dev-657698311127.us-central1.run.app/v1beta/models/default:generateContent"
done
```

When you see `429`, capture that response body and headers (especially
`Retry-After`).

## Step 3 — Save and redact the fixtures

Save the captured artifacts under `Backend/test/contract/ios/` with these exact
names (the tests look for them):

| File | What it is | Source |
|------|------------|--------|
| `attest-challenge.response.json` | pretty-printed JSON body | step 2.a |
| `attest-verify.request.json` | pretty-printed JSON body | step 2.a |
| `attest-verify.response.json` | pretty-printed JSON body | step 2.a |
| `gemini-generate-content.request.json` | pretty-printed JSON body, image bytes redacted | step 2.b |
| `gemini-generate-content.response.json` | pretty-printed JSON body | step 2.b |
| `rate-limit-429.response.json` | body + the captured `Retry-After` header (annotate inline) | step 2.c |
| `captured-attest-verify-request.bin` | full HTTP request bytes (status line + headers + body) | step 2.a, via mitmweb → Export Raw |
| `captured-gemini-request.bin` | full HTTP request bytes | step 2.b, via mitmweb → Export Raw |
| `baseline-latency.md` | run timing measurements (steps below) | step 4 |

### Redaction template

Per `Backend/test/contract/ios/README.md`, replace sensitive fields with
placeholders the replay test substitutes back:

| Field | Replace with |
|-------|--------------|
| keyID (any 32-byte base64 string) | `__KEY_ID__` |
| challenge (UUID) | `__CHALLENGE__` |
| attestation blob (long base64) | `__ATTESTATION_B64__` |
| inline_data.data (image bytes) | `__IMAGE_B64__` |
| x-app-attest-assertion header value | `__ASSERTION_B64__` |

These are textual replacements — use a text editor or `sed` on the JSON files.
For the `.bin` files, use a hex editor (e.g., `hexyl` to view; `dd` to splice).

**Do not redact:** field names, structural punctuation, the `X-App-Platform`
header (it must be present and equal to `ios` to prove iOS already sends it).

## Step 4 — Baseline latency

From the Mac terminal (with the Test iPhone 14 hot — recently used, not after
a long idle, so cold-start latency doesn't skew the measurement):

```bash
# Pin the current Cloud Run revision so we can roll back to it if needed (T1.10.g).
gcloud run services describe watchmycalories-backend-dev \
  --region=us-central1 --project=gen-lang-client-0629636941 \
  --format='value(status.traffic[0].revisionName)' > /tmp/dev-rev.txt
cat /tmp/dev-rev.txt
gcloud run services describe watchmycalories-backend \
  --region=us-central1 --project=gen-lang-client-0629636941 \
  --format='value(status.traffic[0].revisionName)' > /tmp/prod-rev.txt
cat /tmp/prod-rev.txt
```

Record these two revision tags in `baseline-latency.md`.

For per-endpoint latency, the cleanest measure is to drive the app naturally
and read mitmweb's flow timings — capture 30 attest-verify calls and 30
gemini-generate-content calls. Compute p50 and p95 by hand or with:

```bash
# example: paste milliseconds (one per line) into a file `times.txt`
sort -n times.txt | awk 'BEGIN{c=0} {a[c++]=$1} END{print "p50:", a[int(c*0.5)]; print "p95:", a[int(c*0.95)]}'
```

Write the results into `baseline-latency.md`:

```markdown
# iOS-path baseline latency (T1.10.h)

Captured: <YYYY-MM-DD>
Device: Test iPhone 14
Backend: watchmycalories-backend-dev
Pinned revision: <revision tag from step above>

| Endpoint | p50 (ms) | p95 (ms) |
|---|---|---|
| /attest/verify | … | … |
| /v1beta/models/default:generateContent | … | … |

## Rollback tags (T1.10.g)
- dev: <dev-rev tag>
- prod: <prod-rev tag>
```

## Step 5 — Take the iPhone off the proxy

Important — leaving mitmproxy enabled will break unrelated HTTPS browsing on
the device.

1. **iPhone**: Settings → Wi-Fi → (i) → Configure Proxy → **Off**.
2. **iPhone**: Settings → General → VPN & Device Management → mitmproxy →
   Remove Profile. (Optional but recommended — leaving the cert installed is a
   permanent MITM-attack vector if the proxy is ever turned back on by anyone
   with access to the device.)
3. **Mac**: stop the mitmweb process.

## Step 6 — Commit the fixtures

`git add Backend/test/contract/ios/` then commit. The `.bin` files are
intentionally checked in — they're the load-bearing artifact for T1.10.b
structural replay.

If any captured file contains an un-redacted secret you missed (real keyID,
real assertion blob), **do not commit** — re-redact and try again. The
attestation cryptography is replay-protected by design, but treating these as
sensitive is the right hygiene.

## What you've unblocked

Once these eight files are in the repo:
- T1.10.b structural-replay test can be written (it depends on
  `captured-attest-verify-request.bin` + `captured-gemini-request.bin`)
- T1.10.c contract-snapshot test can be wired against the JSON shapes
- T1.10.h has a real baseline to compare future deploys against
- T1.10.g has a pinned revision tag for one-command rollback

At that point, **Stage 0.3 is complete**. Stage 1 backend work can begin once
the Play Console steps (Stage 0.2) also finish.
