# Backend Test Plan

## Overview

100 tests across 13 test files (12 `.test.js` + 1 helper). Uses Node.js built-in test runner (`node:test` + `node:assert/strict`) with `supertest` for HTTP integration tests. Fully self-contained — no network, Firestore, Apple devices, or environment variables needed.

Additionally, 6 deployment smoke tests live in `test/deployment/` and run against real Cloud Run endpoints (not included in `npm test`).

## Running Tests

```bash
cd Backend
npm install
npm test
```

## Running Tests with Claude Code

Ask Claude to run the backend test cases:

> Run the backend test cases

Claude will execute `npm test` in the `Backend/` directory and report results.

## Test Files

| File | Type | Description |
|------|------|-------------|
| `test/assertion-verify.test.js` | HTTP + Unit | Assertion middleware — signature, counter, RP ID hash, Firestore fallback with HMAC hard-fail on tampered keys |
| `test/attestation-verify.test.js` | HTTP | `POST /attest/verify` — cert chain, nonce, RP ID, COSE key, Firestore persistence, server config errors (missing HMAC secret blocks attestation) |
| `test/challenge.test.js` | HTTP | `GET /attest/challenge` endpoint |
| `test/der-parsing.test.js` | Unit | `parseDerLength` and `extractNonceFromCert` pure functions |
| `test/e2e-attestation.test.js` | Integration | End-to-end attestation → assertion flow, HMAC secret rotation, HMAC hard-fail |
| `test/gemini-proxy.test.js` | HTTP | Gemini API proxy relay, error handling, fetch mocking |
| `test/health-cors.test.js` | HTTP | Health check endpoint + CORS headers |
| `test/hmac-secret.test.js` | Unit | `initHmacSecret` resolution logic, getter/setter, Secret Manager mock |
| `test/key-preload.test.js` | Unit | Cold-start key preload from Firestore (`loadKeysFromFirestore`), HMAC validation, graceful skip when HMAC secret unavailable |
| `test/legacy-auth.test.js` | HTTP | `x-backend-key` fallback authentication |
| `test/legacy-key-limiter.test.js` | HTTP | `legacyKeyLimiter` rate limiting for legacy `x-backend-key` auth |
| `test/rate-limiting.test.js` | HTTP | Rate limiting headers, 429 responses, counter isolation |
| `test/raw-body.test.js` | HTTP | `captureRawBody` middleware |
| `test/helpers/crypto-fixtures.js` | Helper | Self-signed P-256 cert chains, CBOR builders |

## Test Case Inventory

| File | Tests |
|------|-------|
| `assertion-verify.test.js` | 13 |
| `attestation-verify.test.js` | 18 |
| `challenge.test.js` | 3 |
| `der-parsing.test.js` | 6 |
| `e2e-attestation.test.js` | 5 |
| `gemini-proxy.test.js` | 13 |
| `health-cors.test.js` | 5 |
| `hmac-secret.test.js` | 7 |
| `key-preload.test.js` | 8 |
| `legacy-auth.test.js` | 4 |
| `legacy-key-limiter.test.js` | 6 |
| `rate-limiting.test.js` | 8 |
| `raw-body.test.js` | 4 |
| **Total** | **100** |

### `assertion-verify.test.js` (13 tests)

**`App Attest assertion verification`**
- `passes through with valid assertion` — valid signature + counter advances past auth
- `increments counter across multiple assertions` — counter updates in memory after each assertion
- `rejects counter not incrementing (replay)` — counter <= stored value returns 401
- `rejects unknown key ID` — unregistered key ID returns 401
- `rejects missing authenticatorData` — CBOR without authenticatorData field returns 401
- `rejects missing signature` — CBOR without signature field returns 401
- `rejects invalid signature (wrong key)` — signature from different private key returns 401
- `rejects RP ID hash mismatch` — wrong RP ID hash in authenticator data returns 401
- `fetches key from Firestore when not in memory` — Firestore fallback loads key, verifies HMAC, caches in memory
- `rejects Firestore key with tampered HMAC` — Firestore key with invalid HMAC returns 401 (hard-fail)
- `rejects when key not found anywhere` — key absent from both memory and Firestore returns 401
- `succeeds even when Firestore counter update fails` — Firestore write error is non-fatal; in-memory counter still updates
- `skips RP ID hash check when APPLE_TEAM_ID is not set` — missing APPLE_TEAM_ID skips RP ID verification

