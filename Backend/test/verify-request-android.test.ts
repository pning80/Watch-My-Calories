/**
 * T1.9.d / T1.8 — Android per-request HMAC verifier.
 *
 * Exercises the locked protocol end-to-end without needing real Play Integrity
 * tokens or Google API access. We seed a synthetic Android key (with a generated
 * androidAssertionSecret) directly into the in-memory cache, then hit
 * /v1beta/models/* with various good and bad headers.
 *
 * Failure modes per T1.8: all reject paths return identical 401 body to avoid
 * leaking which check failed.
 */
import { describe, it, before, beforeEach, after } from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'crypto';
import request from 'supertest';
import {
    app,
    attestedKeys,
    setDb,
    setHmacSecret,
    setAppleRootCa,
    globalLimiter,
    geminiLimiter,
    attestLimiter,
    legacyKeyLimiter,
    keyIdToDocId,
} from '../dist/server';
import { generateP256KeyPair } from './helpers/crypto-fixtures';

const TEST_TEAM_ID = 'TESTTEAMID';

function resetRateLimits() {
    for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
        globalLimiter.resetKey(ip);
        geminiLimiter.resetKey(ip);
        attestLimiter.resetKey(ip);
        legacyKeyLimiter.resetKey('global');
    }
}

/**
 * Build a valid Android assertion for a given body, counter, and secret.
 * Mirrors the client side of the T1.8 protocol.
 */
function buildAndroidAssertion(secretHex: string, counter: number, body: string): string {
    const bodyHash = crypto.createHash('sha256').update(body).digest('hex');
    return crypto.createHmac('sha256', Buffer.from(secretHex, 'hex'))
        .update(`${counter}:${bodyHash}`)
        .digest('hex');
}

/**
 * Mock globalThis.fetch so Gemini relay calls in the test don't hit the network.
 * Returns a small JSON response with status 200.
 */
function stubFetch() {
    const orig = globalThis.fetch;
    globalThis.fetch = (async () => ({
        ok: true,
        status: 200,
        json: async () => ({ candidates: [{ content: { parts: [{ text: '[]' }] } }] }),
    })) as any;
    return () => { globalThis.fetch = orig; };
}

