# Backend Test Helpers

Shared test setup used by both iOS-path and Android-path tests. Factored out per `PORTING_CRITERIA.md` T1.9.g to avoid duplication.

## Current contents

| File | Purpose |
|------|---------|
| `crypto-fixtures.ts` | P-256 keypair generation, COSE key encoding, auth-data assembly, attestation-object builder, test root CA — used by `attestation-verify.test.ts` and `assertion-verify.test.ts`. |

## Expected additions (populated during Stage 1)

| File | Purpose |
|------|---------|
| `rate-limiter-reset.ts` | Reset all rate-limit buckets between tests so order-of-execution doesn't matter (today this is inlined in test setup; factor out when adding the Android branch). |
| `firestore-mock.ts` | In-memory Firestore stand-in for offline tests; same surface as `@google-cloud/firestore` for the methods we use. |
| `attestation-stub.ts` | Always-accept App Attest verifier for tests that only care about routing/dispatch (used by `legacy-ios-no-platform-header.test.ts`). |
| `play-integrity-stub.ts` | Always-accept Play Integrity verifier for the Android-path equivalent. |

## Convention

Tests import from `Backend/test/helpers/` rather than defining shared setup inline. If a helper grows iOS- or Android-specific behavior, split it into platform-suffixed files (e.g., `attestation-stub-ios.ts`) rather than branching inside.

The helpers are for *test* code only — never imported from `Backend/src/`.
