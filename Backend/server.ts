// Load environment variables locally
require('dotenv').config();

import { app } from './src/app';
import { requestLogger, createLogger, setLogger } from './src/logger';
import { globalLimiter, geminiLimiter, attestLimiter, legacyKeyLimiter } from './src/rate-limiters';
import { captureRawBody } from './src/capture-raw-body';
import { verifyRequest } from './src/verify-request';
import { verifyAndroidRequest } from './src/verify-request-android';
import { attestedKeys, loadKeysFromFirestore } from './src/attested-keys';
import { challenges, registerRoutes as registerChallengeRoutes } from './src/challenge';
import { getAppleRootCa, setAppleRootCa } from './src/apple-root-ca';
import { getDb, setDb } from './src/firestore';
import { initHmacSecret, setHmacSecret } from './src/hmac-secret';
import { extractNonceFromCert, parseDerLength } from './src/cert-utils';
import { keyIdToDocId, docIdToKeyId } from './src/firestore-key';
import { counters, Counter, timer } from './src/metrics';
import { registerRoutes as registerAttestationRoutes, setAndroidDecoderForTest } from './src/attestation';
import { registerRoutes as registerMainRoutes } from './src/routes';

// Apply request logger (assigns req.log with requestId + timing)
app.use(requestLogger);

// Apply global rate limiter
app.use(globalLimiter);

// Register routes (order matters)
registerChallengeRoutes(app, attestLimiter);
registerAttestationRoutes(app, attestLimiter);
registerMainRoutes(app, { geminiLimiter, captureRawBody, verifyRequest });

// ---------------------------------------------------------------------------
// Start server (only when run directly, not when imported)
// ---------------------------------------------------------------------------
const PORT = process.env.PORT || 8080;

if (require.main === module) {
    const log = createLogger('server');
    (async () => {
        await initHmacSecret();
        await loadKeysFromFirestore();
        app.listen(PORT, () => {
            const appleRootCaPem = getAppleRootCa();
            log.info({
                port: PORT,
                env: process.env.BACKEND_ENV || 'development (local)',
                model: process.env.GEMINI_MODEL_NAME || 'NOT SET',
                appAttest: appleRootCaPem ? 'enabled' : 'disabled (no root CA)',
                firestore: getDb() ? 'connected' : 'unavailable (in-memory only)',
            }, 'Backend server started');
        });
    })();
}

export { app, attestedKeys, challenges, extractNonceFromCert, parseDerLength, setAppleRootCa, setDb, setHmacSecret, setLogger, captureRawBody, verifyRequest, verifyAndroidRequest, setAndroidDecoderForTest, globalLimiter, geminiLimiter, attestLimiter, legacyKeyLimiter, loadKeysFromFirestore, counters, Counter, timer, keyIdToDocId, docIdToKeyId };
