import crypto from 'crypto';
import { getDb } from './firestore';
import { ATTESTED_KEYS_COLLECTION, KEY_PRELOAD_MAX_AGE_MS } from './constants';
import { getHmacSecret } from './hmac-secret';
import { createLogger } from './logger';
import { docIdToKeyId } from './firestore-key';
import { AttestedKeyData } from './types';

const log = createLogger('attested-keys');

export const attestedKeys = new Map<string, AttestedKeyData>();

export async function loadKeysFromFirestore(): Promise<number> {
    const db = getDb();
    if (!db) {
        log.info('Key preload: Firestore unavailable — skipping');
        return 0;
    }

    const hmacSecret = getHmacSecret();
    if (!hmacSecret) {
        log.warn('Key preload: HMAC secret not available — skipping');
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
            const keyID = docIdToKeyId(doc.id);

            // Verify HMAC before trusting Firestore data
            const expectedHmacBuf = Buffer.from(
                crypto.createHmac('sha256', hmacSecret)
                    .update(data.publicKeyPem + keyID)
                    .digest('hex')
            );
            const actualHmacBuf = Buffer.from(data.hmac);

            if (expectedHmacBuf.length !== actualHmacBuf.length ||
                !crypto.timingSafeEqual(expectedHmacBuf, actualHmacBuf)) {
                log.warn({ keyId: keyID.substring(0, 8) }, 'Key preload: Skipping — HMAC mismatch');
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
            } catch (err: any) {
                log.warn({ keyId: keyID.substring(0, 8), error: err.message }, 'Key preload: Skipping — invalid key');
                skipped++;
            }
        }

        log.info({ loaded, skipped }, 'Key preload complete');
        return loaded;
    } catch (err: any) {
        log.error({ error: err.message }, 'Key preload: Firestore query failed');
        return 0;
    }
}
