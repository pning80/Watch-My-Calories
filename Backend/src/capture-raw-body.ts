import { Request, Response, NextFunction } from 'express';
import { RawBodyRequest } from './types';

const MAX_BODY_SIZE = 12 * 1024 * 1024; // 12 MB

export const captureRawBody = (req: Request, res: Response, next: NextFunction): void => {
    const chunks: Buffer[] = [];
    let size = 0;
    let aborted = false;

    req.on('data', (chunk: Buffer) => {
        if (aborted) return;
        size += chunk.length;
        if (size > MAX_BODY_SIZE) {
            aborted = true;
            // Stop reading but don't destroy the socket — let the response flush
            req.removeAllListeners('data');
            req.resume(); // drain remaining data
            res.status(413).json({ error: 'Request message body too large.' });
            return;
        }
        chunks.push(chunk);
    });

    req.on('end', () => {
        if (aborted) return;
        (req as RawBodyRequest).rawBody = Buffer.concat(chunks);
        // Also parse JSON so req.body is available
        if ((req as RawBodyRequest).rawBody.length > 0) {
            try {
                req.body = JSON.parse((req as RawBodyRequest).rawBody.toString());
            } catch {
                // Not JSON — leave req.body undefined
            }
        }
        next();
    });
};
