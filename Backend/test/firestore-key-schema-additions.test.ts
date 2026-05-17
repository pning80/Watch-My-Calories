/**
 * T1.9.c — Firestore key schema additions.
 *
 * Covers:
 *  - The iOS attestation write path persists `platform: 'ios'` on the doc.
 *  - The in-memory `attestedKeys` entry after iOS attest carries `platform: 'ios'`.
 *  - `loadKeysFromFirestore` tolerates docs **without** the `platform` field
 *    (backward-compat for iOS docs written before this change — T1.10.d).
 *  - `loadKeysFromFirestore` passes through `platform: 'android'` and
 *    `androidAssertionSecret` when present.
 */
import { describe, it, before, beforeEach, after } from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'crypto';
import request from 'supertest';
import {
    app,
    attestedKeys,
    challenges,
    setAppleRootCa,
    setDb,
    setHmacSecret,
    globalLimiter,
    attestLimiter,
    loadKeysFromFirestore,
    keyIdToDocId,
} from '../dist/server';
import {
    getTestRootCaPem,
    buildAuthData,
    buildAttestationObject,
    generateP256KeyPair,
    buildCoseKey,
    computeRpIdHash,
} from './helpers/crypto-fixtures';

const TEST_TEAM_ID = 'TESTTEAMID';
const TEST_HMAC_SECRET = 'schema-test-hmac-secret';

function resetRateLimits() {
    for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
        globalLimiter.resetKey(ip);
        attestLimiter.resetKey(ip);
    }
}

