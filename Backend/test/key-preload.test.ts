import { describe, it, before, beforeEach, after } from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'crypto';
import { attestedKeys, setDb, setHmacSecret, loadKeysFromFirestore, keyIdToDocId } from '../dist/server';

const TEST_HMAC_SECRET = 'test-hmac-secret';

describe('Cold-start key preload from Firestore', () => {
    before(() => {
        setHmacSecret(TEST_HMAC_SECRET);
    });

    beforeEach(() => {
        attestedKeys.clear();
        setDb(null);
    });

    after(() => {
        setHmacSecret(null);
        setDb(null);
    });

    function makeKeyDoc(keyID: string, opts: { counter?: number; hmac?: string; createdAt?: Date; lastUsedAt?: Date } = {}) {
        const { publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' }) as string;
        const hmac = crypto.createHmac('sha256', TEST_HMAC_SECRET)
            .update(publicKeyPem + keyID)
            .digest('hex');

        return {
            // Firestore doc ID is base64url-encoded (matches attestation.ts)
            id: keyIdToDocId(keyID),
            data: {
                publicKeyPem,
                counter: opts.counter ?? 0,
                hmac: opts.hmac ?? hmac,
                createdAt: opts.createdAt ?? new Date(),
                lastUsedAt: opts.lastUsedAt ?? new Date(),
            },
        };
    }

    function createMockDb(docs: ReturnType<typeof makeKeyDoc>[]) {
        return {
            collection: () => ({
                where: () => ({
                    get: async () => ({
                        docs: docs.map(d => ({
                            id: d.id,
                            data: () => d.data,
                        })),
                    }),
                }),
                doc: (id: string) => ({
                    set: async () => {},
                    update: async () => {},
                    get: async () => {
                        const found = docs.find(d => d.id === id);
                        return found
                            ? { exists: true, data: () => found.data }
                            : { exists: false };
                    },
                }),
            }),
        };
    }

    // --- Happy path ---

    it('loads keys from Firestore into memory', async () => {
        const doc1 = makeKeyDoc('a2V5LTE=');
        const doc2 = makeKeyDoc('a2V5LTI=', { counter: 42 });

        setDb(createMockDb([doc1, doc2]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 2);
        assert.ok(attestedKeys.has('a2V5LTE='));
        assert.ok(attestedKeys.has('a2V5LTI='));
        assert.equal(attestedKeys.get('a2V5LTE=').counter, 0);
        assert.equal(attestedKeys.get('a2V5LTI=').counter, 42);
    });

    it('returns 0 when no keys in Firestore', async () => {
        setDb(createMockDb([]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 0);
        assert.equal(attestedKeys.size, 0);
    });

    it('loaded keys can be used for assertion verification', async () => {
        const doc = makeKeyDoc('a2V5LXVzYWJsZQ==');
        setDb(createMockDb([doc]));

        await loadKeysFromFirestore();

        const keyData = attestedKeys.get('a2V5LXVzYWJsZQ==');
        assert.ok(keyData.publicKey);
        assert.equal(typeof keyData.publicKey.export, 'function');
        assert.equal(keyData.hmac, doc.data.hmac);
    });

    // --- HMAC tamper detection ---

    it('skips keys with invalid HMAC', async () => {
        const goodDoc = makeKeyDoc('a2V5LWdvb2Q=');
        const badDoc = makeKeyDoc('a2V5LXRhbXBlcmVk', { hmac: 'wrong-hmac-value-definitely-bad!!' });

        setDb(createMockDb([goodDoc, badDoc]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 1);
        assert.ok(attestedKeys.has('a2V5LWdvb2Q='));
        assert.ok(!attestedKeys.has('a2V5LXRhbXBlcmVk'));
    });

    // --- Invalid key material ---

    it('skips keys with invalid PEM', async () => {
        const goodDoc = makeKeyDoc('a2V5LXZhbGlk');
        const badDoc = makeKeyDoc('a2V5LWJhZC1wZW0=');
        badDoc.data.publicKeyPem = 'not-a-valid-pem';
        // Recompute HMAC to match the bad PEM (so HMAC passes but key parsing fails)
        badDoc.data.hmac = crypto.createHmac('sha256', TEST_HMAC_SECRET)
            .update('not-a-valid-pem' + 'a2V5LWJhZC1wZW0=')
            .digest('hex');

        setDb(createMockDb([goodDoc, badDoc]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 1);
        assert.ok(attestedKeys.has('a2V5LXZhbGlk'));
        assert.ok(!attestedKeys.has('a2V5LWJhZC1wZW0='));
    });

    // --- Firestore unavailable ---

    it('returns 0 when Firestore is not configured', async () => {
        setDb(null);

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 0);
        assert.equal(attestedKeys.size, 0);
    });

    it('returns 0 when Firestore query fails', async () => {
        setDb({
            collection: () => ({
                where: () => ({
                    get: async () => { throw new Error('Firestore connection lost'); },
                }),
            }),
        });

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 0);
        assert.equal(attestedKeys.size, 0);
    });

    // --- Missing HMAC secret ---

    it('returns 0 when HMAC secret is not available', async () => {
        setHmacSecret(null);

        setDb(createMockDb([makeKeyDoc('a2V5LW9ycGhhbg==')]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 0);

        setHmacSecret(TEST_HMAC_SECRET);
    });
});
