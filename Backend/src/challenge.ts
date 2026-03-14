import crypto from 'crypto';
import { Express } from 'express';
import { CHALLENGE_TTL_MS } from './constants';

interface ChallengeEntry {
    createdAt: number;
}

export const challenges = new Map<string, ChallengeEntry>();

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

export function registerRoutes(app: Express, attestLimiter: any): void {
    // GET /attest/challenge — returns a one-time challenge
    app.get('/attest/challenge', attestLimiter, (req, res) => {
        const challenge = crypto.randomUUID();
        challenges.set(challenge, { createdAt: Date.now() });
        res.json({ challenge });
    });
}
