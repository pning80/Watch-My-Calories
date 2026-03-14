import pino, { Logger } from 'pino';
import crypto from 'crypto';
import path from 'path';
import { Request, Response, NextFunction } from 'express';

const { version } = require(path.join(__dirname, '..', '..', 'package.json'));

const CLOUD_SEVERITY: Record<number, string> = {
    10: 'DEBUG',    // trace
    20: 'DEBUG',    // debug
    30: 'INFO',     // info
    40: 'WARNING',  // warn
    50: 'ERROR',    // error
    60: 'CRITICAL', // fatal
};

let logger: Logger = pino({
    level: process.env.LOG_LEVEL || 'info',
    formatters: {
        level(label: string, number: number) {
            return { severity: CLOUD_SEVERITY[number] || 'DEFAULT', level: label };
        },
    },
    base: { env: process.env.BACKEND_ENV || 'development', version },
    timestamp: pino.stdTimeFunctions.isoTime,
});

export function createLogger(component: string): Logger {
    return logger.child({ component });
}

export function requestLogger(req: Request, res: Response, next: NextFunction): void {
    const requestId = crypto.randomUUID();
    const start = process.hrtime.bigint();
    (req as any).log = logger.child({ requestId });
    (req as any).requestId = requestId;

    res.on('finish', () => {
        const durationMs = Number(process.hrtime.bigint() - start) / 1e6;
        (req as any).log.info({
            msg: 'request completed',
            method: req.method,
            url: req.originalUrl,
            statusCode: res.statusCode,
            responseTime: Math.round(durationMs),
        });
    });

    next();
}

export function setLogger(mock: Logger): void {
    logger = mock;
}

export function getLogger(): Logger {
    return logger;
}
