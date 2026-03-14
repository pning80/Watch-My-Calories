import { describe, it, before, beforeEach, after } from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';
import { app, globalLimiter, geminiLimiter, legacyKeyLimiter } from '../dist/server';

const POSSIBLE_IPS = ['::ffff:127.0.0.1', '127.0.0.1', '::1'];

describe('Legacy key rate limiter', () => {
    const origApiKey = process.env.APP_BACKEND_API_KEY;

    before(() => {
        process.env.APP_BACKEND_API_KEY = 'test-key';
    });

    beforeEach(() => {
        legacyKeyLimiter.resetKey('legacy-key-global');
        for (const ip of POSSIBLE_IPS) {
            globalLimiter.resetKey(ip);
            geminiLimiter.resetKey(ip);
        }
    });

    after(() => {
        if (origApiKey !== undefined) process.env.APP_BACKEND_API_KEY = origApiKey;
        else delete process.env.APP_BACKEND_API_KEY;
    });

    function legacyRequest(key = 'test-key') {
        return request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-backend-key', key)
            .set('Content-Type', 'application/json')
            .send(JSON.stringify({ contents: [] }));
    }

    it('allows legacy requests under the rate limit', async () => {
        const res = await legacyRequest();
        assert.notEqual(res.status, 429);
    });

    it('returns 429 when legacy key limit (15) is exceeded', async () => {
        for (let i = 0; i < 15; i++) {
            await legacyRequest();
        }
        const res = await legacyRequest();
        assert.equal(res.status, 429);
        assert.deepEqual(res.body, { error: 'Too many legacy API key requests, please use App Attest.' });
    });

    it('429 response includes Retry-After header', async () => {
        for (let i = 0; i < 15; i++) {
            await legacyRequest();
        }
        const res = await legacyRequest();
        assert.equal(res.status, 429);
        assert.ok(res.headers['retry-after'], 'retry-after header missing');
    });

    it('App Attest requests bypass the legacy key limiter', async () => {
        // Exhaust the legacy limiter
        for (let i = 0; i < 15; i++) {
            await legacyRequest();
        }
        // Verify legacy is rate-limited
        const legacyRes = await legacyRequest();
        assert.equal(legacyRes.status, 429);

        // App Attest request should bypass legacy limiter entirely (gets 401 for invalid assertion, not 429)
        const attestRes = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-app-attest-assertion', 'fake-assertion')
            .set('x-app-attest-key-id', 'fake-key-id')
            .set('Content-Type', 'application/json')
            .send(JSON.stringify({ contents: [] }));
        assert.notEqual(attestRes.status, 429);
        assert.equal(attestRes.status, 401);
    });

    it('resetting legacy-key-global allows requests again', async () => {
        for (let i = 0; i < 15; i++) {
            await legacyRequest();
        }
        const blockedRes = await legacyRequest();
        assert.equal(blockedRes.status, 429);

        legacyKeyLimiter.resetKey('legacy-key-global');

        const res = await legacyRequest();
        assert.notEqual(res.status, 429);
    });

    it('invalid legacy key requests still consume rate limit budget', async () => {
        // Send 15 requests with wrong key (all get 401)
        for (let i = 0; i < 15; i++) {
            const res = await legacyRequest('wrong-key');
            assert.equal(res.status, 401);
        }
        // 16th request should be rate-limited even with correct key
        const res = await legacyRequest('test-key');
        assert.equal(res.status, 429);
    });
});
