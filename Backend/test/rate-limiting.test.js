const { describe, it, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const { app, globalLimiter, geminiLimiter, attestLimiter, legacyKeyLimiter } = require('../dist/server');

// supertest connects via loopback — with trust proxy the IP may vary
const POSSIBLE_IPS = ['::ffff:127.0.0.1', '127.0.0.1', '::1'];

function resetAllLimiters() {
    for (const ip of POSSIBLE_IPS) {
        globalLimiter.resetKey(ip);
        geminiLimiter.resetKey(ip);
        attestLimiter.resetKey(ip);
    }
    legacyKeyLimiter.resetKey('legacy-key-global');
}

describe('Rate Limiting', () => {
    beforeEach(() => {
        resetAllLimiters();
    });

    it('GET / includes standard rate limit headers', async () => {
        const res = await request(app).get('/');
        assert.ok(res.headers['ratelimit-limit'], 'ratelimit-limit header missing');
        assert.ok(res.headers['ratelimit-remaining'] !== undefined, 'ratelimit-remaining header missing');
        assert.ok(res.headers['ratelimit-reset'], 'ratelimit-reset header missing');
    });

    it('rate limit headers show correct remaining count', async () => {
        const res1 = await request(app).get('/');
        const remaining1 = parseInt(res1.headers['ratelimit-remaining']);

        const res2 = await request(app).get('/');
        const remaining2 = parseInt(res2.headers['ratelimit-remaining']);

        assert.equal(remaining2, remaining1 - 1, 'remaining should decrease by 1');
    });

    it('GET /attest/challenge includes rate limit headers', async () => {
        const res = await request(app).get('/attest/challenge');
        assert.ok(res.headers['ratelimit-limit'], 'ratelimit-limit header missing');
        assert.ok(res.headers['ratelimit-remaining'] !== undefined, 'ratelimit-remaining header missing');
    });

    it('POST /v1beta/models/* includes rate limit headers', async () => {
        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .send({});
        // Request will fail auth (401) but rate limit headers should still be present
        assert.ok(res.headers['ratelimit-limit'], 'ratelimit-limit header missing');
        assert.ok(res.headers['ratelimit-remaining'] !== undefined, 'ratelimit-remaining header missing');
    });

    it('returns 429 when attest limit exceeded', async () => {
        const max = attestLimiter.getMaxFromOptions ? attestLimiter.max : 30;
        // Send max requests (should all succeed)
        for (let i = 0; i < max; i++) {
            await request(app).get('/attest/challenge');
        }
        // Next request should be rate limited
        const res = await request(app).get('/attest/challenge');
        assert.equal(res.status, 429);
    });

    it('429 response includes Retry-After header', async () => {
        const max = 30;
        for (let i = 0; i < max; i++) {
            await request(app).get('/attest/challenge');
        }
        const res = await request(app).get('/attest/challenge');
        assert.equal(res.status, 429);
        assert.ok(res.headers['retry-after'], 'retry-after header missing');
    });

    it('429 response body contains error message', async () => {
        const max = 30;
        for (let i = 0; i < max; i++) {
            await request(app).get('/attest/challenge');
        }
        const res = await request(app).get('/attest/challenge');
        assert.equal(res.status, 429);
        assert.deepEqual(res.body, { error: 'Too many attestation requests, please try again later.' });
    });

    it('different endpoints have independent counters', async () => {
        // Exhaust the attest limiter
        const max = 30;
        for (let i = 0; i < max; i++) {
            await request(app).get('/attest/challenge');
        }
        // Attest should be rate limited
        const attestRes = await request(app).get('/attest/challenge');
        assert.equal(attestRes.status, 429);

        // Health check should still work (different limiter)
        const healthRes = await request(app).get('/');
        assert.equal(healthRes.status, 200);
    });
});
