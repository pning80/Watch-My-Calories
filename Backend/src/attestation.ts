import crypto, { X509Certificate } from 'crypto';
import express, { Express } from 'express';
import { decode, Decoder } from 'cbor-x';
import { getAppleRootCa } from './apple-root-ca';
import { challenges } from './challenge';
import { extractNonceFromCert } from './cert-utils';
import { attestedKeys } from './attested-keys';
import { getDb } from './firestore';
import { BUNDLE_ID, ATTESTED_KEYS_COLLECTION, CHALLENGE_TTL_MS } from './constants';
import { getHmacSecret } from './hmac-secret';
import { createLogger } from './logger';
import { counters } from './metrics';
import { keyIdToDocId } from './firestore-key';
import { resolvePlatform } from './platform';
import { verifyPlayIntegrityToken, DecoderFn } from './play-integrity';

/**
 * Optional override for the Play Integrity decoder, used by tests to stub the
 * Google API call. Production code leaves this null and the verifier uses ADC.
 */
let androidDecoderOverride: DecoderFn | null = null;
export function setAndroidDecoderForTest(fn: DecoderFn | null): void {
    androidDecoderOverride = fn;
}

const log = createLogger('attestation');

export function registerRoutes(app: Express, attestLimiter: any): void {
    // POST /attest/verify — verifies attestation and stores the public key
    app.post('/attest/verify', attestLimiter, express.json({ limit: '1mb' }), async (req, res) => {
        try {
            // Platform dispatch (PORTING_CRITERIA.md T1.9.a, T1.10.b).
            // Missing/unknown X-App-Platform → 'ios', preserving in-field iOS client behavior.
            const platform = resolvePlatform(req.headers['x-app-platform']);
            counters.increment('attest_verify_total', { platform });
            if (platform === 'android') {
                return handleAndroidAttestVerify(req, res);
            }

            const { keyID, attestation, challenge } = req.body;

            if (!keyID || !attestation || !challenge) {
                return res.status(400).json({ error: 'Missing keyID, attestation, or challenge.' });
            }

            const appleRootCaPem = getAppleRootCa();
            if (!appleRootCaPem) {
                return res.status(500).json({ error: 'App Attest root CA not configured.' });
            }

            if (!process.env.APPLE_TEAM_ID || !getHmacSecret()) {
                return res.status(500).json({ error: 'App Attest server configuration incomplete.' });
            }

            // Validate challenge
            const challengeEntry = challenges.get(challenge);
            if (!challengeEntry) {
                return res.status(400).json({ error: 'Invalid or expired challenge.' });
            }
            if (Date.now() - challengeEntry.createdAt > CHALLENGE_TTL_MS) {
                challenges.delete(challenge);
                return res.status(400).json({ error: 'Challenge expired.' });
            }
            challenges.delete(challenge); // One-time use

            // Decode CBOR attestation
            const attestationBuffer = Buffer.from(attestation, 'base64');
            const attestObj = decode(attestationBuffer);

            const fmt = attestObj.fmt;
            if (fmt !== 'apple-appattest') {
                return res.status(400).json({ error: `Unexpected attestation format: ${fmt}` });
            }

            const attStmt = attestObj.attStmt;
            const authData = attestObj.authData;

            if (!attStmt || !attStmt.x5c || !authData) {
                return res.status(400).json({ error: 'Malformed attestation object.' });
            }

            // Build certificate chain
            const certs = attStmt.x5c.map((certDer: Buffer) => {
                const pem = `-----BEGIN CERTIFICATE-----\n${Buffer.from(certDer).toString('base64').match(/.{1,64}/g)!.join('\n')}\n-----END CERTIFICATE-----`;
                return new X509Certificate(pem);
            });

            if (certs.length < 2) {
                return res.status(400).json({ error: 'Certificate chain too short.' });
            }

            const leafCert = certs[0];
            const intermediateCert = certs[1];
            const rootCert = new X509Certificate(appleRootCaPem);

            // Verify certificate chain
            if (!intermediateCert.verify(rootCert.publicKey)) {
                return res.status(400).json({ error: 'Intermediate cert not signed by Apple root.' });
            }
            if (!leafCert.verify(intermediateCert.publicKey)) {
                return res.status(400).json({ error: 'Leaf cert not signed by intermediate.' });
            }

            // Verify nonce: SHA256(authData || SHA256(challenge))
            const challengeHash = crypto.createHash('sha256').update(challenge).digest();
            const nonceData = Buffer.concat([Buffer.from(authData), challengeHash]);
            const expectedNonce = crypto.createHash('sha256').update(nonceData).digest();

            // Extract nonce from leaf cert extension OID 1.2.840.113635.100.8.2
            const nonceCertExt = extractNonceFromCert(leafCert);
            if (!nonceCertExt) {
                return res.status(400).json({ error: 'Could not extract nonce from certificate.' });
            }

            if (!crypto.timingSafeEqual(expectedNonce, nonceCertExt)) {
                return res.status(400).json({ error: 'Nonce verification failed.' });
            }

            // Verify RP ID hash (first 32 bytes of authData)
            const rpIdHash = Buffer.from(authData).subarray(0, 32);
            const expectedRpIdHash = crypto.createHash('sha256')
                .update(`${process.env.APPLE_TEAM_ID}.${BUNDLE_ID}`)
                .digest();

            if (!crypto.timingSafeEqual(rpIdHash, expectedRpIdHash)) {
                return res.status(400).json({ error: 'RP ID hash mismatch.' });
            }

            // Extract credential public key from authData
            // authData format: rpIdHash(32) + flags(1) + counter(4) + aaguid(16) + credIdLen(2) + credId(credIdLen) + credentialPublicKey(CBOR)
            const authDataBuf = Buffer.from(authData);
            if (authDataBuf.length < 55) {
                return res.status(400).json({ error: 'authData too short.' });
            }
            const credIdLen = authDataBuf.readUInt16BE(53); // offset 32+1+4+16 = 53
            const coseKeyOffset = 55 + credIdLen; // 53 + 2 + credIdLen
            const coseKeyData = authDataBuf.subarray(coseKeyOffset);
            // COSE keys use negative integer map keys (-1, -2, -3) which cbor-x
            // can only represent as a Map, not a plain object.
            const coseDecoder = new Decoder({ mapsAsObjects: false });
            const coseKey = coseDecoder.decode(coseKeyData);

            // COSE EC2 key: -1 = curve (1 = P-256), -2 = x, -3 = y
            const x = coseKey.get(-2) as Buffer | undefined;
            const y = coseKey.get(-3) as Buffer | undefined;

            if (!x || !y) {
                return res.status(400).json({ error: 'Invalid COSE key in attestation.' });
            }

            // Validate COSE key x/y are 32 bytes each (P-256)
            if (Buffer.from(x).length !== 32 || Buffer.from(y).length !== 32) {
                return res.status(400).json({ error: 'Invalid COSE key coordinates.' });
            }

            // Convert to uncompressed point format (0x04 || x || y) and create KeyObject
            const publicKey = crypto.createPublicKey({
                key: {
                    kty: 'EC',
                    crv: 'P-256',
                    x: Buffer.from(x).toString('base64url'),
                    y: Buffer.from(y).toString('base64url'),
                },
                format: 'jwk',
            });

            const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' }) as string;

            // Compute HMAC for tamper detection
            const hmac = crypto.createHmac('sha256', getHmacSecret()!)
                .update(publicKeyPem + keyID)
                .digest('hex');

            // Store in memory — iOS path explicitly tags platform='ios' (T1.9.c)
            attestedKeys.set(keyID, { publicKey, counter: 0, hmac, platform: 'ios' });

            // Persist to Firestore (awaited to guarantee durability across cold starts).
            // The `platform` field is always written for new docs going forward; existing
            // iOS docs written before this change read back as `undefined` and are treated
            // as 'ios' by callers (see attested-keys.ts and types.ts).
            const db = getDb();
            if (db) {
                try {
                    await db.collection(ATTESTED_KEYS_COLLECTION).doc(keyIdToDocId(keyID)).set({
                        publicKeyPem,
                        counter: 0,
                        hmac,
                        createdAt: new Date(),
                        lastUsedAt: new Date(),
                        platform: 'ios',
                    });
                } catch (err: any) {
                    log.error({ keyId: keyID.substring(0, 8), error: err.message }, 'Firestore write error (attestation)');
                }
            }

            counters.increment('attestation_total', { result: 'success' });
            log.info({ keyId: keyID.substring(0, 8) }, 'Key attested successfully');
            res.json({ success: true });

        } catch (err: any) {
            counters.increment('attestation_total', { result: 'error' });
            log.error({ error: err.message }, 'Attestation verification error');
            res.status(400).json({ error: 'Attestation verification failed.' });
        }
    });
}

