const crypto = require('crypto');
const { decode } = require('cbor-x');
const { attestedKeys } = require('./attested-keys');
const { getDb } = require('./firestore');
const { BUNDLE_ID, ATTESTED_KEYS_COLLECTION } = require('./constants');
const { getHmacSecret } = require('./hmac-secret');

async function verifyAppAttestAssertion(req, assertionBase64, keyID) {
    const db = getDb();

    // Look up stored key — in-memory first, then Firestore
    let keyData = attestedKeys.get(keyID);

    if (!keyData && db) {
        // Fetch from Firestore
        const doc = await db.collection(ATTESTED_KEYS_COLLECTION).doc(keyID).get();
        if (doc.exists) {
            const data = doc.data();

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

    if (!keyData) {
        throw new Error('Key not attested.');
    }

    // Decode CBOR assertion
    const assertionBuffer = Buffer.from(assertionBase64, 'base64');
    const assertion = decode(assertionBuffer);

    const authenticatorData = assertion.authenticatorData || assertion.get?.('authenticatorData');
    const signature = assertion.signature || assertion.get?.('signature');

    if (!authenticatorData || !signature) {
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
            throw new Error('RP ID hash mismatch.');
        }
    }

    // Verify counter is incrementing (replay protection)
    const counter = authDataBuf.readUInt32BE(33); // offset: rpIdHash(32) + flags(1)
    if (counter <= keyData.counter) {
        throw new Error(`Counter not incrementing: got ${counter}, expected > ${keyData.counter}.`);
    }

    // Compute client data hash from raw request body
    const clientDataHash = crypto.createHash('sha256').update(req.rawBody).digest();

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
        throw new Error('Signature verification failed.');
    }

    // Update counter
    keyData.counter = counter;

    // Async Firestore counter update (fire-and-forget)
    if (db) {
        db.collection(ATTESTED_KEYS_COLLECTION).doc(keyID).update({
            counter,
            lastUsedAt: new Date(),
        }).catch(err => console.error('Firestore counter update error:', err.message));
    }
}

module.exports = { verifyAppAttestAssertion };
