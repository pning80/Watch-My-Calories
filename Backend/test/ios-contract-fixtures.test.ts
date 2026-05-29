/**
 * iOS contract fixture sanity tests (PORTING_CRITERIA.md T1.10.c).
 *
 * These tests don't replay the captured iOS bytes through the Express app
 * (that's a follow-up; see PORTING_RUNBOOK.md Stage 0.3 / Phase 6.1). They
 * lock down the *shape* of the captured fixtures so:
 *
 *   1. If anyone re-captures with a different redaction scheme, this test
 *      fails and the snapshot pipeline gets fixed before stale fixtures land.
 *   2. If the Stage 1 backend ever drifts away from the captured iOS shapes,
 *      a future structural-replay test can hold it accountable against these
 *      same files.
 *
 * The fixtures themselves are the load-bearing artifact for T1.10.b and
 * T1.10.h — captured against pre-port revision
 * `watchmycalories-backend-dev-00016-xmw` (2026-05-28) per
 * `Backend/test/contract/ios/baseline-latency.md`.
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';

const FIXTURES_DIR = join(__dirname, 'contract', 'ios');
const readJson = (name: string) => JSON.parse(readFileSync(join(FIXTURES_DIR, name), 'utf8'));

describe('iOS contract fixtures (T1.10.c)', () => {
    describe('/attest/challenge', () => {
        it('response has the expected shape (single redacted `challenge` field)', () => {
            const body = readJson('attest-challenge.response.json');
            assert.deepEqual(Object.keys(body).sort(), ['challenge']);
            assert.equal(body.challenge, '__CHALLENGE__', 'challenge should be redacted in committed fixture');
        });
    });

    describe('/attest/verify', () => {
        it('request body has the three iOS-side fields the verify handler expects', () => {
            const body = readJson('attest-verify.request.json');
            assert.deepEqual(Object.keys(body).sort(), ['attestation', 'challenge', 'keyID']);
            assert.equal(body.keyID, '__KEY_ID__');
            assert.equal(body.challenge, '__CHALLENGE__');
            assert.equal(body.attestation, '__ATTESTATION_B64__');
        });

        it('response is the success-only shape iOS already handles', () => {
            const body = readJson('attest-verify.response.json');
            assert.deepEqual(body, { success: true });
        });
    });

    describe('/v1beta/models/default:generateContent', () => {
        it('response has Gemini-API top-level keys (no shape drift)', () => {
            const body = readJson('gemini-generate-content.response.json');
            assert.deepEqual(
                Object.keys(body).sort(),
                ['candidates', 'modelVersion', 'responseId', 'usageMetadata'],
                'Gemini response shape must not regress — iOS parsing depends on these keys',
            );
        });

        it('candidate has content.parts[0].text shaped for Watch My Calories JSON parsing', () => {
            const body = readJson('gemini-generate-content.response.json');
            const part = body.candidates[0].content.parts[0];
            assert.ok(typeof part.text === 'string', 'parts[0].text must be a string');
            // The text payload is itself a JSON string with an `items` array — verify it parses.
            const parsed = JSON.parse(part.text);
            assert.ok(Array.isArray(parsed.items), '.text must parse to {items: [...]}');
            assert.ok(parsed.items.length > 0, 'captured response should have ≥1 item');
            for (const item of parsed.items) {
                assert.deepEqual(
                    Object.keys(item).sort(),
                    ['calories', 'carbs', 'confidence', 'fat', 'name', 'protein', 'quantity'].sort(),
                    'each item must have all 7 iOS-expected fields',
                );
            }
        });
    });

    describe('429 rate-limit response', () => {
        it('preserves the iOS-facing error shape + Retry-After header', () => {
            const body = readJson('rate-limit-429.response.json');
            assert.equal(body._meta.status, 429);
            assert.match(body._meta.retryAfter, /^\d+$/, 'Retry-After must be a numeric seconds value');
            assert.ok(typeof body.body.error === 'string', 'body.error must be a string the iOS client surfaces');
        });
    });

    // Raw .bin HTTP-request captures are intentionally NOT checked into the repo:
    // they embed real iPhone App Attest blobs + a real JPEG, and the test we would
    // write against them (structural replay) doesn't exist yet. When that test is
    // written, captures will be regenerated via `Backend/scripts/mitm-capture.py`
    // either at CI time (downloaded from artifact storage) or synthesized at test
    // setup time (deterministic, no real device data). See README.md in this dir.

    describe('latency baseline', () => {
        it('baseline-latency.md exists and pins a Cloud Run revision SHA', () => {
            const md = readFileSync(join(FIXTURES_DIR, 'baseline-latency.md'), 'utf8');
            assert.match(md, /Pinned revision:\s*`watchmycalories-backend-dev-\d+-\w+`/, 'must pin a revision for T1.10.g rollback');
            assert.match(md, /\/attest\/challenge\s*\|\s*\d+\s*\|\s*\d+\s*\|\s*\d+/, 'must contain attest-challenge p50/p95/mean row');
            assert.match(md, /\/v1beta\/models\/default:generateContent\s*\|\s*\d+\s*\|\s*\d+\s*\|\s*\d+/, 'must contain gemini p50/p95/mean row');
        });
    });
});