/**
 * Android `/attest/verify` handler — Play Integrity path (T1.9.b + T1.8).
 *
 * Body shape (sent by the Android client):
 *   { keyID, attestation: <play-integrity-token>, challenge }
 *
 * On success returns:
 *   { success: true, androidAssertionSecret: "<hex>" }
 *
 * The secret is returned ONCE — the client stores it in EncryptedSharedPreferences
 * and uses it to sign every subsequent /v1beta/models/* request (see T1.8 +
 * verify-request-android.ts). Loss of the secret requires re-attestation.
 *
 * All failure modes return 401 with a stable `{ error }` body. The error code
 * indicates the *category* (so the client can decide whether to retry or back
 * off), but does not leak Google's internal verdict reasons.
 */
async function handleAndroidAttestVerify(req: express.Request, res: express.Response): Promise<express.Response | void> {
    const { keyID, attestation, challenge } = req.body || {};

    if (!keyID || !attestation || !challenge) {
        counters.increment('android_attest_total', { result: 'missing_field' });
        return res.status(400).json({ error: 'missing_field' });
    }

    const packageName = process.env.PLAY_INTEGRITY_PACKAGE_NAME;
    if (!packageName) {
        // Server not yet configured for Play Integrity. iOS path remains unaffected
        // (this branch is only reached when X-App-Platform=android). T1.10.e.
        log.warn('PLAY_INTEGRITY_PACKAGE_NAME unset — Android attest unavailable');
        counters.increment('android_attest_total', { result: 'not_configured' });
        return res.status(503).json({ error: 'play_integrity_not_configured' });
    }

    // Validate challenge (same TTL semantics as iOS path)
    const challengeEntry = challenges.get(challenge);
    if (!challengeEntry) {
        counters.increment('android_attest_total', { result: 'bad_challenge' });
        return res.status(401).json({ error: 'attestation_invalid' });
    }
    if (Date.now() - challengeEntry.createdAt > CHALLENGE_TTL_MS) {
        challenges.delete(challenge);
        counters.increment('android_attest_total', { result: 'expired_challenge' });
        return res.status(401).json({ error: 'attestation_invalid' });
    }
    challenges.delete(challenge); // one-time use, same as iOS

    // Verify the Play Integrity token. Decoder may be overridden by tests.
    try {
        await verifyPlayIntegrityToken({
            token: attestation,
            challenge,
            packageName,
            decoder: androidDecoderOverride ?? undefined,
        });
    } catch (err: any) {
        counters.increment('android_attest_total', { result: err.message });
        log.warn({ reason: err.message, keyId: String(keyID).substring(0, 8) }, 'Play Integrity verification failed');
        return res.status(401).json({ error: 'attestation_invalid' });
    }

    // Issue the per-key HMAC secret used for subsequent request signing (T1.8).
    const androidAssertionSecret = crypto.randomBytes(32).toString('hex');

    // HMAC for tamper detection on the Firestore key doc, using the same formula
    // as the iOS path (HMAC(serverSecret, publicKeyPem + keyID)) so verify-request-
    // android.ts can validate Android docs with the same routine. Android keys have
    // no real public key, so we use a placeholder PEM (SPKI of a throwaway key) as
    // the "publicKeyPem" — it's just bytes that participate in tamper detection.
    //
    // NOTE: this scheme protects against substitution of publicKeyPem but does NOT
    // include androidAssertionSecret in the HMAC. An attacker with Firestore write
    // access could swap in a different secret, causing the client's HMACs to fail
    // (a DoS — the client recovers via re-attest on 401). The runtime SA's IAM is
    // restrictive enough that this is acceptable for now; tighten if the threat
    // model changes.
    const hmacSecret = getHmacSecret();
    if (!hmacSecret) {
        log.error('HMAC secret unavailable — cannot persist attested Android key');
        return res.status(500).json({ error: 'server_misconfigured' });
    }
    const placeholderKey = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' }).publicKey;
    const placeholderPem = placeholderKey.export({ type: 'spki', format: 'pem' }) as string;
    const hmac = crypto.createHmac('sha256', hmacSecret)
        .update(placeholderPem + keyID)
        .digest('hex');

    // Store in memory. publicKey is required by AttestedKeyData (iOS shape) but
    // is unused on the Android path; the placeholder keeps the cache layout uniform.
    attestedKeys.set(keyID, {
        publicKey: placeholderKey,
        counter: 0,
        hmac,
        platform: 'android',
        androidAssertionSecret,
    });

    // Persist to Firestore. The placeholder PEM round-trips through attested-keys.ts
    // loadKeysFromFirestore unchanged because the load path doesn't actually use the
    // public key for verification on Android — it just reconstructs the cache entry.
    const db = getDb();
    if (db) {
        try {
            await db.collection(ATTESTED_KEYS_COLLECTION).doc(keyIdToDocId(keyID)).set({
                publicKeyPem: placeholderPem,
                counter: 0,
                hmac,
                createdAt: new Date(),
                lastUsedAt: new Date(),
                platform: 'android',
                androidAssertionSecret,
            });
        } catch (err: any) {
            log.error({ keyId: String(keyID).substring(0, 8), error: err.message }, 'Firestore write error (android attestation)');
        }
    }

    counters.increment('android_attest_total', { result: 'success' });
    log.info({ keyId: String(keyID).substring(0, 8) }, 'Android key attested successfully');
    return res.json({ success: true, androidAssertionSecret });
}
