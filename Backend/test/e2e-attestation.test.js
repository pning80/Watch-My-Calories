const { describe, it, before, beforeEach, after } = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('crypto');
const request = require('supertest');
const { app, attestedKeys, challenges, setAppleRootCa, setDb, setHmacSecret, globalLimiter, geminiLimiter, attestLimiter, legacyKeyLimiter } = require('../server');
const {
    getTestRootCaPem,
    buildAuthData,
    buildAttestationObject,
    buildAssertionObject,
    buildAssertionAuthData,
    generateP256KeyPair,
    buildCoseKey,
    computeRpIdHash,
    signAssertion,
} = require('./helpers/crypto-fixtures');

const TEST_TEAM_ID = 'TESTTEAMID';

describe('End-to-end attestation → assertion flow', () => {
    let rootCaPem;
    let rpIdHash;
    const origTeamId = process.env.APPLE_TEAM_ID;
    const origApiKey = process.env.APP_BACKEND_API_KEY;
    const origGeminiKey = process.env.GEMINI_API_KEY;
    const origGeminiModel = process.env.GEMINI_MODEL_NAME;

    before(async () => {
        rootCaPem = await getTestRootCaPem();
        setAppleRootCa(rootCaPem);
        rpIdHash = computeRpIdHash(TEST_TEAM_ID);
        process.env.APPLE_TEAM_ID = TEST_TEAM_ID;
        process.env.APP_BACKEND_API_KEY = 'test-key';
        process.env.GEMINI_API_KEY = 'fake-gemini-key';
        process.env.GEMINI_MODEL_NAME = 'gemini-2.0-flash';
        setHmacSecret('e2e-hmac-secret');
    });

    beforeEach(() => {
        challenges.clear();
        attestedKeys.clear();
        setDb(null);
        // Reset all rate limiters
        for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
            globalLimiter.resetKey(ip);
            geminiLimiter.resetKey(ip);
            attestLimiter.resetKey(ip);
            legacyKeyLimiter.resetKey('global');
        }
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId;
        else delete process.env.APPLE_TEAM_ID;
        if (origApiKey !== undefined) process.env.APP_BACKEND_API_KEY = origApiKey;
        else delete process.env.APP_BACKEND_API_KEY;
        if (origGeminiKey !== undefined) process.env.GEMINI_API_KEY = origGeminiKey;
        else delete process.env.GEMINI_API_KEY;
        if (origGeminiModel !== undefined) process.env.GEMINI_MODEL_NAME = origGeminiModel;
        else delete process.env.GEMINI_MODEL_NAME;
        setHmacSecret(null);
    });

    /**
     * Perform a full attestation: get challenge, build attestation, verify.
     * Returns { keyID, keyPair } on success.
     */
    async function performAttestation() {
        const chalRes = await request(app).get('/attest/challenge').expect(200);
        const challenge = chalRes.body.challenge;

        const keyPair = generateP256KeyPair();
        const coseKey = buildCoseKey(keyPair.x, keyPair.y);
        const keyID = crypto.randomBytes(32).toString('base64');
        const credId = Buffer.from(keyID, 'base64');
        const authData = buildAuthData(rpIdHash, coseKey, { credId });
        const attest = await buildAttestationObject(authData, challenge);

        await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation: attest.base64, challenge })
            .expect(200);

        return { keyID, keyPair };
    }

    /**
     * Make an asserted request (assertion auth through Gemini proxy).
     * Mock fetch to avoid real Gemini calls.
     */
    async function makeAssertedRequest(keyID, privateKey, counter, body = { contents: [] }) {
        const bodyStr = JSON.stringify(body);
        const bodyBuf = Buffer.from(bodyStr);
        const clientDataHash = crypto.createHash('sha256').update(bodyBuf).digest();

        const authData = buildAssertionAuthData(rpIdHash, counter);
        const signature = signAssertion(privateKey, authData, clientDataHash);
        const assertionBase64 = buildAssertionObject(authData, signature);

        // Mock globalThis.fetch to avoid real Gemini calls
        const origFetch = globalThis.fetch;
        globalThis.fetch = async () => ({
            ok: true,
            status: 200,
            json: async () => ({ candidates: [{ content: { parts: [{ text: '[]' }] } }] }),
        });

        try {
            return await request(app)
                .post('/v1beta/models/test:generateContent')
                .set('x-app-attest-assertion', assertionBase64)
                .set('x-app-attest-key-id', keyID)
                .set('Content-Type', 'application/json')
                .send(bodyStr);
        } finally {
            globalThis.fetch = origFetch;
        }
    }

    it('attested key can be used for subsequent assertions', async () => {
        const { keyID, keyPair } = await performAttestation();

        const res = await makeAssertedRequest(keyID, keyPair.privateKey, 1);
        assert.notEqual(res.status, 401, 'Assertion should pass after attestation');
        assert.equal(res.status, 200);
    });

    it('counter increments correctly across multiple assertions after attestation', async () => {
        const { keyID, keyPair } = await performAttestation();

        const res1 = await makeAssertedRequest(keyID, keyPair.privateKey, 1);
        assert.equal(res1.status, 200);
        assert.equal(attestedKeys.get(keyID).counter, 1);

        const res2 = await makeAssertedRequest(keyID, keyPair.privateKey, 2);
        assert.equal(res2.status, 200);
        assert.equal(attestedKeys.get(keyID).counter, 2);
    });

    it('second attestation with same keyID overwrites the first', async () => {
        // Attest key pair A
        const chalRes1 = await request(app).get('/attest/challenge').expect(200);
        const keyPairA = generateP256KeyPair();
        const coseKeyA = buildCoseKey(keyPairA.x, keyPairA.y);
        const keyID = crypto.randomBytes(32).toString('base64');
        const credIdA = Buffer.from(keyID, 'base64');
        const authDataA = buildAuthData(rpIdHash, coseKeyA, { credId: credIdA });
        const attestA = await buildAttestationObject(authDataA, chalRes1.body.challenge);

        await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation: attestA.base64, challenge: chalRes1.body.challenge })
            .expect(200);

        // Attest key pair B with same keyID
        const chalRes2 = await request(app).get('/attest/challenge').expect(200);
        const keyPairB = generateP256KeyPair();
        const coseKeyB = buildCoseKey(keyPairB.x, keyPairB.y);
        const credIdB = Buffer.from(keyID, 'base64');
        const authDataB = buildAuthData(rpIdHash, coseKeyB, { credId: credIdB });
        const attestB = await buildAttestationObject(authDataB, chalRes2.body.challenge);

        await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation: attestB.base64, challenge: chalRes2.body.challenge })
            .expect(200);

        // Assert with key A should fail (overwritten)
        const resA = await makeAssertedRequest(keyID, keyPairA.privateKey, 1);
        assert.equal(resA.status, 401, 'Old key pair A should be rejected after re-attestation');

        // Assert with key B should succeed
        const resB = await makeAssertedRequest(keyID, keyPairB.privateKey, 1);
        assert.equal(resB.status, 200, 'New key pair B should work after re-attestation');
    });

    it('assertion fails after HMAC secret changes (Firestore fallback)', async () => {
        // Attest with current HMAC secret
        const { keyID, keyPair } = await performAttestation();

        // Verify assertion works before change
        const res1 = await makeAssertedRequest(keyID, keyPair.privateKey, 1);
        assert.equal(res1.status, 200);

        // Save the key data that was stored in memory
        const keyData = attestedKeys.get(keyID);
        const publicKeyPem = keyData.publicKey.export({ type: 'spki', format: 'pem' });
        const oldHmac = keyData.hmac;

        // Clear in-memory cache to force Firestore lookup
        attestedKeys.clear();

        // Mock Firestore with the old key data (HMAC was computed with old secret)
        const mockDb = {
            collection: () => ({
                doc: () => ({
                    get: async () => ({
                        exists: true,
                        data: () => ({
                            publicKeyPem,
                            counter: 1,
                            hmac: oldHmac,
                        }),
                    }),
                    update: async () => {},
                }),
            }),
        };
        setDb(mockDb);

        // Change HMAC secret — Firestore key's HMAC no longer matches
        setHmacSecret('new-different-hmac-secret');

        const res2 = await makeAssertedRequest(keyID, keyPair.privateKey, 2);
        assert.equal(res2.status, 401, 'Assertion should fail when HMAC secret has changed');

        // Restore for other tests
        setHmacSecret('e2e-hmac-secret');
    });
});