describe('T1.9.d — Android per-request HMAC verifier', () => {
    const origTeamId = process.env.APPLE_TEAM_ID;
    const origApiKey = process.env.APP_BACKEND_API_KEY;
    const origGeminiKey = process.env.GEMINI_API_KEY;
    const origGeminiModel = process.env.GEMINI_MODEL_NAME;

    let keyID: string;
    let secretHex: string;
    let publicKeyPem: string;

    before(() => {
        process.env.APPLE_TEAM_ID = TEST_TEAM_ID;
        process.env.GEMINI_API_KEY = 'fake-gemini-key';
        process.env.GEMINI_MODEL_NAME = 'gemini-2.0-flash';
        setAppleRootCa('placeholder');
        setHmacSecret('android-test-hmac-secret');
    });

    beforeEach(() => {
        attestedKeys.clear();
        setDb(null);
        resetRateLimits();

        // Seed a synthetic Android key directly into the in-memory cache.
        // This is what a successful Android /attest/verify will do once T1.9.b lands.
        keyID = crypto.randomBytes(32).toString('base64');
        secretHex = crypto.randomBytes(32).toString('hex');
        const kp = generateP256KeyPair();
        publicKeyPem = kp.publicKey.export({ type: 'spki', format: 'pem' }) as string;
        attestedKeys.set(keyID, {
            publicKey: kp.publicKey,
            counter: 0,
            hmac: 'unused-on-android-path',
            platform: 'android',
            androidAssertionSecret: secretHex,
        });
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId; else delete process.env.APPLE_TEAM_ID;
        if (origApiKey !== undefined) process.env.APP_BACKEND_API_KEY = origApiKey; else delete process.env.APP_BACKEND_API_KEY;
        if (origGeminiKey !== undefined) process.env.GEMINI_API_KEY = origGeminiKey; else delete process.env.GEMINI_API_KEY;
        if (origGeminiModel !== undefined) process.env.GEMINI_MODEL_NAME = origGeminiModel; else delete process.env.GEMINI_MODEL_NAME;
        setHmacSecret(null);
    });

    describe('happy path', () => {
        it('accepts a correctly-signed Android request', async () => {
            const body = JSON.stringify({ contents: [{ parts: [{ text: 'hi' }] }] });
            const assertion = buildAndroidAssertion(secretHex, 1, body);
            const restore = stubFetch();
            try {
                const res = await request(app)
                    .post('/v1beta/models/test:generateContent')
                    .set('Content-Type', 'application/json')
                    .set('X-Android-Key-Id', keyID)
                    .set('X-Android-Counter', '1')
                    .set('X-Android-Assertion', assertion)
                    .send(body);
                assert.equal(res.status, 200, `expected 200, got ${res.status}: ${JSON.stringify(res.body)}`);
                assert.equal(attestedKeys.get(keyID)!.counter, 1, 'counter should advance');
            } finally {
                restore();
            }
        });

        it('counter advances across successive valid requests', async () => {
            const body = JSON.stringify({ contents: [] });
            const restore = stubFetch();
            try {
                for (const counter of [1, 2, 3]) {
                    const assertion = buildAndroidAssertion(secretHex, counter, body);
                    const res = await request(app)
                        .post('/v1beta/models/test:generateContent')
                        .set('Content-Type', 'application/json')
                        .set('X-Android-Key-Id', keyID)
                        .set('X-Android-Counter', String(counter))
                        .set('X-Android-Assertion', assertion)
                        .send(body);
                    assert.equal(res.status, 200);
                    assert.equal(attestedKeys.get(keyID)!.counter, counter);
                }
            } finally {
                restore();
            }
        });
    });

    describe('failure modes — all return identical 401 body', () => {
        const EXPECTED = { error: 'android_assertion_invalid' };

        it('rejects unknown keyID', async () => {
            const body = JSON.stringify({ contents: [] });
            const assertion = buildAndroidAssertion(secretHex, 1, body);
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', crypto.randomBytes(32).toString('base64'))
                .set('X-Android-Counter', '1')
                .set('X-Android-Assertion', assertion)
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects HMAC over wrong body (tampered body)', async () => {
            const realBody = JSON.stringify({ contents: [{ parts: [{ text: 'real' }] }] });
            const assertion = buildAndroidAssertion(secretHex, 1, realBody);
            const tamperedBody = JSON.stringify({ contents: [{ parts: [{ text: 'tampered' }] }] });
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Counter', '1')
                .set('X-Android-Assertion', assertion) // computed for realBody
                .send(tamperedBody); // but we send tamperedBody
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects HMAC over wrong counter', async () => {
            const body = JSON.stringify({ contents: [] });
            const assertion = buildAndroidAssertion(secretHex, 5, body);
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Counter', '1') // claimed counter
                .set('X-Android-Assertion', assertion) // HMAC says 5
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects HMAC computed with wrong secret', async () => {
            const body = JSON.stringify({ contents: [] });
            const wrongSecret = crypto.randomBytes(32).toString('hex');
            const assertion = buildAndroidAssertion(wrongSecret, 1, body);
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Counter', '1')
                .set('X-Android-Assertion', assertion)
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects counter replay (counter equal to stored)', async () => {
            const body = JSON.stringify({ contents: [] });
            const restore = stubFetch();
            try {
                // Establish stored counter = 1
                const a1 = buildAndroidAssertion(secretHex, 1, body);
                await request(app)
                    .post('/v1beta/models/test:generateContent')
                    .set('Content-Type', 'application/json')
                    .set('X-Android-Key-Id', keyID)
                    .set('X-Android-Counter', '1')
                    .set('X-Android-Assertion', a1)
                    .send(body)
                    .expect(200);

                // Replay counter=1
                const replay = buildAndroidAssertion(secretHex, 1, body);
                const res = await request(app)
                    .post('/v1beta/models/test:generateContent')
                    .set('Content-Type', 'application/json')
                    .set('X-Android-Key-Id', keyID)
                    .set('X-Android-Counter', '1')
                    .set('X-Android-Assertion', replay)
                    .send(body);
                assert.equal(res.status, 401);
                assert.deepEqual(res.body, EXPECTED);
            } finally {
                restore();
            }
        });

        it('rejects counter going backwards', async () => {
            const body = JSON.stringify({ contents: [] });
            const restore = stubFetch();
            try {
                // Advance to counter=5
                const a5 = buildAndroidAssertion(secretHex, 5, body);
                await request(app)
                    .post('/v1beta/models/test:generateContent')
                    .set('Content-Type', 'application/json')
                    .set('X-Android-Key-Id', keyID)
                    .set('X-Android-Counter', '5')
                    .set('X-Android-Assertion', a5)
                    .send(body)
                    .expect(200);

                // Try counter=3 (backwards)
                const a3 = buildAndroidAssertion(secretHex, 3, body);
                const res = await request(app)
                    .post('/v1beta/models/test:generateContent')
                    .set('Content-Type', 'application/json')
                    .set('X-Android-Key-Id', keyID)
                    .set('X-Android-Counter', '3')
                    .set('X-Android-Assertion', a3)
                    .send(body);
                assert.equal(res.status, 401);
                assert.deepEqual(res.body, EXPECTED);
            } finally {
                restore();
            }
        });

        it('rejects missing X-Android-Counter', async () => {
            const body = JSON.stringify({ contents: [] });
            const assertion = buildAndroidAssertion(secretHex, 1, body);
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Assertion', assertion)
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects missing X-Android-Assertion', async () => {
            const body = JSON.stringify({ contents: [] });
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Counter', '1')
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects non-numeric X-Android-Counter', async () => {
            const body = JSON.stringify({ contents: [] });
            const assertion = buildAndroidAssertion(secretHex, 1, body);
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Counter', 'abc')
                .set('X-Android-Assertion', assertion)
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects negative X-Android-Counter', async () => {
            const body = JSON.stringify({ contents: [] });
            const assertion = buildAndroidAssertion(secretHex, 1, body);
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Counter', '-1')
                .set('X-Android-Assertion', assertion)
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects when the seeded key is platform=ios (defense in depth)', async () => {
            // Overwrite the seeded key with an iOS-platformed entry.
            attestedKeys.set(keyID, {
                publicKey: attestedKeys.get(keyID)!.publicKey,
                counter: 0,
                hmac: 'whatever',
                platform: 'ios',
                androidAssertionSecret: secretHex, // even if a secret happens to be present
            });

            const body = JSON.stringify({ contents: [] });
            const assertion = buildAndroidAssertion(secretHex, 1, body);
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Counter', '1')
                .set('X-Android-Assertion', assertion)
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });

        it('rejects when the seeded key has no androidAssertionSecret', async () => {
            attestedKeys.set(keyID, {
                publicKey: attestedKeys.get(keyID)!.publicKey,
                counter: 0,
                hmac: 'whatever',
                platform: 'android',
                // No androidAssertionSecret
            });

            const body = JSON.stringify({ contents: [] });
            const assertion = buildAndroidAssertion(secretHex, 1, body);
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('X-Android-Key-Id', keyID)
                .set('X-Android-Counter', '1')
                .set('X-Android-Assertion', assertion)
                .send(body);
            assert.equal(res.status, 401);
            assert.deepEqual(res.body, EXPECTED);
        });
    });

    describe('Firestore fallback', () => {
        it('loads an Android key from Firestore on cache miss and accepts the request', async () => {
            const body = JSON.stringify({ contents: [] });
            attestedKeys.clear(); // force the fallback path

            // HMAC tamper-check uses the server-level HMAC secret + publicKeyPem + keyID
            const hmacOfDoc = crypto.createHmac('sha256', 'android-test-hmac-secret')
                .update(publicKeyPem + keyID)
                .digest('hex');

            const mockDb = {
                collection: () => ({
                    doc: () => ({
                        get: async () => ({
                            exists: true,
                            data: () => ({
                                publicKeyPem,
                                counter: 0,
                                hmac: hmacOfDoc,
                                platform: 'android',
                                androidAssertionSecret: secretHex,
                            }),
                        }),
                        update: async () => {},
                    }),
                }),
            };
            setDb(mockDb);

            const restore = stubFetch();
            try {
                const assertion = buildAndroidAssertion(secretHex, 1, body);
                const res = await request(app)
                    .post('/v1beta/models/test:generateContent')
                    .set('Content-Type', 'application/json')
                    .set('X-Android-Key-Id', keyID)
                    .set('X-Android-Counter', '1')
                    .set('X-Android-Assertion', assertion)
                    .send(body);
                assert.equal(res.status, 200, `expected 200, got ${res.status}: ${JSON.stringify(res.body)}`);
            } finally {
                restore();
                setDb(null);
            }
        });
    });

    describe('dispatch isolation', () => {
        it('iOS path is unaffected — App Attest headers still route to verifyAppAttestAssertion', async () => {
            // No Android header present → not the Android branch. With only App Attest
            // headers, this exercises the iOS path which (without a valid assertion) will
            // 401 from verifyAppAttestAssertion. Either way, NOT a 200 from the Android
            // path and NOT the android_assertion_invalid body.
            const body = JSON.stringify({ contents: [] });
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .set('x-app-attest-assertion', 'bogus-base64')
                .set('x-app-attest-key-id', 'bogus-key')
                .send(body);
            assert.equal(res.status, 401);
            assert.notDeepEqual(res.body, { error: 'android_assertion_invalid' });
            assert.match(res.body.error, /App Attest assertion/);
        });

        it('no auth headers at all → legacy key path (dev) returns its own 401', async () => {
            process.env.APP_BACKEND_API_KEY = 'whatever';
            const body = JSON.stringify({ contents: [] });
            const res = await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('Content-Type', 'application/json')
                .send(body);
            assert.equal(res.status, 401);
            assert.notDeepEqual(res.body, { error: 'android_assertion_invalid' });
        });
    });
});
