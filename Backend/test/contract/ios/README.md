# iOS Wire Contract Snapshots

Frozen captures of the iOS-facing wire contract, used by `PORTING_CRITERIA.md` T1.10.b and T1.10.c to prove the backend changes for Android did not regress iOS.

## What goes here

| File | Purpose | Captured how |
|------|---------|--------------|
| `attest-challenge.response.json` | Response shape for `GET /attest/challenge` | curl against pre-port `watchmycalories-backend-dev`; pretty-printed JSON |
| `attest-verify.request.json` | Request shape sent by iOS to `POST /attest/verify` | mitmproxy capture from Test iPhone 14; keyID + challenge + attestation redacted to template placeholders |
| `attest-verify.response.json` | Response shape returned to iOS | same capture as above |
| `gemini-generate-content.request.json` | Request shape for `POST /v1beta/models/default:generateContent` | mitmproxy; image bytes redacted to short placeholder |
| `gemini-generate-content.response.json` | Response shape | same capture |
| `rate-limit-429.response.json` | 429 body + `Retry-After` header | curl loop until limiter trips; capture last response (status, body, headers) |
| `baseline-latency.md` | Pre-port p50/p95 latency for `/attest/challenge` (proxy for `/attest/verify`) and `/v1beta/models/default:generateContent`, plus pinned Cloud Run revision tags | `Backend/scripts/capture-latency.sh N` |
| `STAGE_1_DIFF.md` | Head-to-head diff of pre-port (`watchmycalories-backend-dev-00016-xmw`) vs post-Stage-1 (`watchmycalories-backend-dev-00017-xvk`) captures — evidence for the T1.10.c iOS non-regression gate | written once per major backend change |

## Redaction template

Where a field is redacted from a real iOS request, use these placeholders so replay tests can substitute valid values back:

| Field | Placeholder |
|-------|-------------|
| keyID | `__KEY_ID__` (32-byte base64 token) |
| challenge | `__CHALLENGE__` (UUID v4) |
| attestation blob | `__ATTESTATION_B64__` |
| image bytes | `__IMAGE_B64__` |
| assertion blob | `__ASSERTION_B64__` |

## Why no `.bin` captures are committed

Verbatim binary captures (raw HTTP request bytes from a real iPhone) embed real App Attest blobs and a real JPEG. They were captured once during Stage 0.3 (see `STAGE_1_DIFF.md`) and used as evidence for the iOS-non-regression check, but are not committed: the JSON fixtures above cover the contract-shape needs of every existing test. When the structural-replay test is written, captures will be regenerated via `Backend/scripts/mitm-capture.py` and either downloaded from artifact storage at CI time or synthesized at test setup time (deterministic, no real device data). The placeholder names listed in the redaction template above are the contract that future test will rely on.

## Snapshot update policy

Per `PORTING_CRITERIA.md` T1.10.c — snapshot updates require explicit reviewer sign-off and a note in `Backend/CHANGELOG.md`. An "innocent" snapshot bump in a porting PR is a red flag, not a routine fix. If the snapshot has to move, the iOS app is changing and that is out of scope per Operating Principle 7.
