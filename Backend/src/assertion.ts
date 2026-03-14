import crypto from 'crypto';
import { decode } from 'cbor-x';
import { attestedKeys } from './attested-keys';
import { getDb } from './firestore';
import { BUNDLE_ID, ATTESTED_KEYS_COLLECTION } from './constants';
import { getHmacSecret } from './hmac-secret';
import { createLogger } from './logger';
import { counters } from './metrics';
import { keyIdToDocId } from './firestore-key';
import { RawBodyRequest } from './types';
import { Request } from 'express';

const log = createLogger('assertion');

export async function verifyAppAttestAssertion(req: Request, assertionBase64: string, keyID: string): Promise<void> {
    const db = getDb();

    // Look up stored key — in-memory first, then Firestore
    let keyData = attestedKeys.get(keyID);

    if (keyData) {
        counters.increment('key_cache_total', { result: 'hit' });
    }

    if (!keyData && db) {
        counters.increment('key_cache_total', { result: 'firestore_fallback' });
        // Fetch from Firestore
        const doc = await db.collection(ATTESTED_KEYS_COLLECTION).doc(keyIdToDocId(keyID)).get();
        if (doc.exists) {
            const data = doc.data()!;

            // Verify HMAC (tamper detection) — hard-fail if secret unavailable
            const hmacSecret = getHmacSecret();
            if (!hmacSecret) {
                throw new Error('HMAC secret unavailable — cannot verify Firestore key integrity.');
            }
            const expectedHmac = crypto.createHmac('sha256', hmacSecret)
                .update(data.publicKeyPem + keyID)
                .digest('hex');
            if (!crypto.timingSafeEqual(Buffer.from(expectedHmac), Buffer.from(data.hmac))) {
                throw new Error('HMAC verification failed — possible tampering.');
            }

            const publicKey = crypto.createPublicKey({
                key: data.publicKeyPem,
                format: 'pem',
            });
            keyData = { publicKey, counter: data.counter, hmac: data.hmac };
            attestedKeys.set(keyID, keyData); // Populate cache
        }
    }

    if (!keyData && !db) {
        counters.increment('key_cache_total', { result: 'miss' });
    }

    if (!keyData) {
        counters.increment('assertion_total', { result: 'error' });
        throw new Error('Key not attested.');
    }

    // Decode CBOR assertion
    const assertionBuffer = Buffer.from(assertionBase64, 'base64');
    const assertion = decode(assertionBuffer);

    const authenticatorData = assertion.authenticatorData || assertion.get?.('authenticatorData');
    const signature = assertion.signature || assertion.get?.('signature');

    if (!authenticatorData || !signature) {
        counters.increment('assertion_total', { result: 'invalid' });
        throw new Error('Malformed assertion.');
    }

    const authDataBuf = Buffer.from(authenticatorData);

    // Verify RP ID hash
    if (process.env.APPLE_TEAM_ID) {
        const rpIdHash = authDataBuf.subarray(0, 32);
        const expectedRpIdHash = crypto.createHash('sha256')
            .update(`${process.env.APPLE_TEAM_ID}.${BUNDLE_ID}`)
            .digest();
        if (!crypto.timingSafeEqual(rpIdHash, expectedRpIdHash)) {
            counters.increment('assertion_total', { result: 'invalid' });
            throw new Error('RP ID hash mismatch.');
        }
    }

    // Verify counter is incrementing (replay protection)
    const counter = authDataBuf.readUInt32BE(33); // offset: rpIdHash(32) + flags(1)
    if (counter <= keyData.counter) {
        counters.increment('assertion_total', { result: 'replay' });
        throw new Error(`Counter not incrementing: got ${counter}, expected > ${keyData.counter}.`);
    }

    // Compute client data hash from raw request body
    const clientDataHash = crypto.createHash('sha256').update((req as RawBodyRequest).rawBody).digest();

    // Compute composite hash: SHA256(authenticatorData || clientDataHash)
    const compositeHash = crypto.createHash('sha256')
        .update(Buffer.concat([authDataBuf, clientDataHash]))
        .digest();

    // Verify ECDSA signature
    const isValid = crypto.verify(
        'sha256',
        compositeHash,
        { key: keyData.publicKey, dsaEncoding: 'der' },
        Buffer.from(signature)
    );

    if (!isValid) {
        counters.increment('assertion_total', { result: 'invalid' });
        throw new Error('Signature verification failed.');
    }

    counters.increment('assertion_total', { result: 'success' });

    // Update counter
    keyData.counter = counter;

    // Async Firestore counter update (fire-and-forget)
    if (db) {
        db.collection(ATTESTED_KEYS_COLLECTION).doc(keyIdToDocId(keyID)).update({
            counter,
            lastUsedAt: new Date(),
        }).catch((err: any) => log.error({ keyId: keyID.substring(0, 8), error: err.message }, 'Firestore counter update error'));
    }
}