### `attestation-verify.test.js` (18 tests)

**`POST /attest/verify`**
- `stores key on valid attestation` — valid cert chain + nonce stores key with counter 0
- `rejects missing keyID` — request without keyID returns 400
- `rejects missing attestation` — request without attestation returns 400
- `rejects missing challenge` — request without challenge returns 400
- `rejects invalid challenge (not in map)` — unknown challenge string returns 400
- `rejects expired challenge` — challenge older than 60s returns 400
- `rejects challenge reuse` — consumed challenge cannot be used again
- `rejects wrong attestation format` — `fmt` != `apple-appattest` returns 400
- `rejects cert chain too short` — x5c with only leaf cert returns 400
- `rejects nonce mismatch (wrong challenge in computation)` — attestation built with challenge A submitted with challenge B returns 400
- `rejects RP ID hash mismatch` — wrong RP ID hash in authData returns 400
- `rejects invalid COSE key (missing x/y)` — COSE key without x/y coordinates returns 400
- `writes key to Firestore on successful attestation` — verifies Firestore collection, document ID, counter, publicKeyPem, hmac, timestamps
- `succeeds even when Firestore write fails` — Firestore error is non-fatal; key still stored in memory
- `returns 500 when no Apple root CA configured` — missing root CA returns server config error
- `returns 500 when APPLE_TEAM_ID missing` — missing team ID returns server config error
- `returns 500 when HMAC secret missing but APPLE_TEAM_ID present` — missing HMAC secret blocks attestation with 500
- `returns 400 for garbled (non-CBOR) attestation data` — non-decodable base64 returns 400

### `challenge.test.js` (3 tests)

**`GET /attest/challenge`**
- `returns 200 with a UUID-format challenge` — response contains UUID v4 string
- `returns unique challenges on each call` — two consecutive calls return different values
- `stores the challenge in the challenges Map` — challenge is stored with createdAt timestamp

### `der-parsing.test.js` (6 tests)

**`parseDerLength`**
- `parses single-byte length` — value < 128 parsed in 1 byte
- `parses multi-byte length (1 extra byte)` — 0x81 prefix, value 128
- `parses multi-byte length (2 extra bytes)` — 0x82 prefix, value 256

**`extractNonceFromCert`**
- `extracts 32-byte nonce from cert with Apple OID` — nonce round-trips through cert extension
- `returns null for cert without Apple OID` — self-signed cert without OID 1.2.840.113635.100.8.2
- `handles cert with critical flag on Apple OID` — parser tolerates optional BOOLEAN in extension

### `e2e-attestation.test.js` (5 tests)

**`End-to-end attestation → assertion flow`**
- `attested key can be used for subsequent assertions` — full flow: GET challenge → POST attest/verify → POST with assertion headers → 200
- `counter increments correctly across multiple assertions after attestation` — attest, then 2 assertions with counter 1 and 2
- `second attestation with same keyID overwrites the first` — attest key A, attest key B with same ID, assert with A fails (401), assert with B succeeds (200)
- `assertion fails after HMAC secret changes (Firestore fallback)` — attest with secret-1, clear memory cache, set mock Firestore, change to secret-2, assert → 401

**`Assertion HMAC hard-fail`**
- `returns 401 when HMAC secret is null and key is only in Firestore` — set up mock Firestore with key, `setHmacSecret(null)`, assert → 401 (covers `assertion.js:22-23`)

### `gemini-proxy.test.js` (13 tests)

**`Gemini proxy endpoint` > `environment variable validation`**
- `returns 500 when GEMINI_API_KEY missing` — server config error
- `returns 500 when GEMINI_MODEL_NAME missing` — server config error

**`Gemini proxy endpoint` > `successful proxy relay`**
- `relays request to Gemini and returns response` — mocked fetch returns 200 with candidates
- `constructs correct Gemini URL` — URL uses env var model name and API key
- `sends request body as JSON` — fetch called with POST, Content-Type, and JSON body
- `uses GEMINI_MODEL_NAME regardless of path param` — path model name is ignored in favor of env var