describe('T1.9.c — Firestore key schema additions', () => {
    let rootCaPem: string;
    let rpIdHash: Buffer;
    const origTeamId = process.env.APPLE_TEAM_ID;

    before(async () => {
        rootCaPem = await getTestRootCaPem();
        setAppleRootCa(rootCaPem);
        rpIdHash = computeRpIdHash(TEST_TEAM_ID);
        process.env.APPLE_TEAM_ID = TEST_TEAM_ID;
        setHmacSecret(TEST_HMAC_SECRET);
    });

    beforeEach(() => {
        challenges.clear();
        attestedKeys.clear();
        setDb(null);
        resetRateLimits();
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId;
        else delete process.env.APPLE_TEAM_ID;
        setHmacSecret(null);
    });

    /**
     * Run a full successful iOS attestation flow and return the keyID.
     * Optionally provide a Firestore mock to capture the write.
     */
    async function attestIosKey(mockDb?: any): Promise<string> {
        if (mockDb) setDb(mockDb);

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

        return keyID;
    }

    it('in-memory entry after iOS attest carries platform="ios"', async () => {
        const keyID = await attestIosKey();
        const entry = attestedKeys.get(keyID);
        assert.ok(entry, 'key should be in the in-memory cache');
        assert.equal(entry.platform, 'ios');
        assert.equal(entry.androidAssertionSecret, undefined);
    });

    it('Firestore write includes platform="ios"', async () => {
        let captured: any = null;
        const mockDb = {
            collection: () => ({
                doc: () => ({
                    set: async (data: any) => { captured = data; },
                }),
            }),
        };

        await attestIosKey(mockDb);

        assert.ok(captured, 'Firestore set should have been called');
        assert.equal(captured.platform, 'ios');
        // Pre-existing required fields still present
        assert.ok(captured.publicKeyPem);
        assert.equal(captured.counter, 0);
        assert.ok(captured.hmac);
        assert.ok(captured.createdAt instanceof Date);
        assert.ok(captured.lastUsedAt instanceof Date);
    });

    it('loadKeysFromFirestore tolerates docs WITHOUT platform field (backward-compat for old iOS docs)', async () => {
        // Synthesize a "legacy" iOS doc — written before T1.9.c, has no `platform`.
        // keyID must be real base64 so keyIdToDocId / docIdToKeyId round-trips cleanly,
        // otherwise the load path's HMAC check fails.
        const keyPair = generateP256KeyPair();
        const publicKeyPem = keyPair.publicKey.export({ type: 'spki', format: 'pem' }) as string;
        const keyID = crypto.randomBytes(32).toString('base64');
        const hmac = crypto.createHmac('sha256', TEST_HMAC_SECRET)
            .update(publicKeyPem + keyID)
            .digest('hex');

        const recentDate = new Date(); // within the preload window
        const mockDb = {
            collection: () => ({
                where: () => ({
                    get: async () => ({
                        docs: [
                            {
                                id: keyIdToDocId(keyID),
                                data: () => ({
                                    publicKeyPem,
                                    counter: 3,
                                    hmac,
                                    createdAt: recentDate,
                                    lastUsedAt: recentDate,
                                    // No platform, no androidAssertionSecret — the legacy shape
                                }),
                            },
                        ],
                    }),
                }),
            }),
        };

        setDb(mockDb);
        const loaded = await loadKeysFromFirestore();
        assert.equal(loaded, 1);

        const entry = attestedKeys.get(keyID);
        assert.ok(entry, 'legacy doc should have loaded into the cache');
        assert.equal(entry.counter, 3);
        assert.equal(entry.platform, undefined, 'platform field is undefined when not present on the doc — callers treat as ios');
        assert.equal(entry.androidAssertionSecret, undefined);
    });

    it('loadKeysFromFirestore passes through platform="android" and androidAssertionSecret', async () => {
        const keyPair = generateP256KeyPair();
        const publicKeyPem = keyPair.publicKey.export({ type: 'spki', format: 'pem' }) as string;
        const keyID = crypto.randomBytes(32).toString('base64');
        const hmac = crypto.createHmac('sha256', TEST_HMAC_SECRET)
            .update(publicKeyPem + keyID)
            .digest('hex');
        const androidSecret = crypto.randomBytes(32).toString('hex');

        const recentDate = new Date();
        const mockDb = {
            collection: () => ({
                where: () => ({
                    get: async () => ({
                        docs: [
                            {
                                id: keyIdToDocId(keyID),
                                data: () => ({
                                    publicKeyPem,
                                    counter: 7,
                                    hmac,
                                    createdAt: recentDate,
                                    lastUsedAt: recentDate,
                                    platform: 'android',
                                    androidAssertionSecret: androidSecret,
                                }),
                            },
                        ],
                    }),
                }),
            }),
        };

        setDb(mockDb);
        const loaded = await loadKeysFromFirestore();
        assert.equal(loaded, 1);

        const entry = attestedKeys.get(keyID);
        assert.ok(entry, 'android doc should have loaded');
        assert.equal(entry.platform, 'android');
        assert.equal(entry.androidAssertionSecret, androidSecret);
        assert.equal(entry.counter, 7);
    });

    it('loadKeysFromFirestore passes through platform="ios" when explicitly stored', async () => {
        // Newly-written iOS docs (post-T1.9.c) have explicit platform: 'ios'.
        const keyPair = generateP256KeyPair();
        const publicKeyPem = keyPair.publicKey.export({ type: 'spki', format: 'pem' }) as string;
        const keyID = crypto.randomBytes(32).toString('base64');
        const hmac = crypto.createHmac('sha256', TEST_HMAC_SECRET)
            .update(publicKeyPem + keyID)
            .digest('hex');

        const recentDate = new Date();
        const mockDb = {
            collection: () => ({
                where: () => ({
                    get: async () => ({
                        docs: [
                            {
                                id: keyIdToDocId(keyID),
                                data: () => ({
                                    publicKeyPem,
                                    counter: 0,
                                    hmac,
                                    createdAt: recentDate,
                                    lastUsedAt: recentDate,
                                    platform: 'ios',
                                    // No androidAssertionSecret on iOS docs.
                                }),
                            },
                        ],
                    }),
                }),
            }),
        };

        setDb(mockDb);
        const loaded = await loadKeysFromFirestore();
        assert.equal(loaded, 1);

        const entry = attestedKeys.get(keyID);
        assert.equal(entry.platform, 'ios');
        assert.equal(entry.androidAssertionSecret, undefined);
    });
});
