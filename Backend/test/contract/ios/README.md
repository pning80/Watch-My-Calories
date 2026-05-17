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
| `captured-attest-verify-request.bin` | Verbatim binary HTTP request from iOS | mitmproxy raw export; used by `legacy-ios-no-platform-header.test.ts` for structural replay |
| `captured-gemini-request.bin` | Verbatim binary Gemini request from iOS | same; used by the same replay test |
| `baseline-latency.md` | Pre-port p50/p95 latency for `/attest/verify` and `/v1beta/models/default:generateContent`, plus pinned Cloud Run revision tags | run 100 requests from Test iPhone 14 against current dev backend; record per `PORTING_RUNBOOK.md` 0.3 |
| `live-equivalence.md` | Runbook for the manual live-device equivalence test (T1.10.b test 2) | written once; followed each time the backend is redeployed |

## Redaction template

Where a field is redacted from a real iOS request, use these placeholders so replay tests can substitute valid values back:

| Field | Placeholder |
|-------|-------------|
| keyID | `__KEY_ID__` (32-byte base64 token) |
| challenge | `__CHALLENGE__` (UUID v4) |
| attestation blob | `__ATTESTATION_B64__` |
| image bytes | `__IMAGE_B64__` |
| assertion blob | `__ASSERTION_B64__` |

The replay test reads the binary fixture, regex-substitutes the placeholders with synthesized-and-signed values, and replays against the post-port server with App Attest verification stubbed.

## Snapshot update policy

Per `PORTING_CRITERIA.md` T1.10.c — snapshot updates require explicit reviewer sign-off and a note in `Backend/CHANGELOG.md`. An "innocent" snapshot bump in a porting PR is a red flag, not a routine fix. If the snapshot has to move, the iOS app is changing and that is out of scope per Operating Principle 7.