**`Gemini proxy endpoint` > `error relay from Gemini`**
- `relays 400 from Gemini` — upstream 400 passed through
- `relays 429 from Gemini` — upstream 429 passed through
- `relays 500 from Gemini` — upstream 500 passed through

**`Gemini proxy endpoint` > `network errors`**
- `returns 502 when fetch throws` — ECONNREFUSED returns 502 Bad Gateway
- `returns 502 on network rejection` — DNS failure returns 502 Bad Gateway

**`Gemini proxy endpoint` > `body handling`**
- `handles empty request body` — empty object relayed successfully
- `handles large body (base64 image)` — 100KB base64 payload relayed intact

### `health-cors.test.js` (5 tests)

**`Health check endpoint`**
- `GET / returns 200 with expected message` — "WatchMyCalories Backend is running."
- `GET / includes CORS header` — `access-control-allow-origin: *`
- `GET /nonexistent returns 404` — unknown routes return 404

**`CORS middleware`**
- `OPTIONS preflight returns CORS headers` — 204 with CORS headers
- `POST response includes CORS headers` — CORS headers present even on 401

### `hmac-secret.test.js` (7 tests)

**`hmac-secret module` > `initHmacSecret`**
- `loads secret from ATTEST_HMAC_SECRET env var` — set env var, call init, verify `getHmacSecret()` returns value
- `loads secret from Secret Manager when ATTEST_HMAC_SECRET_NAME is set` — mock SecretManagerServiceClient, verify secret loaded and correct Secret Manager path used
- `logs warning and leaves secret null when neither env var is set` — neither var set, verify `getHmacSecret() === null`
- `leaves secret null when Secret Manager throws` — mock client to throw, verify graceful degradation
- `env var takes priority over Secret Manager` — set both vars, verify env var value used, mock not called

**`hmac-secret module` > `getHmacSecret / setHmacSecret`**
- `returns null before initialization` — fresh state returns null
- `setHmacSecret overrides cached value` — set via setter, verify getter returns it, verify null reset works

### `key-preload.test.js` (8 tests)

**`Cold-start key preload from Firestore`**
- `loads keys from Firestore into memory` — loads 2 keys, preserves counters
- `returns 0 when no keys in Firestore` — empty collection returns 0
- `loaded keys can be used for assertion verification` — loaded key has usable publicKey with export()
- `skips keys with invalid HMAC` — tampered HMAC key skipped, valid key loaded
- `skips keys with invalid PEM` — unparseable PEM skipped even with matching HMAC
- `returns 0 when Firestore is not configured` — null db returns 0
- `returns 0 when Firestore query fails` — query exception returns 0
- `returns 0 when HMAC secret is not available` — null HMAC secret skips entire preload (returns 0)

### `legacy-auth.test.js` (4 tests)

**`Legacy x-backend-key auth`**
- `allows requests with valid x-backend-key` — correct key passes auth
- `rejects requests with invalid x-backend-key` — wrong key returns 401
- `rejects requests with no auth headers` — no auth headers returns 401

**`Legacy auth with missing APP_BACKEND_API_KEY`**
- `returns 500 when APP_BACKEND_API_KEY is not configured` — server config error

### `legacy-key-limiter.test.js` (6 tests)

**`Legacy key rate limiter`**
- `allows legacy requests under the rate limit` — first request is not 429
- `returns 429 when legacy key limit (15) is exceeded` — 16th request returns 429 with upgrade message
- `429 response includes Retry-After header` — rate-limited response has retry-after
- `App Attest requests bypass the legacy key limiter` — attested requests get 401 (not 429) when legacy limit exhausted
- `resetting legacy-key-global allows requests again` — limiter reset restores access
- `invalid legacy key requests still consume rate limit budget` — wrong-key 401s count toward the limit

### `rate-limiting.test.js` (8 tests)

**`Rate Limiting`**
- `GET / includes standard rate limit headers` — ratelimit-limit, ratelimit-remaining, ratelimit-reset
- `rate limit headers show correct remaining count` — remaining decreases by 1 per request
- `GET /attest/challenge includes rate limit headers` — attest endpoint has rate limit headers
- `POST /v1beta/models/* includes rate limit headers` — Gemini proxy has rate limit headers (even on 401)
- `returns 429 when attest limit exceeded` — 31st attest/challenge request returns 429
- `429 response includes Retry-After header` — retry-after header present
- `429 response body contains error message` — "Too many attestation requests, please try again later."
- `different endpoints have independent counters` — exhausted attest limiter does not block health check

