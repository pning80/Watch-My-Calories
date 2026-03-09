// Load environment variables locally
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const crypto = require('crypto');
const { decode } = require('cbor-x');
const { X509Certificate } = require('crypto');
const fs = require('fs');
const path = require('path');
const rateLimit = require('express-rate-limit');

const app = express();
app.set('trust proxy', 1);
const PORT = process.env.PORT || 8080;

app.use(cors());

// ---------------------------------------------------------------------------
// Rate Limiting
// ---------------------------------------------------------------------------
const globalLimiter = rateLimit({
    windowMs: parseInt(process.env.RATE_LIMIT_GLOBAL_WINDOW_MS) || 900000,
    max: parseInt(process.env.RATE_LIMIT_GLOBAL_MAX) || 100,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests, please try again later.' },
});

const geminiLimiter = rateLimit({
    windowMs: parseInt(process.env.RATE_LIMIT_GEMINI_WINDOW_MS) || 900000,
    max: parseInt(process.env.RATE_LIMIT_GEMINI_MAX) || 100,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many analysis requests, please try again later.' },
});

const attestLimiter = rateLimit({
    windowMs: parseInt(process.env.RATE_LIMIT_ATTEST_WINDOW_MS) || 900000,
    max: parseInt(process.env.RATE_LIMIT_ATTEST_MAX) || 30,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many attestation requests, please try again later.' },
});

app.use(globalLimiter);

// ---------------------------------------------------------------------------
// Raw body capture — must come BEFORE express.json() on attested routes.
// The iOS client hashes request.httpBody (raw bytes). The server must hash the
// exact same bytes for assertion verification to succeed.
// ---------------------------------------------------------------------------
const captureRawBody = (req, res, next) => {
    const chunks = [];
    req.on('data', chunk => chunks.push(chunk));
    req.on('end', () => {
        req.rawBody = Buffer.concat(chunks);
        // Also parse JSON so req.body is available
        if (req.rawBody.length > 0) {
            try {
                req.body = JSON.parse(req.rawBody);
            } catch {
                // Not JSON — leave req.body undefined
            }
        }
        next();
    });
};

// ---------------------------------------------------------------------------
// Apple App Attestation Root CA
// ---------------------------------------------------------------------------
let appleRootCaPem;
try {
    appleRootCaPem = fs.readFileSync(
        path.join(__dirname, 'apple_attest_root_ca.pem'),
        'utf8'
    );
} catch {
    console.warn('Warning: apple_attest_root_ca.pem not found — App Attest verification will be unavailable.');
}

// ---------------------------------------------------------------------------
// Firestore (optional — graceful degradation if unavailable)
// ---------------------------------------------------------------------------
let db = null;
try {
    const { Firestore } = require('@google-cloud/firestore');
    db = new Firestore();
    console.log('Firestore initialized successfully.');
} catch (err) {
    console.warn('Firestore unavailable — running with in-memory key storage only:', err.message);
}

const ATTESTED_KEYS_COLLECTION = 'attestedKeys';

// ---------------------------------------------------------------------------
// In-memory key store + challenge store
// ---------------------------------------------------------------------------
const attestedKeys = new Map();   // keyID -> { publicKey (KeyObject), counter, hmac }
const challenges = new Map();     // challenge -> { createdAt }
const CHALLENGE_TTL_MS = 60_000;  // 60 seconds

// Periodic challenge cleanup (unref so it doesn't prevent process exit in tests)
const challengeCleanupTimer = setInterval(() => {
    const now = Date.now();
    for (const [challenge, { createdAt }] of challenges) {
        if (now - createdAt > CHALLENGE_TTL_MS) {
            challenges.delete(challenge);
        }
    }
}, 30_000);
challengeCleanupTimer.unref();

// ---------------------------------------------------------------------------
// Config
// APPLE_TEAM_ID and ATTEST_HMAC_SECRET are read from process.env at request
// time (not cached at module load) so they can be configured dynamically.
// ---------------------------------------------------------------------------
const BUNDLE_ID = 'com.pning80.WatchMyCalories';

// ---------------------------------------------------------------------------
// App Attest Endpoints
// ---------------------------------------------------------------------------

// GET /attest/challenge — returns a one-time challenge
app.get('/attest/challenge', attestLimiter, (req, res) => {
    const challenge = crypto.randomUUID();
    challenges.set(challenge, { createdAt: Date.now() });
    res.json({ challenge });
});

