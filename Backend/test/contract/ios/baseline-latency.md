# iOS-path baseline latency (T1.10.h)

Captured: 2026-05-29T05:58:22Z
Backend: `watchmycalories-backend-dev`
Pinned revision: `watchmycalories-backend-dev-00016-xmw`
Iterations: 30 per endpoint
Source: `scripts/capture-latency.sh` (Mac, x-backend-key path, text-only body)

| Endpoint | p50 (ms) | p95 (ms) | mean (ms) |
|---|---|---|---|
| /attest/challenge | 107 | 130 | 108 |
| /v1beta/models/default:generateContent | 1229 | 1612 | 1217 |

## Notes
- `/attest/challenge` is used as the cheaper proxy for `/attest/verify` (a real verify needs an iPhone-emitted App Attest blob).
- Gemini path uses a text-only body to avoid burning model tokens; same middleware/auth/Firestore path as a real image request.
