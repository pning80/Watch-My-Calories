import { Request, Response, NextFunction } from 'express';
import { RawBodyRequest } from './types';

export const captureRawBody = (req: Request, res: Response, next: NextFunction): void => {
    const chunks: Buffer[] = [];
    req.on('data', (chunk: Buffer) => chunks.push(chunk));
    req.on('end', () => {
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
