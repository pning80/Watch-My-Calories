import { Request, Response, NextFunction } from 'express';
import { verifyAppAttestAssertion } from './assertion';
import { legacyKeyLimiter } from './rate-limiters';
import { createLogger } from './logger';
import { counters } from './metrics';

const log = createLogger('verify-request');

export const verifyRequest = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    const assertionHeader = req.headers['x-app-attest-assertion'] as string | undefined;
    const keyID = req.headers['x-app-attest-key-id'] as string | undefined;

    // Path 1: App Attest assertion (production)
    if (assertionHeader && keyID) {
        try {
            await verifyAppAttestAssertion(req, assertionHeader, keyID);
            return next();
        } catch (err: any) {
            log.warn({ keyId: keyID.substring(0, 8), error: err.message }, 'App Attest assertion failed');
            res.status(401).json({ error: 'App Attest assertion verification failed.' });
            return;
        }
    }

    // Path 2: Legacy x-backend-key (dev/testing only — disabled in production)
    if (process.env.BACKEND_ENV === 'prod') {
        res.status(401).json({ error: 'Unauthorized: App Attest required.' });
        return;
    }

    legacyKeyLimiter(req, res, (err?: any) => {
        if (err) return next(err);

        const token = req.headers['x-backend-key'] as string | undefined;
        const validToken = process.env.APP_BACKEND_API_KEY;

        if (!validToken) {
            log.error('APP_BACKEND_API_KEY is not configured on the server');
            return res.status(500).json({ error: 'Server Configuration Error' });
        }

        if (token !== validToken) {
            // SECURITY: Never log the token value — only log the requesting IP
            log.warn({ ip: req.ip }, 'Unauthorized request attempt with invalid legacy key');
            return res.status(401).json({ error: 'Unauthorized access' });
        }

        next();
    });
};