describe('Assertion HMAC hard-fail', () => {
    const origTeamId = process.env.APPLE_TEAM_ID;
    const origApiKey = process.env.APP_BACKEND_API_KEY;

    before(() => {
        process.env.APPLE_TEAM_ID = TEST_TEAM_ID;
        process.env.APP_BACKEND_API_KEY = 'test-key';
    });

    beforeEach(() => {
        attestedKeys.clear();
        // Reset rate limiters
        for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
            globalLimiter.resetKey(ip);
            geminiLimiter.resetKey(ip);
            attestLimiter.resetKey(ip);
            legacyKeyLimiter.resetKey('global');
        }
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId;
        else delete process.env.APPLE_TEAM_ID;
        if (origApiKey !== undefined) process.env.APP_BACKEND_API_KEY = origApiKey;
        else delete process.env.APP_BACKEND_API_KEY;
        setHmacSecret(null);
    });

    it('returns 401 when HMAC secret is null and key is only in Firestore', async () => {
        const rpIdHash = computeRpIdHash(TEST_TEAM_ID);
        const keyID = 'hmac-hard-fail-key';
        const { publicKey, privateKey } = generateP256KeyPair();
        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' });
        const hmac = crypto.createHmac('sha256', 'some-secret')
            .update(publicKeyPem + keyID)
            .digest('hex');

        // Mock Firestore with a valid-looking key
        const mockDb = {
            collection: () => ({
                doc: () => ({
                    get: async () => ({
                        exists: true,
                        data: () => ({ publicKeyPem, counter: 0, hmac }),
                    }),
                }),
            }),
        };
        setDb(mockDb);

        // HMAC secret is null — assertion.js should hard-fail at line 22-23
        setHmacSecret(null);

        const body = { contents: [] };
        const bodyStr = JSON.stringify(body);
        const bodyBuf = Buffer.from(bodyStr);
        const clientDataHash = crypto.createHash('sha256').update(bodyBuf).digest();

        const authData = buildAssertionAuthData(rpIdHash, 1);
        const signature = signAssertion(privateKey, authData, clientDataHash);
        const assertionBase64 = buildAssertionObject(authData, signature);

        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-app-attest-assertion', assertionBase64)
            .set('x-app-attest-key-id', keyID)
            .set('Content-Type', 'application/json')
            .send(bodyStr);

        assert.equal(res.status, 401, 'Should return 401 when HMAC secret is unavailable');

        setDb(null);
    });
});
