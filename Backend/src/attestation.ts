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

const log = createLogger('attestation');

export function registerRoutes(app: Express, attestLimiter: any): void {
    // POST /attest/verify — verifies attestation and stores the public key
    app.post('/attest/verify', attestLimiter, express.json({ limit: '1mb' }), async (req, res) => {
        try {
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

            // Store in memory
            attestedKeys.set(keyID, { publicKey, counter: 0, hmac });

            // Persist to Firestore (awaited to guarantee durability across cold starts)
            const db = getDb();
            if (db) {
                try {
                    await db.collection(ATTESTED_KEYS_COLLECTION).doc(keyID).set({
                        publicKeyPem,
                        counter: 0,
                        hmac,
                        createdAt: new Date(),
                        lastUsedAt: new Date(),
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
