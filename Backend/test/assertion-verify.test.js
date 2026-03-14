const { describe, it, before, beforeEach, after } = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('crypto');
const request = require('supertest');
const { app, attestedKeys, setAppleRootCa, setDb, setHmacSecret } = require('../dist/server');
const {
    generateP256KeyPair,
    buildAssertionObject,
    buildAssertionAuthData,
    signAssertion,
    computeRpIdHash,
} = require('./helpers/crypto-fixtures');

const TEST_TEAM_ID = 'TESTTEAMID';

describe('App Attest assertion verification', () => {
    const origTeamId = process.env.APPLE_TEAM_ID;
    const origApiKey = process.env.APP_BACKEND_API_KEY;
    let rpIdHash;

    before(() => {
        process.env.APPLE_TEAM_ID = TEST_TEAM_ID;
        process.env.APP_BACKEND_API_KEY = 'test-key';
        setHmacSecret('test-hmac-secret');
        rpIdHash = computeRpIdHash(TEST_TEAM_ID);
    });

    beforeEach(() => {
        attestedKeys.clear();
        setDb(null);
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId;
        else delete process.env.APPLE_TEAM_ID;
        if (origApiKey !== undefined) process.env.APP_BACKEND_API_KEY = origApiKey;
        else delete process.env.APP_BACKEND_API_KEY;
        setHmacSecret(null);
    });

    function registerKey(keyID, publicKey, counter = 0) {
        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' });
        const hmac = crypto.createHmac('sha256', 'test-hmac-secret')
            .update(publicKeyPem + keyID)
            .digest('hex');
        attestedKeys.set(keyID, { publicKey, counter, hmac });
    }

    async function makeAssertedRequest(keyID, privateKey, counter, body = { contents: [] }) {
        const bodyStr = JSON.stringify(body);
        const bodyBuf = Buffer.from(bodyStr);
        const clientDataHash = crypto.createHash('sha256').update(bodyBuf).digest();

        const authData = buildAssertionAuthData(rpIdHash, counter);
        const signature = signAssertion(privateKey, authData, clientDataHash);
        const assertionBase64 = buildAssertionObject(authData, signature);

        return request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-app-attest-assertion', assertionBase64)
            .set('x-app-attest-key-id', keyID)
            .set('Content-Type', 'application/json')
            .send(bodyStr);
    }

    // --- Happy path ---

    it('passes through with valid assertion', async () => {
        const keyID = 'test-key-1';
        const { publicKey, privateKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 0);

        const res = await makeAssertedRequest(keyID, privateKey, 1);
        // Should pass auth (not 401). May get 500 for missing GEMINI_API_KEY, which is fine.
        assert.notEqual(res.status, 401);
    });

    it('increments counter across multiple assertions', async () => {
        const keyID = 'test-key-counter';
        const { publicKey, privateKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 0);

        await makeAssertedRequest(keyID, privateKey, 1);
        assert.equal(attestedKeys.get(keyID).counter, 1);

        await makeAssertedRequest(keyID, privateKey, 2);
        assert.equal(attestedKeys.get(keyID).counter, 2);

        await makeAssertedRequest(keyID, privateKey, 5); // can skip
        assert.equal(attestedKeys.get(keyID).counter, 5);
    });

    // --- Error paths ---

    it('rejects counter not incrementing (replay)', async () => {
        const keyID = 'test-key-replay';
        const { publicKey, privateKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 5);

        const res = await makeAssertedRequest(keyID, privateKey, 3); // counter 3 <= 5
        assert.equal(res.status, 401);
    });

    it('rejects unknown key ID', async () => {
        const { publicKey, privateKey } = generateP256KeyPair();
        const res = await makeAssertedRequest('unknown-key', privateKey, 1);
        assert.equal(res.status, 401);
    });

    it('rejects missing authenticatorData', async () => {
        const keyID = 'test-key-no-auth';
        const { publicKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 0);

        const { encode } = require('cbor-x');
        const malformedAssertion = encode({ signature: Buffer.alloc(64) }).toString('base64');

        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-app-attest-assertion', malformedAssertion)
            .set('x-app-attest-key-id', keyID)
            .set('Content-Type', 'application/json')
            .send(JSON.stringify({ contents: [] }));
        assert.equal(res.status, 401);
    });

    it('rejects missing signature', async () => {
        const keyID = 'test-key-no-sig';
        const { publicKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 0);

        const { encode } = require('cbor-x');
        const authData = buildAssertionAuthData(rpIdHash, 1);
        const malformedAssertion = encode({ authenticatorData: authData }).toString('base64');

        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-app-attest-assertion', malformedAssertion)
            .set('x-app-attest-key-id', keyID)
            .set('Content-Type', 'application/json')
            .send(JSON.stringify({ contents: [] }));
        assert.equal(res.status, 401);
    });

    it('rejects invalid signature (wrong key)', async () => {
        const keyID = 'test-key-wrong-sig';
        const { publicKey } = generateP256KeyPair();
        const { privateKey: wrongPrivateKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 0);

        const res = await makeAssertedRequest(keyID, wrongPrivateKey, 1);
        assert.equal(res.status, 401);
    });

    it('rejects RP ID hash mismatch', async () => {
        const keyID = 'test-key-rpid';
        const { publicKey, privateKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 0);

        const body = { contents: [] };
        const bodyBuf = Buffer.from(JSON.stringify(body));
        const clientDataHash = crypto.createHash('sha256').update(bodyBuf).digest();

        // Use wrong RP ID hash
        const wrongRpIdHash = crypto.randomBytes(32);
        const authData = buildAssertionAuthData(wrongRpIdHash, 1);
        const signature = signAssertion(privateKey, authData, clientDataHash);
        const assertionBase64 = buildAssertionObject(authData, signature);

        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-app-attest-assertion', assertionBase64)
            .set('x-app-attest-key-id', keyID)
            .set('Content-Type', 'application/json')
            .send(JSON.stringify(body));
        assert.equal(res.status, 401);
    });

    // --- Firestore fallback ---

    it('fetches key from Firestore when not in memory', async () => {
        const keyID = 'test-firestore-key';
        const { publicKey, privateKey } = generateP256KeyPair();
        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' });
        const hmac = crypto.createHmac('sha256', 'test-hmac-secret')
            .update(publicKeyPem + keyID)
            .digest('hex');

        // Mock Firestore
        const mockDb = {
            collection: () => ({
                doc: () => ({
                    get: async () => ({
                        exists: true,
                        data: () => ({
                            publicKeyPem,
                            counter: 0,
                            hmac,
                        }),
                    }),
                    update: async () => {},
                }),
            }),
        };
        setDb(mockDb);

        // Key is NOT in attestedKeys — should be fetched from Firestore
        const res = await makeAssertedRequest(keyID, privateKey, 1);
        assert.notEqual(res.status, 401);
        // Should now be cached in memory
        assert.ok(attestedKeys.has(keyID));
    });

    it('rejects Firestore key with tampered HMAC', async () => {
        const keyID = 'test-firestore-tampered';
        const { publicKey, privateKey } = generateP256KeyPair();
        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' });

        const mockDb = {
            collection: () => ({
                doc: () => ({
                    get: async () => ({
                        exists: true,
                        data: () => ({
                            publicKeyPem,
                            counter: 0,
                            hmac: 'tampered-hmac-value-that-is-definitely-wrong!!',
                        }),
                    }),
                }),
            }),
        };
        setDb(mockDb);

        const res = await makeAssertedRequest(keyID, privateKey, 1);
        assert.equal(res.status, 401);
    });

    it('rejects when key not found anywhere', async () => {
        const keyID = 'test-nowhere-key';
        const { privateKey } = generateP256KeyPair();

        const mockDb = {
            collection: () => ({
                doc: () => ({
                    get: async () => ({ exists: false }),
                }),
            }),
        };
        setDb(mockDb);

        const res = await makeAssertedRequest(keyID, privateKey, 1);
        assert.equal(res.status, 401);
    });

    it('succeeds even when Firestore counter update fails', async () => {
        const keyID = 'test-key-fs-update-fail';
        const { publicKey, privateKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 0);

        const mockDb = {
            collection: () => ({
                doc: () => ({
                    update: async () => { throw new Error('Firestore update failed'); },
                }),
            }),
        };
        setDb(mockDb);

        const res = await makeAssertedRequest(keyID, privateKey, 1);
        assert.notEqual(res.status, 401);
        // In-memory counter should still be updated
        assert.equal(attestedKeys.get(keyID).counter, 1);
    });

    // --- RP ID hash check when APPLE_TEAM_ID is not set ---

    it('skips RP ID hash check when APPLE_TEAM_ID is not set', async () => {
        const savedTeamId = process.env.APPLE_TEAM_ID;
        delete process.env.APPLE_TEAM_ID;

        const keyID = 'test-key-no-teamid';
        const { publicKey, privateKey } = generateP256KeyPair();
        registerKey(keyID, publicKey, 0);

        // Build assertion with wrong RP ID hash but sign correctly
        const body = { contents: [] };
        const bodyStr = JSON.stringify(body);
        const bodyBuf = Buffer.from(bodyStr);
        const clientDataHash = crypto.createHash('sha256').update(bodyBuf).digest();

        const wrongRpIdHash = crypto.randomBytes(32);
        const authData = buildAssertionAuthData(wrongRpIdHash, 1);
        const signature = signAssertion(privateKey, authData, clientDataHash);
        const assertionBase64 = buildAssertionObject(authData, signature);

        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-app-attest-assertion', assertionBase64)
            .set('x-app-attest-key-id', keyID)
            .set('Content-Type', 'application/json')
            .send(bodyStr);

        // Should NOT be 401 because RP ID check is skipped when APPLE_TEAM_ID is absent
        assert.notEqual(res.status, 401);

        process.env.APPLE_TEAM_ID = savedTeamId;
    });
});
