const { describe, it, before, beforeEach, after } = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('crypto');
const request = require('supertest');
const { app, attestedKeys, challenges, setAppleRootCa, setDb, setHmacSecret, globalLimiter, attestLimiter } = require('../server');
const {
    getTestRootCaPem,
    buildAuthData,
    buildAttestationObject,
    generateP256KeyPair,
    buildCoseKey,
    computeRpIdHash,
} = require('./helpers/crypto-fixtures');

const TEST_TEAM_ID = 'TESTTEAMID';

describe('POST /attest/verify', () => {
    let rootCaPem;
    const origTeamId = process.env.APPLE_TEAM_ID;

    before(async () => {
        rootCaPem = await getTestRootCaPem();
        setAppleRootCa(rootCaPem);
        process.env.APPLE_TEAM_ID = TEST_TEAM_ID;
        setHmacSecret('test-hmac-secret');
    });

    beforeEach(() => {
        challenges.clear();
        attestedKeys.clear();
        setDb(null); // Prevent real Firestore from interfering
        // Reset rate limiters to avoid 429s from prior tests
        for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
            globalLimiter.resetKey(ip);
            attestLimiter.resetKey(ip);
        }
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId;
        else delete process.env.APPLE_TEAM_ID;
        setHmacSecret(null);
    });

    async function validAttestation(challengeOverride) {
        // Get a challenge
        let challenge = challengeOverride;
        if (!challenge) {
            const chalRes = await request(app).get('/attest/challenge').expect(200);
            challenge = chalRes.body.challenge;
        }

        const keyPair = generateP256KeyPair();
        const coseKey = buildCoseKey(keyPair.x, keyPair.y);
        const rpIdHash = computeRpIdHash(TEST_TEAM_ID);
        const keyID = crypto.randomBytes(32).toString('base64');
        const credId = Buffer.from(keyID, 'base64');

        const authData = buildAuthData(rpIdHash, coseKey, { credId });
        const attest = await buildAttestationObject(authData, challenge);

        return { challenge, keyID, attestation: attest.base64, keyPair };
    }

    // --- Happy path ---

    it('stores key on valid attestation', async () => {
        const { keyID, attestation, challenge } = await validAttestation();
        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation, challenge })
            .expect(200);

        assert.ok(res.body.success);
        assert.ok(attestedKeys.has(keyID));
        assert.equal(attestedKeys.get(keyID).counter, 0);
    });

    // --- Missing fields ---

    it('rejects missing keyID', async () => {
        const res = await request(app)
            .post('/attest/verify')
            .send({ attestation: 'abc', challenge: 'xyz' })
            .expect(400);
        assert.ok(res.body.error);
    });

    it('rejects missing attestation', async () => {
        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID: 'abc', challenge: 'xyz' })
            .expect(400);
        assert.ok(res.body.error);
    });

    it('rejects missing challenge', async () => {
        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID: 'abc', attestation: 'xyz' })
            .expect(400);
        assert.ok(res.body.error);
    });

    // --- Challenge validation ---

    it('rejects invalid challenge (not in map)', async () => {
        const { keyID, attestation } = await validAttestation();
        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation, challenge: 'nonexistent-challenge' })
            .expect(400);
        assert.match(res.body.error, /Invalid|expired/i);
    });

    it('rejects expired challenge', async () => {
        const challenge = crypto.randomUUID();
        challenges.set(challenge, { createdAt: Date.now() - 61_000 }); // 61s ago

        const { keyID, attestation } = await validAttestation(challenge);
        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation, challenge })
            .expect(400);
        assert.match(res.body.error, /expired/i);
    });

    it('rejects challenge reuse', async () => {
        const { keyID, attestation, challenge } = await validAttestation();
        // First use succeeds
        await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation, challenge })
            .expect(200);

        // Second use fails (challenge consumed)
        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation, challenge })
            .expect(400);
        assert.match(res.body.error, /Invalid|expired/i);
    });

    // --- Attestation format ---

    it('rejects wrong attestation format', async () => {
        const chalRes = await request(app).get('/attest/challenge').expect(200);
        const challenge = chalRes.body.challenge;

        const keyPair = generateP256KeyPair();
        const coseKey = buildCoseKey(keyPair.x, keyPair.y);
        const rpIdHash = computeRpIdHash(TEST_TEAM_ID);
        const authData = buildAuthData(rpIdHash, coseKey);
        const attest = await buildAttestationObject(authData, challenge, { fmt: 'packed' });

        const res = await request(app)
            .post('/attest/verify')
            .send({
                keyID: crypto.randomBytes(32).toString('base64'),
                attestation: attest.base64,
                challenge,
            })
            .expect(400);
        assert.match(res.body.error, /format/i);
    });

    // --- Cert chain ---

    it('rejects cert chain too short', async () => {
        const chalRes = await request(app).get('/attest/challenge').expect(200);
        const challenge = chalRes.body.challenge;

        const keyPair = generateP256KeyPair();
        const coseKey = buildCoseKey(keyPair.x, keyPair.y);
        const rpIdHash = computeRpIdHash(TEST_TEAM_ID);
        const authData = buildAuthData(rpIdHash, coseKey);
        const attest = await buildAttestationObject(authData, challenge, { shortChain: true });

        const res = await request(app)
            .post('/attest/verify')
            .send({
                keyID: crypto.randomBytes(32).toString('base64'),
                attestation: attest.base64,
                challenge,
            })
            .expect(400);
        assert.match(res.body.error, /chain too short/i);
    });

    // --- Nonce mismatch ---

    it('rejects nonce mismatch (wrong challenge in computation)', async () => {
        // Get two different challenges
        const chalRes1 = await request(app).get('/attest/challenge').expect(200);
        const chalRes2 = await request(app).get('/attest/challenge').expect(200);
        const challenge1 = chalRes1.body.challenge;
        const challenge2 = chalRes2.body.challenge;

        const keyPair = generateP256KeyPair();
        const coseKey = buildCoseKey(keyPair.x, keyPair.y);
        const rpIdHash = computeRpIdHash(TEST_TEAM_ID);
        const authData = buildAuthData(rpIdHash, coseKey);
        // Build attestation with challenge1's nonce but submit with challenge2
        const attest = await buildAttestationObject(authData, challenge1);

        const res = await request(app)
            .post('/attest/verify')
            .send({
                keyID: crypto.randomBytes(32).toString('base64'),
                attestation: attest.base64,
                challenge: challenge2,
            })
            .expect(400);
        assert.match(res.body.error, /[Nn]once/);
    });

    // --- RP ID hash mismatch ---

    it('rejects RP ID hash mismatch', async () => {
        const chalRes = await request(app).get('/attest/challenge').expect(200);
        const challenge = chalRes.body.challenge;

        const keyPair = generateP256KeyPair();
        const coseKey = buildCoseKey(keyPair.x, keyPair.y);
        const wrongRpIdHash = crypto.randomBytes(32); // wrong hash
        const authData = buildAuthData(wrongRpIdHash, coseKey);
        const attest = await buildAttestationObject(authData, challenge);

        const res = await request(app)
            .post('/attest/verify')
            .send({
                keyID: crypto.randomBytes(32).toString('base64'),
                attestation: attest.base64,
                challenge,
            })
            .expect(400);
        assert.match(res.body.error, /RP ID|nonce|Nonce/i);
    });

    // --- Invalid COSE key ---

    it('rejects invalid COSE key (missing x/y)', async () => {
        const chalRes = await request(app).get('/attest/challenge').expect(200);
        const challenge = chalRes.body.challenge;

        const rpIdHash = computeRpIdHash(TEST_TEAM_ID);
        // COSE key with no x/y
        const badCoseKey = new Map();
        badCoseKey.set(1, 2); // kty: EC2
        const authData = buildAuthData(rpIdHash, badCoseKey);
        const attest = await buildAttestationObject(authData, challenge);

        const res = await request(app)
            .post('/attest/verify')
            .send({
                keyID: crypto.randomBytes(32).toString('base64'),
                attestation: attest.base64,
                challenge,
            })
            .expect(400);
        assert.ok(res.body.error);
    });

    // --- Firestore persistence ---

    it('writes key to Firestore on successful attestation', async () => {
        let firestoreWrite = null;
        const mockDb = {
            collection: (name) => ({
                doc: (id) => ({
                    set: async (data) => { firestoreWrite = { collection: name, id, data }; },
                }),
            }),
        };
        setDb(mockDb);

        const { keyID, attestation, challenge } = await validAttestation();
        await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation, challenge })
            .expect(200);

        assert.ok(firestoreWrite, 'Firestore set() should have been called');
        assert.equal(firestoreWrite.collection, 'attestedKeys-dev');
        assert.equal(firestoreWrite.id, keyID);
        assert.equal(firestoreWrite.data.counter, 0);
        assert.ok(firestoreWrite.data.publicKeyPem);
        assert.ok(firestoreWrite.data.hmac);
        assert.ok(firestoreWrite.data.createdAt instanceof Date);
        assert.ok(firestoreWrite.data.lastUsedAt instanceof Date);

        setDb(null);
    });

    it('succeeds even when Firestore write fails', async () => {
        const mockDb = {
            collection: () => ({
                doc: () => ({
                    set: async () => { throw new Error('Firestore unavailable'); },
                }),
            }),
        };
        setDb(mockDb);

        const { keyID, attestation, challenge } = await validAttestation();
        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID, attestation, challenge })
            .expect(200);

        assert.ok(res.body.success);
        assert.ok(attestedKeys.has(keyID), 'Key should still be in memory');

        setDb(null);
    });

    // --- Server config errors ---

    it('returns 500 when no Apple root CA configured', async () => {
        const savedCa = rootCaPem;
        setAppleRootCa(null);

        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID: 'abc', attestation: 'xyz', challenge: 'test' })
            .expect(500);
        assert.match(res.body.error, /root CA/i);

        setAppleRootCa(savedCa);
    });

    it('returns 500 when APPLE_TEAM_ID missing', async () => {
        const savedTeamId = process.env.APPLE_TEAM_ID;
        delete process.env.APPLE_TEAM_ID;

        // Need a valid challenge for this to reach the config check
        const chalRes = await request(app).get('/attest/challenge').expect(200);
        const res = await request(app)
            .post('/attest/verify')
            .send({
                keyID: 'abc',
                attestation: 'xyz',
                challenge: chalRes.body.challenge,
            })
            .expect(500);
        assert.match(res.body.error, /configuration/i);

        process.env.APPLE_TEAM_ID = savedTeamId;
    });

    it('returns 500 when HMAC secret missing but APPLE_TEAM_ID present', async () => {
        setHmacSecret(null);

        const chalRes = await request(app).get('/attest/challenge').expect(200);
        const res = await request(app)
            .post('/attest/verify')
            .send({
                keyID: 'abc',
                attestation: 'xyz',
                challenge: chalRes.body.challenge,
            })
            .expect(500);
        assert.match(res.body.error, /configuration/i);

        setHmacSecret('test-hmac-secret');
    });

    // --- Garbled attestation data ---

    it('returns 400 for garbled (non-CBOR) attestation data', async () => {
        const chalRes = await request(app).get('/attest/challenge').expect(200);
        const challenge = chalRes.body.challenge;

        const res = await request(app)
            .post('/attest/verify')
            .send({
                keyID: crypto.randomBytes(32).toString('base64'),
                attestation: Buffer.from('garbage').toString('base64'),
                challenge,
            })
            .expect(400);
        assert.match(res.body.error, /Attestation verification failed/i);
    });
});
