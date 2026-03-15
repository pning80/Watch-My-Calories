import { describe, it, before, beforeEach, after } from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';
import { app, challenges, setAppleRootCa, setHmacSecret, setDb, globalLimiter, attestLimiter } from '../dist/server';
import { getTestRootCaPem } from './helpers/crypto-fixtures';

const TEST_TEAM_ID = 'TESTTEAMID';

describe('Challenge one-time use', () => {
    let rootCaPem: string;
    const origTeamId = process.env.APPLE_TEAM_ID;

    before(async () => {
        rootCaPem = await getTestRootCaPem();
        setAppleRootCa(rootCaPem);
        process.env.APPLE_TEAM_ID = TEST_TEAM_ID;
        setHmacSecret('test-hmac-secret');
    });

    beforeEach(() => {
        challenges.clear();
        setDb(null);
        for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
            globalLimiter.resetKey(ip);
            attestLimiter.resetKey(ip);
        }
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId;
        else delete process.env.APPLE_TEAM_ID;
        setHmacSecret(null);
    });

    it('challenge is deleted after successful consumption by /attest/verify', async () => {
        // Get a challenge
        const res = await request(app).get('/attest/challenge').expect(200);
        const challenge = res.body.challenge;
        assert.ok(challenges.has(challenge), 'Challenge should exist after creation');

        // POST to /attest/verify — will fail attestation validation but challenge should be consumed
        await request(app)
            .post('/attest/verify')
            .send({ keyID: 'test', attestation: 'dGVzdA==', challenge })
            .expect(400);

        // Challenge should be consumed (deleted) after use
        assert.ok(!challenges.has(challenge), 'Challenge should be deleted after use');
    });

    it('rejects an already-consumed challenge', async () => {
        const res = await request(app).get('/attest/challenge').expect(200);
        const challenge = res.body.challenge;

        // First use — consumes the challenge
        await request(app)
            .post('/attest/verify')
            .send({ keyID: 'test', attestation: 'dGVzdA==', challenge })
            .expect(400);

        // Second use — challenge no longer exists
        const res2 = await request(app)
            .post('/attest/verify')
            .send({ keyID: 'test', attestation: 'dGVzdA==', challenge })
            .expect(400);

        assert.equal(res2.body.error, 'Invalid or expired challenge.');
    });
});

describe('Challenge expiry', () => {
    const origTeamId = process.env.APPLE_TEAM_ID;

    before(async () => {
        const rootCaPem = await getTestRootCaPem();
        setAppleRootCa(rootCaPem);
        process.env.APPLE_TEAM_ID = TEST_TEAM_ID;
        setHmacSecret('test-hmac-secret');
    });

    beforeEach(() => {
        challenges.clear();
        setDb(null);
        for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
            globalLimiter.resetKey(ip);
            attestLimiter.resetKey(ip);
        }
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId;
        else delete process.env.APPLE_TEAM_ID;
        setHmacSecret(null);
    });

    it('rejects a challenge that exceeds TTL', async () => {
        // Manually insert a challenge with an old timestamp (61 seconds ago)
        const oldChallenge = 'expired-challenge-id';
        challenges.set(oldChallenge, { createdAt: Date.now() - 61_000 });

        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID: 'test', attestation: 'dGVzdA==', challenge: oldChallenge })
            .expect(400);

        assert.equal(res.body.error, 'Challenge expired.');
        assert.ok(!challenges.has(oldChallenge), 'Expired challenge should be removed');
    });

    it('accepts a challenge within TTL', async () => {
        // Manually insert a challenge with a recent timestamp
        const freshChallenge = 'fresh-challenge-id';
        challenges.set(freshChallenge, { createdAt: Date.now() - 5_000 });

        // Will fail on attestation CBOR decoding, but should NOT fail on challenge expiry
        const res = await request(app)
            .post('/attest/verify')
            .send({ keyID: 'test', attestation: 'dGVzdA==', challenge: freshChallenge })
            .expect(400);

        // Error should be about attestation format, not about challenge expiry
        assert.notEqual(res.body.error, 'Challenge expired.');
        assert.notEqual(res.body.error, 'Invalid or expired challenge.');
    });
});
