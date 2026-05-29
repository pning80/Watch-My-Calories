# Stage 1 non-regression evidence: pre-port vs post-Stage-1 iOS-path diff

Captured 2026-05-28 from a real Test iPhone 14 hitting `watchmycalories-backend-dev`
on both pre-port revision `watchmycalories-backend-dev-00016-xmw` (deployed 2026-05-16,
predates Stage 1) and post-Stage-1 revision `watchmycalories-backend-dev-00017-xvk`
(deployed 2026-05-28 05:48 UTC, contains the X-App-Platform dispatcher, Play Integrity
verifier, Android per-request HMAC, and Firestore schema additions).

The same iPhone, the same app build, and the same logical action (one food photo of
8 fl oz water, captured via mitmproxy with passthrough for Apple endpoints) was run
against both revisions in succession. Cloud Run traffic was switched between
revisions via `gcloud run services update-traffic`; no other state changed.

## Endpoint-by-endpoint diff

### `GET /attest/challenge`
| | Pre-port `00016-xmw` | Post-Stage-1 `00017-xvk` | Drift |
|---|---|---|---|
| Response keys | `["challenge"]` | `["challenge"]` | none |
| Response shape | `{ "challenge": "<uuid>" }` | `{ "challenge": "<uuid>" }` | none |
| Status | 200 | 200 | none |

The challenge UUID value differs (it's a one-time nonce — that's the intended behavior).

### `POST /attest/verify`
| | Pre-port `00016-xmw` | Post-Stage-1 `00017-xvk` | Drift |
|---|---|---|---|
| Request body keys | `["attestation","challenge","keyID"]` | (iPhone sent the same shape, identical client) | n/a |
| Response | `{"success":true}` | `{"success":true}` | **byte-identical** |
| Status | 200 | 200 | none |

### `POST /v1beta/models/default:generateContent`
| | Pre-port `00016-xmw` | Post-Stage-1 `00017-xvk` | Drift |
|---|---|---|---|
| Top-level response keys | `["candidates","modelVersion","responseId","usageMetadata"]` | `["candidates","modelVersion","responseId","usageMetadata"]` | none |
| `candidates[0]` keys | `["content","finishReason","index"]` | `["content","finishReason","index"]` | none |
| `parts[0]` keys | `["text","thoughtSignature"]` | `["text","thoughtSignature"]` | none |
| iOS-parsed JSON shape | `{items:[{name,quantity,calories,protein,carbs,fat,confidence}]}` | identical | none |

### Rate-limit `429` (deferred — Mac rate-limit budget exhausted)
The pre-port 429 fixture is committed at `rate-limit-429.response.json` with body
`{"error":"Too many analysis requests, please try again later."}` and `Retry-After: 563`.
Post-deploy 429 capture against `00017-xvk` was deferred because forcing a 429 on the
pre-port revision consumed the gemini rate-limit window for the Mac's egress IP.
Window resets 2026-05-28T~23:14 local; re-capture from a fresh terminal then. The
unit test `Backend/test/rate-limiting.test.ts` already covers the in-code 429 shape
contract; this gap is therefore evidence-only, not a coverage gap.

## Latency baseline (T1.10.h)

Captured against pre-port `00016-xmw` only (post-deploy deferred for the same
rate-limit reason). See `baseline-latency.md`:

| Endpoint | p50 (ms) | p95 (ms) | mean (ms) |
|---|---|---|---|
| /attest/challenge | 107 | 130 | 108 |
| /v1beta/models/default:generateContent | 1229 | 1612 | 1217 |

Post-deploy values must land within ±10% of these for the Stage 4 prod-cutover gate.

## Verdict (T1.10.c)

**The iOS-facing wire contract is empirically identical between the pre-port and
post-Stage-1 backends for every endpoint captured.** The Stage 1 changes (platform
dispatcher, Play Integrity, Android HMAC, Firestore schema additions) introduced
no observable shape drift on the iOS path. This validates the existing unit-test
non-regression coverage with a real-device end-to-end check.

The captured JSON fixtures in this directory are now load-bearing artifacts
for `Backend/test/ios-contract-fixtures.test.ts` (7 test cases, all green as
of 2026-05-28). The raw `.bin` binary captures referenced in the head-to-head
diff above were used as evidence at capture-time but are intentionally not
committed — see `README.md` "Why no `.bin` captures are committed".
