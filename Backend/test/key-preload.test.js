const { describe, it, before, beforeEach, after } = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('crypto');
const { attestedKeys, setDb, setHmacSecret, loadKeysFromFirestore } = require('../server');

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

    function makeKeyDoc(keyID, opts = {}) {
        const { publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' });
        const hmac = crypto.createHmac('sha256', TEST_HMAC_SECRET)
            .update(publicKeyPem + keyID)
            .digest('hex');

        return {
            id: keyID,
            data: {
                publicKeyPem,
                counter: opts.counter ?? 0,
                hmac: opts.hmac ?? hmac,
                createdAt: opts.createdAt ?? new Date(),
                lastUsedAt: opts.lastUsedAt ?? new Date(),
            },
        };
    }

    function createMockDb(docs) {
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
                doc: (id) => ({
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
        const doc1 = makeKeyDoc('key-1');
        const doc2 = makeKeyDoc('key-2', { counter: 42 });

        setDb(createMockDb([doc1, doc2]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 2);
        assert.ok(attestedKeys.has('key-1'));
        assert.ok(attestedKeys.has('key-2'));
        assert.equal(attestedKeys.get('key-1').counter, 0);
        assert.equal(attestedKeys.get('key-2').counter, 42);
    });

    it('returns 0 when no keys in Firestore', async () => {
        setDb(createMockDb([]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 0);
        assert.equal(attestedKeys.size, 0);
    });

    it('loaded keys can be used for assertion verification', async () => {
        const doc = makeKeyDoc('key-usable');
        setDb(createMockDb([doc]));

        await loadKeysFromFirestore();

        const keyData = attestedKeys.get('key-usable');
        assert.ok(keyData.publicKey);
        assert.equal(typeof keyData.publicKey.export, 'function');
        assert.equal(keyData.hmac, doc.data.hmac);
    });

    // --- HMAC tamper detection ---

    it('skips keys with invalid HMAC', async () => {
        const goodDoc = makeKeyDoc('key-good');
        const badDoc = makeKeyDoc('key-tampered', { hmac: 'wrong-hmac-value-definitely-bad!!' });

        setDb(createMockDb([goodDoc, badDoc]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 1);
        assert.ok(attestedKeys.has('key-good'));
        assert.ok(!attestedKeys.has('key-tampered'));
    });

    // --- Invalid key material ---

    it('skips keys with invalid PEM', async () => {
        const goodDoc = makeKeyDoc('key-valid');
        const badDoc = makeKeyDoc('key-bad-pem');
        badDoc.data.publicKeyPem = 'not-a-valid-pem';
        // Recompute HMAC to match the bad PEM (so HMAC passes but key parsing fails)
        badDoc.data.hmac = crypto.createHmac('sha256', TEST_HMAC_SECRET)
            .update('not-a-valid-pem' + 'key-bad-pem')
            .digest('hex');

        setDb(createMockDb([goodDoc, badDoc]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 1);
        assert.ok(attestedKeys.has('key-valid'));
        assert.ok(!attestedKeys.has('key-bad-pem'));
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

        setDb(createMockDb([makeKeyDoc('key-orphan')]));

        const loaded = await loadKeysFromFirestore();

        assert.equal(loaded, 0);

        setHmacSecret(TEST_HMAC_SECRET);
    });
});
