# Backend Test Plan

## Overview

Tests for the Watch My Calories backend (`server.js`), covering App Attest security, Gemini API proxy, health check, and CORS middleware. Uses Node.js built-in test runner (`node:test` + `node:assert`) with `supertest` for HTTP integration tests.

## Running Tests

```bash
cd Backend
npm install
npm test
```

No network access, Firestore, Apple devices, or environment variables required.

## Running Tests with Claude Code

Ask Claude to run the backend test cases:

> Run the backend test cases

Claude will execute `npm test` in the `Backend/` directory and report results.

## Test Files

| File | Type | Description |
|------|------|-------------|
| `test/der-parsing.test.js` | Unit | `parseDerLength` and `extractNonceFromCert` pure functions |
| `test/challenge.test.js` | HTTP | `GET /attest/challenge` endpoint |
| `test/attestation-verify.test.js` | HTTP | `POST /attest/verify` — cert chain, nonce, RP ID verification |
| `test/assertion-verify.test.js` | HTTP + Unit | Assertion middleware — signature, counter, Firestore fallback |
| `test/legacy-auth.test.js` | HTTP | `x-backend-key` fallback authentication |
| `test/raw-body.test.js` | HTTP | `captureRawBody` middleware |
| `test/health-cors.test.js` | HTTP | Health check endpoint + CORS headers |
| `test/gemini-proxy.test.js` | HTTP | Gemini API proxy relay, error handling, fetch mocking |
| `test/rate-limiting.test.js` | HTTP | Rate limiting headers, 429 responses, counter isolation |
| `test/helpers/crypto-fixtures.js` | Helper | Self-signed P-256 cert chains, CBOR builders |

## Test Infrastructure

`test/helpers/crypto-fixtures.js` generates synthetic Apple-like artifacts:

- **Self-signed cert chain** (root → intermediate → leaf) with P-256 keys
- Leaf certs include OID `1.2.840.113635.100.8.2` with the attestation nonce
- CBOR attestation/assertion builders matching Apple's format
- P-256 key pair generation for assertion signing

Uses `@peculiar/x509` (dev dependency) for X.509 cert generation with custom OID extensions.

## server.js Test Exports

`server.js` exports the following when imported (does not start the HTTP listener):

```js
{ app, attestedKeys, challenges, extractNonceFromCert, parseDerLength,
  setAppleRootCa, setDb, captureRawBody, verifyRequest }
```

- `setAppleRootCa(pem)` — inject test root CA
- `setDb(mockDb)` — inject mock Firestore
