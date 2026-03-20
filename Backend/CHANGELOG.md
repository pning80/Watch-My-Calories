# Changelog

## 1.1.1 (2026-03-19)

- Log client app platform and version from `X-App-Platform` / `X-App-Version` request headers

## 1.1.0 (2026-03-14)

- Apple App Attest verification with Firestore persistence and HMAC tamper detection
- Production-only App Attest enforcement; dev allows legacy `x-backend-key` auth with strict rate limiting
- Per-endpoint rate limiting (global, Gemini relay, attest) configurable via env vars
- 12 MB request body size limit (DoS prevention)
- Full TypeScript migration with strict mode
- Structured JSON logging via Pino (Cloud Logging compatible); removed plaintext API token logging
- Lightweight counter and timer metrics emitted as structured log entries
- DER parsing hardened with bounds checks; nonce validated to exactly 32 bytes
- Base64url encoding for Firestore document IDs (fixes `/` in keyIDs)
- HMAC secret lifecycle extracted into dedicated module (reads from env or Secret Manager)
- Health endpoint returns JSON status
- 144 tests covering attestation, assertion, DER parsing, legacy auth, rate limiting, metrics, type guards, and E2E flows
- Deployment smoke tests for live Cloud Run endpoints

## 1.0.0 (2026-03-01)

- Initial release
- Express server proxying food analysis requests to Google Gemini API
- Cloud Run deployment with `deploy.sh`
- CORS support
- Environment-based configuration via dotenv
