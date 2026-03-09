const rateLimit = require('express-rate-limit');

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

// Strict limiter for legacy x-backend-key auth (dev/testing only)
const legacyKeyLimiter = rateLimit({
    windowMs: parseInt(process.env.RATE_LIMIT_LEGACY_KEY_WINDOW_MS) || 900000,
    max: parseInt(process.env.RATE_LIMIT_LEGACY_KEY_MAX) || 15,
    standardHeaders: true,
    legacyHeaders: false,
    keyGenerator: () => 'legacy-key-global', // single bucket across all devices
    message: { error: 'Too many legacy API key requests, please use App Attest.' },
});

module.exports = { globalLimiter, geminiLimiter, attestLimiter, legacyKeyLimiter };
