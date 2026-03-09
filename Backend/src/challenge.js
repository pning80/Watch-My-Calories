const crypto = require('crypto');
const { CHALLENGE_TTL_MS } = require('./constants');

const challenges = new Map(); // challenge -> { createdAt }

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

function registerRoutes(app, attestLimiter) {
    // GET /attest/challenge — returns a one-time challenge
    app.get('/attest/challenge', attestLimiter, (req, res) => {
        const challenge = crypto.randomUUID();
        challenges.set(challenge, { createdAt: Date.now() });
        res.json({ challenge });
    });
}

module.exports = { challenges, registerRoutes };