### `raw-body.test.js` (4 tests)

**`captureRawBody middleware`**
- `captures raw bytes and parses JSON body` — raw bytes match sent JSON, parsed body available
- `captures raw bytes for non-JSON body` — text/plain body captured as raw bytes
- `handles empty body` — rawBody.length === 0
- `preserves raw bytes for large body` — 10KB payload round-trips through rawBody

## Test Infrastructure

`test/helpers/crypto-fixtures.js` generates synthetic Apple-like artifacts:

- **Self-signed cert chain** (root → intermediate → leaf) with P-256 keys
- Leaf certs include OID `1.2.840.113635.100.8.2` with the attestation nonce
- CBOR attestation/assertion builders matching Apple's format
- P-256 key pair generation for assertion signing
- RP ID hash computation from team ID + bundle ID

Exports: `getTestRootCaPem`, `generateLeafCert`, `buildAuthData`, `buildAttestationObject`, `buildAssertionObject`, `generateP256KeyPair`, `buildCoseKey`, `computeRpIdHash`, `signAssertion`, `buildAssertionAuthData`, `BUNDLE_ID`

Uses `@peculiar/x509` (dev dependency) for X.509 cert generation with custom OID extensions.

## server.js Test Exports

`server.js` exports the following when imported (does not start the HTTP listener):

```js
{ app, attestedKeys, challenges, extractNonceFromCert, parseDerLength,
  setAppleRootCa, setDb, setHmacSecret, captureRawBody, verifyRequest,
  globalLimiter, geminiLimiter, attestLimiter, legacyKeyLimiter, loadKeysFromFirestore }
```

- `setAppleRootCa(pem)` — inject test root CA
- `setDb(mockDb)` — inject mock Firestore
- `setHmacSecret(value)` — inject HMAC secret for tests (mirrors `setDb` pattern)
- `legacyKeyLimiter` — rate limiter for legacy `x-backend-key` auth (15 req/15min global bucket)
- `loadKeysFromFirestore()` — preload attested keys from Firestore on cold start

## Key Behavioral Notes

1. **HMAC verification is mandatory** — `assertion.js` throws when loading a key from Firestore if the HMAC secret is unavailable or the HMAC does not match. This is a hard-fail, not an optional/skippable check.
2. **HMAC secret is read-only** — `hmac-secret.js` reads from `ATTEST_HMAC_SECRET` env var or Secret Manager; `deploy.sh` generates the value. The backend never writes to Secret Manager.
3. **Key preload skips entirely** when HMAC secret is unavailable — `loadKeysFromFirestore()` returns 0 without loading any keys.
4. **Attestation blocks** (500) when HMAC secret is missing — `POST /attest/verify` returns a server configuration error if HMAC secret is not set.

## Deployment Smoke Tests

Separate from `npm test` — these hit real Cloud Run endpoints. Not counted in the main test total.

### Running

```bash
npm run test:smoke          # dev (watchmycalories-backend-dev)
npm run test:smoke:prod     # prod (watchmycalories-backend)
```

URL resolution: `CLOUD_RUN_URL` env var (for CI), or `gcloud run services describe` (requires gcloud CLI).

### Test Files

| File | Tests | Description |
|------|-------|-------------|
| `test/deployment/smoke.test.js` | 6 | Health check, CORS, challenge endpoint, auth rejection, rate limit headers |

### `deployment/smoke.test.js` (6 tests)

**`Deployment smoke tests (<env>)`**
- `GET / returns 200 with health message` — verify "WatchMyCalories Backend is running."
- `responses include CORS headers` — verify `access-control-allow-origin: *`
- `GET /attest/challenge returns 200 with UUID challenge` — verify UUID v4 format
- `POST /v1beta/models/* with no auth returns 401` — no headers → 401
- `POST /v1beta/models/* with invalid legacy key returns 401` — wrong key → 401
- `responses include rate limit headers` — verify ratelimit-limit, ratelimit-remaining present
