import { Express, Request, Response, NextFunction } from 'express';
import { createLogger } from './logger';
import { counters, timer } from './metrics';
import { attestedKeys } from './attested-keys';

const log = createLogger('routes');

interface RouteMiddleware {
    geminiLimiter: any;
    captureRawBody: any;
    verifyRequest: any;
}

export function registerRoutes(app: Express, { geminiLimiter, captureRawBody, verifyRequest }: RouteMiddleware): void {
    // Relay generateContent requests to Gemini API
    app.post('/v1beta/models/*', geminiLimiter, captureRawBody, verifyRequest, async (req: Request, res: Response) => {
        const geminiKey = process.env.GEMINI_API_KEY;
        const model = process.env.GEMINI_MODEL_NAME;

        if (!geminiKey) {
            log.error('GEMINI_API_KEY is not configured on the server');
            return res.status(500).json({ error: 'Server Configuration Error' });
        }

        if (!model) {
            log.error('GEMINI_MODEL_NAME is not configured on the server');
            return res.status(500).json({ error: 'Server Configuration Error' });
        }

        const targetURL = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiKey}`;
        const end = timer('gemini_relay_duration_ms');

        try {
            // Inject thinking and media resolution config before forwarding to Gemini
            const thinkingLevel = process.env.GEMINI_THINKING_LEVEL || 'medium';
            const mediaResolution = process.env.GEMINI_MEDIA_RESOLUTION || 'media_resolution_high';

            const modifiedBody = {
                ...req.body,
                generationConfig: {
                    ...(req.body.generationConfig || {}),
                    thinkingConfig: { thinkingLevel },
                },
                contents: (req.body.contents || []).map((content: any) => ({
                    ...content,
                    parts: (content.parts || []).map((part: any) => {
                        if (part.inline_data || part.inlineData) {
                            return { ...part, mediaResolution: { level: mediaResolution } };
                        }
                        return part;
                    }),
                })),
            };

            const response = await fetch(targetURL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(modifiedBody),
            });

            const data = await response.json();
            const status = response.ok ? 'success' : `error_${response.status}`;
            end({ status });
            counters.increment('gemini_relay_total', { status });

            if (!response.ok) {
                log.error({ geminiStatus: response.status }, 'Gemini API returned error');
            }

            res.status(response.status).json(data);
        } catch (err: any) {
            end({ status: 'network_error' });
            counters.increment('gemini_relay_total', { status: 'network_error' });
            log.error({ error: err.message }, 'Relay error');
            res.status(502).json({ error: 'Bad Gateway' });
        }
    });

    // Health Check — returns JSON with status info
    app.get('/', (req: Request, res: Response) => {
        res.json({
            status: 'ok',
            uptime: Math.floor(process.uptime()),
            attestedKeysCount: attestedKeys.size,
            env: process.env.BACKEND_ENV || 'development',
        });
    });
}
