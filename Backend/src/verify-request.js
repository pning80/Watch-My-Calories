const { verifyAppAttestAssertion } = require('./assertion');
const { legacyKeyLimiter } = require('./rate-limiters');

const verifyRequest = async (req, res, next) => {
    const assertionHeader = req.headers['x-app-attest-assertion'];
    const keyID = req.headers['x-app-attest-key-id'];

    // Path 1: App Attest assertion (production)
    if (assertionHeader && keyID) {
        try {
            await verifyAppAttestAssertion(req, assertionHeader, keyID);
            return next();
        } catch (err) {
            console.warn(`App Attest assertion failed for key ${keyID.substring(0, 8)}...: ${err.message}`);
            return res.status(401).json({ error: 'App Attest assertion verification failed.' });
        }
    }

    // Path 2: Legacy x-backend-key (dev/testing only — disabled in production)
    if (process.env.BACKEND_ENV === 'prod') {
        return res.status(401).json({ error: 'Unauthorized: App Attest required.' });
    }

    legacyKeyLimiter(req, res, (err) => {
        if (err) return next(err);

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
    });
};

module.exports = { verifyRequest };
