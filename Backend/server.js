// Load environment variables locally
require('dotenv').config();

const { app } = require('./src/app');
const { globalLimiter, geminiLimiter, attestLimiter, legacyKeyLimiter } = require('./src/rate-limiters');
const { captureRawBody } = require('./src/capture-raw-body');
const { verifyRequest } = require('./src/verify-request');
const { attestedKeys, loadKeysFromFirestore } = require('./src/attested-keys');
const { challenges } = require('./src/challenge');
const { getAppleRootCa, setAppleRootCa } = require('./src/apple-root-ca');
const { setDb } = require('./src/firestore');
const { extractNonceFromCert, parseDerLength } = require('./src/cert-utils');

// Apply global rate limiter
app.use(globalLimiter);

// Register routes (order matters)
require('./src/challenge').registerRoutes(app, attestLimiter);
require('./src/attestation').registerRoutes(app, attestLimiter);
require('./src/routes').registerRoutes(app, { geminiLimiter, captureRawBody, verifyRequest });

// ---------------------------------------------------------------------------
// Start server (only when run directly, not when imported)
// ---------------------------------------------------------------------------
const PORT = process.env.PORT || 8080;

if (require.main === module) {
    (async () => {
        await loadKeysFromFirestore();
        app.listen(PORT, () => {
            const appleRootCaPem = getAppleRootCa();
            console.log(`Backend server listening on port ${PORT}`);
            console.log(`Enforcing Model: ${process.env.GEMINI_MODEL_NAME || 'NOT SET'}`);
            console.log(`App Attest: ${appleRootCaPem ? 'enabled' : 'disabled (no root CA)'}`);
            console.log(`Firestore: ${require('./src/firestore').getDb() ? 'connected' : 'unavailable (in-memory only)'}`);
        });
    })();
}

module.exports = { app, attestedKeys, challenges, extractNonceFromCert, parseDerLength, setAppleRootCa, setDb, captureRawBody, verifyRequest, globalLimiter, geminiLimiter, attestLimiter, legacyKeyLimiter, loadKeysFromFirestore };
