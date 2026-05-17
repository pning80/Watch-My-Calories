/**
 * Android per-request integrity verifier (PORTING_CRITERIA.md T1.8 + T1.9.d).
 *
 * Locked protocol (T1.8):
 *  - Client sends three headers on every /v1beta/models/* request:
 *      X-Android-Key-Id:   the attested keyID returned by /attest/verify
 *      X-Android-Counter:  monotonically increasing integer, persisted by the client
 *      X-Android-Assertion: hex( HMAC-SHA256(secret, counter || ":" || sha256(body)) )
 *  - Server retrieves the per-key `androidAssertionSecret` (issued at /attest/verify
 *    for Android, T1.8 step 1) and verifies the HMAC in constant time.
 *  - Counter must be strictly greater than the last-stored value (replay protection).
 *  - On success: persist the new counter (in-memory + Firestore async).
 *  - On any failure: 401 with body `{"error":"android_assertion_invalid"}`.
 *    No information leakage about which check failed.
 *
 * Mirrors the iOS pattern in assertion.ts (cache-first lookup, Firestore fallback,
 * HMAC tamper check on Firestore reads, async counter writeback).
 */
import crypto from 'crypto';
import { Request, Response, NextFunction } from 'express';
import { attestedKeys } from './attested-keys';
import { getDb } from './firestore';
import { ATTESTED_KEYS_COLLECTION } from './constants';
import { getHmacSecret } from './hmac-secret';
import { createLogger } from './logger';
import { counters } from './metrics';
import { keyIdToDocId } from './firestore-key';
import { RawBodyRequest } from './types';

const log = createLogger('verify-request-android');

const ANDROID_ASSERTION_ERROR = { error: 'android_assertion_invalid' };

export async function verifyAndroidRequest(req: Request, res: Response, next: NextFunction): Promise<void> {
    const keyID = req.headers['x-android-key-id'] as string | undefined;
    const counterStr = req.headers['x-android-counter'] as string | undefined;
    const assertion = req.headers['x-android-assertion'] as string | undefined;

    try {
        if (!keyID || !counterStr || !assertion) {
            throw new Error('missing_header');
        }

        const counter = Number.parseInt(counterStr, 10);
        if (!Number.isFinite(counter) || counter < 0 || String(counter) !== counterStr.trim()) {
            throw new Error('bad_counter');
        }

        // Load key — in-memory first (cache hit), then Firestore fallback (mirror of
        // assertion.ts iOS path). HMAC tamper-detection on the Firestore read uses the
        // server-level HMAC secret, same as iOS.
        let keyData = attestedKeys.get(keyID);
        const db = getDb();

        if (keyData) {
            counters.increment('android_key_cache_total', { result: 'hit' });
        }

        if (!keyData && db) {
            counters.increment('android_key_cache_total', { result: 'firestore_fallback' });
            const doc = await db.collection(ATTESTED_KEYS_COLLECTION).doc(keyIdToDocId(keyID)).get();
            if (doc.exists) {
                const data = doc.data()!;
                const hmacSecret = getHmacSecret();
                if (!hmacSecret) {
                    throw new Error('hmac_secret_unavailable');
                }
                const expectedHmac = crypto.createHmac('sha256', hmacSecret)
                    .update(data.publicKeyPem + keyID)
                    .digest('hex');
                const expectedBuf = Buffer.from(expectedHmac);
                const actualBuf = Buffer.from(data.hmac);
                if (expectedBuf.length !== actualBuf.length || !crypto.timingSafeEqual(expectedBuf, actualBuf)) {
                    throw new Error('firestore_hmac_mismatch');
                }
                // Materialize the in-memory entry. publicKey is required by the type but
                // is unused on the Android path; we still construct it so the cache layout
                // matches iOS and helpers don't have to special-case the Android entry.
                const publicKey = crypto.createPublicKey({ key: data.publicKeyPem, format: 'pem' });
                keyData = {
                    publicKey,
                    counter: data.counter,
                    hmac: data.hmac,
                    platform: data.platform,
                    androidAssertionSecret: data.androidAssertionSecret,
                };
                attestedKeys.set(keyID, keyData);
            }
        }

        if (!keyData) {
            throw new Error('unknown_key');
        }
        if (keyData.platform !== 'android') {
            // Defense in depth — an iOS key being used on the Android path is a bug or
            // an attack. Treated identically to unknown_key from the client's perspective.
            throw new Error('platform_mismatch');
        }
        if (!keyData.androidAssertionSecret) {
            throw new Error('missing_secret');
        }
        if (counter <= keyData.counter) {
            throw new Error('counter_replay');
        }

        // Recompute the HMAC: secret over (counter || ":" || sha256(body)).
        const bodyHash = crypto.createHash('sha256').update((req as RawBodyRequest).rawBody).digest('hex');
        const expected = crypto.createHmac('sha256', Buffer.from(keyData.androidAssertionSecret, 'hex'))
            .update(`${counter}:${bodyHash}`)
            .digest('hex');

        const expectedBuf = Buffer.from(expected);
        const providedBuf = Buffer.from(assertion);
        if (expectedBuf.length !== providedBuf.length || !crypto.timingSafeEqual(expectedBuf, providedBuf)) {
            throw new Error('hmac_mismatch');
        }

        // Success — persist the new counter. In-memory first (source of truth for
        // subsequent requests in the same process). Firestore async fire-and-forget
        // matches the iOS pattern; a missed Firestore write resurfaces as a Firestore
        // fallback on a future cold start, which is acceptable.
        keyData.counter = counter;
        if (db) {
            db.collection(ATTESTED_KEYS_COLLECTION).doc(keyIdToDocId(keyID)).update({
                counter,
                lastUsedAt: new Date(),
            }).catch((err: any) => log.error({ keyId: keyID.substring(0, 8), error: err.message }, 'Firestore counter update error (android)'));
        }

        counters.increment('android_assertion_total', { result: 'success' });
        return next();
    } catch (err: any) {
        // Log the specific reason server-side; client sees only the generic error.
        counters.increment('android_assertion_total', { result: err.message });
        log.warn({
            keyId: keyID ? keyID.substring(0, 8) : undefined,
            reason: err.message,
        }, 'Android assertion rejected');
        res.status(401).json(ANDROID_ASSERTION_ERROR);
    }
}
