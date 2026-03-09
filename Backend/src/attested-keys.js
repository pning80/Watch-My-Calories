const crypto = require('crypto');
const { getDb } = require('./firestore');
const { ATTESTED_KEYS_COLLECTION, KEY_PRELOAD_MAX_AGE_MS } = require('./constants');

const attestedKeys = new Map(); // keyID -> { publicKey (KeyObject), counter, hmac }

async function loadKeysFromFirestore() {
    const db = getDb();
    if (!db) {
        console.log('Key preload: Firestore unavailable — skipping.');
        return 0;
    }

    const hmacSecret = process.env.ATTEST_HMAC_SECRET;
    if (!hmacSecret) {
        console.warn('Key preload: ATTEST_HMAC_SECRET not set — skipping.');
        return 0;
    }

    try {
        const cutoff = new Date(Date.now() - KEY_PRELOAD_MAX_AGE_MS);
        const snapshot = await db.collection(ATTESTED_KEYS_COLLECTION)
            .where('lastUsedAt', '>=', cutoff)
            .get();

        let loaded = 0;
        let skipped = 0;

        for (const doc of snapshot.docs) {
            const data = doc.data();
            const keyID = doc.id;

            // Verify HMAC before trusting Firestore data
            const expectedHmacBuf = Buffer.from(
                crypto.createHmac('sha256', hmacSecret)
                    .update(data.publicKeyPem + keyID)
                    .digest('hex')
            );
            const actualHmacBuf = Buffer.from(data.hmac);

            if (expectedHmacBuf.length !== actualHmacBuf.length ||
                !crypto.timingSafeEqual(expectedHmacBuf, actualHmacBuf)) {
                console.warn(`Key preload: Skipping ${keyID.substring(0, 8)}... — HMAC mismatch.`);
                skipped++;
                continue;
            }

            try {
                const publicKey = crypto.createPublicKey({
                    key: data.publicKeyPem,
                    format: 'pem',
                });
                attestedKeys.set(keyID, { publicKey, counter: data.counter, hmac: data.hmac });
                loaded++;
            } catch (err) {
                console.warn(`Key preload: Skipping ${keyID.substring(0, 8)}... — invalid key: ${err.message}`);
                skipped++;
            }
        }

        console.log(`Key preload: Loaded ${loaded} key(s) from Firestore${skipped > 0 ? `, skipped ${skipped}` : ''}.`);
        return loaded;
    } catch (err) {
        console.error('Key preload: Firestore query failed:', err.message);
        return 0;
    }
}

module.exports = { attestedKeys, loadKeysFromFirestore };