// POST /attest/verify — verifies attestation and stores the public key
app.post('/attest/verify', attestLimiter, express.json({ limit: '1mb' }), async (req, res) => {
    try {
        const { keyID, attestation, challenge } = req.body;

        if (!keyID || !attestation || !challenge) {
            return res.status(400).json({ error: 'Missing keyID, attestation, or challenge.' });
        }

        if (!appleRootCaPem) {
            return res.status(500).json({ error: 'App Attest root CA not configured.' });
        }

        if (!process.env.APPLE_TEAM_ID || !process.env.ATTEST_HMAC_SECRET) {
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
        const certs = attStmt.x5c.map(certDer => {
            const pem = `-----BEGIN CERTIFICATE-----\n${Buffer.from(certDer).toString('base64').match(/.{1,64}/g).join('\n')}\n-----END CERTIFICATE-----`;
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
        const credIdLen = authDataBuf.readUInt16BE(53); // offset 32+1+4+16 = 53
        const coseKeyOffset = 55 + credIdLen; // 53 + 2 + credIdLen
        const coseKeyData = authDataBuf.subarray(coseKeyOffset);
        const coseKey = decode(coseKeyData);

        // COSE EC2 key: -1 = curve (1 = P-256), -2 = x, -3 = y
        const x = coseKey.get(-2);
        const y = coseKey.get(-3);

        if (!x || !y) {
            return res.status(400).json({ error: 'Invalid COSE key in attestation.' });
        }

        // Convert to uncompressed point format (0x04 || x || y) and create KeyObject
        const publicKeyUncompressed = Buffer.concat([Buffer.from([0x04]), Buffer.from(x), Buffer.from(y)]);
        const publicKey = crypto.createPublicKey({
            key: {
                kty: 'EC',
                crv: 'P-256',
                x: Buffer.from(x).toString('base64url'),
                y: Buffer.from(y).toString('base64url'),
            },
            format: 'jwk',
        });

        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' });

        // Compute HMAC for tamper detection
        const hmac = crypto.createHmac('sha256', process.env.ATTEST_HMAC_SECRET)
            .update(publicKeyPem + keyID)
            .digest('hex');

        // Store in memory
        attestedKeys.set(keyID, { publicKey, counter: 0, hmac });

        // Store in Firestore (async, non-blocking)
        if (db) {
            db.collection(ATTESTED_KEYS_COLLECTION).doc(keyID).set({
                publicKeyPem,
                counter: 0,
                hmac,
                createdAt: new Date(),
                lastUsedAt: new Date(),
            }).catch(err => console.error('Firestore write error (attestation):', err.message));
        }

        console.log(`App Attest: Key ${keyID.substring(0, 8)}... attested successfully.`);
        res.json({ success: true });

    } catch (err) {
        console.error('Attestation verification error:', err);
        res.status(400).json({ error: 'Attestation verification failed.' });
    }
});

// ---------------------------------------------------------------------------
// Extract nonce from Apple App Attest leaf certificate
// OID: 1.2.840.113635.100.8.2
// ---------------------------------------------------------------------------
function extractNonceFromCert(cert) {
    // The nonce is in a custom extension with OID 1.2.840.113635.100.8.2
    // Node's X509Certificate doesn't expose arbitrary extensions directly,
    // so we parse the raw DER to find it.
    const certDer = cert.raw;
    const oidHex = '06092a864886f763640802'; // DER encoding of 1.2.840.113635.100.8.2

    const oidBytes = Buffer.from(oidHex, 'hex');
    const idx = certDer.indexOf(oidBytes);
    if (idx === -1) return null;

    // After OID: skip OID bytes, then OCTET STRING wrapper(s)
    // The structure is: OID | critical? | OCTET STRING (outer) | SEQUENCE | OCTET STRING (inner, 32 bytes)
    let pos = idx + oidBytes.length;

    // Skip optional BOOLEAN (critical flag)
    if (certDer[pos] === 0x01) {
        pos += 3; // BOOLEAN tag(1) + length(1) + value(1)
    }

    // Outer OCTET STRING
    if (certDer[pos] !== 0x04) return null;
    pos++;
    const outerLen = parseDerLength(certDer, pos);
    pos += outerLen.bytesRead;

    // SEQUENCE
    if (certDer[pos] !== 0x30) return null;
    pos++;
    const seqLen = parseDerLength(certDer, pos);
    pos += seqLen.bytesRead;

    // Context-specific [1] wrapping the OCTET STRING
    // Tag 0xA1 = context-specific, constructed, tag number 1
    if (certDer[pos] === 0xa1) {
        pos++;
        const ctxLen = parseDerLength(certDer, pos);
        pos += ctxLen.bytesRead;
    }

    // Inner OCTET STRING (the actual nonce, 32 bytes)
    if (certDer[pos] !== 0x04) return null;
    pos++;
    const innerLen = parseDerLength(certDer, pos);
    pos += innerLen.bytesRead;

    return certDer.subarray(pos, pos + innerLen.value);
}

function parseDerLength(buf, offset) {
    const first = buf[offset];
    if (first < 0x80) {
        return { value: first, bytesRead: 1 };
    }
    const numBytes = first & 0x7f;
    let value = 0;
    for (let i = 0; i < numBytes; i++) {
        value = (value << 8) | buf[offset + 1 + i];
    }
    return { value, bytesRead: 1 + numBytes };
}

// ---------------------------------------------------------------------------
// Auth middleware — supports App Attest assertions and legacy x-backend-key
// ---------------------------------------------------------------------------
const verifyRequest = async (req, res, next) => {
    const assertionHeader = req.headers['x-app-attest-assertion'];
    const keyID = req.headers['x-app-attest-key-id'];

    // Path 1: App Attest assertion
    if (assertionHeader && keyID) {
        try {
            await verifyAppAttestAssertion(req, assertionHeader, keyID);
            return next();
        } catch (err) {
            console.warn(`App Attest assertion failed for key ${keyID.substring(0, 8)}...: ${err.message}`);
            return res.status(401).json({ error: 'App Attest assertion verification failed.' });
        }
    }

    // Path 2: Legacy x-backend-key
    const token = req.headers['x-backend-key'];
    const validToken = process.env.APP_BACKEND_API_KEY;

    if (!validToken) {
        console.error('CRITICAL: APP_BACKEND_API_KEY is not configured on the server.');
        return res.status(500).json({ error: 'Server Configuration Error' });
    }

    if (token !== validToken) {
        console.warn(`Unauthorized request attempt with token: ${token}`);
        return res.status(401).json({ error: 'Unauthorized access' });
    }

    next();
};

// ---------------------------------------------------------------------------
// Verify App Attest assertion
// ---------------------------------------------------------------------------
async function verifyAppAttestAssertion(req, assertionBase64, keyID) {
    // Look up stored key — in-memory first, then Firestore
    let keyData = attestedKeys.get(keyID);

    if (!keyData && db) {
        // Fetch from Firestore
        const doc = await db.collection(ATTESTED_KEYS_COLLECTION).doc(keyID).get();
        if (doc.exists) {
            const data = doc.data();

            // Verify HMAC (tamper detection)
            if (process.env.ATTEST_HMAC_SECRET) {
                const expectedHmac = crypto.createHmac('sha256', process.env.ATTEST_HMAC_SECRET)
                    .update(data.publicKeyPem + keyID)
                    .digest('hex');
                if (!crypto.timingSafeEqual(Buffer.from(expectedHmac), Buffer.from(data.hmac))) {
                    throw new Error('HMAC verification failed — possible tampering.');
                }
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

// ---------------------------------------------------------------------------
// Relay generateContent requests to Gemini API
// Wildcard avoids Express param parsing issues with colons in the path.
// Uses captureRawBody to preserve the exact bytes for assertion verification.
// ---------------------------------------------------------------------------
app.post('/v1beta/models/*', geminiLimiter, captureRawBody, verifyRequest, async (req, res) => {
    const geminiKey = process.env.GEMINI_API_KEY;
    const model = process.env.GEMINI_MODEL_NAME;

    if (!geminiKey) {
        console.error('CRITICAL: GEMINI_API_KEY is not configured on the server.');
        return res.status(500).json({ error: 'Server Configuration Error' });
    }

    if (!model) {
        console.error('CRITICAL: GEMINI_MODEL_NAME is not configured on the server.');
        return res.status(500).json({ error: 'Server Configuration Error' });
    }

    const targetURL = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiKey}`;

    try {
        const response = await fetch(targetURL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req.body),
        });

        const data = await response.json();

        if (!response.ok) {
            console.error(`Gemini API returned status ${response.status}:`, JSON.stringify(data));
        }

        res.status(response.status).json(data);
    } catch (err) {
        console.error('Relay Error:', err);
        res.status(502).json({ error: 'Bad Gateway' });
    }
});

// Basic Health Check
app.get('/', (req, res) => {
    res.send('WatchMyCalories Backend is running.');
});

// ---------------------------------------------------------------------------
// Setter functions for test injection
// ---------------------------------------------------------------------------
function setAppleRootCa(pem) {
    appleRootCaPem = pem;
}

function setDb(mockDb) {
    db = mockDb;
}

// ---------------------------------------------------------------------------
// Start server (only when run directly, not when imported)
// ---------------------------------------------------------------------------
if (require.main === module) {
    app.listen(PORT, () => {
        console.log(`Backend server listening on port ${PORT}`);
        console.log(`Enforcing Model: ${process.env.GEMINI_MODEL_NAME || 'NOT SET'}`);
        console.log(`App Attest: ${appleRootCaPem ? 'enabled' : 'disabled (no root CA)'}`);
        console.log(`Firestore: ${db ? 'connected' : 'unavailable (in-memory only)'}`);
    });
}

module.exports = { app, attestedKeys, challenges, extractNonceFromCert, parseDerLength, setAppleRootCa, setDb, captureRawBody, verifyRequest, globalLimiter, geminiLimiter, attestLimiter };
