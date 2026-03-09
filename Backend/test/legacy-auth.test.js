const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const { app } = require('../server');

describe('Legacy x-backend-key auth', () => {
    const originalKey = process.env.APP_BACKEND_API_KEY;

    before(() => {
        process.env.APP_BACKEND_API_KEY = 'test-secret-key';
    });

    after(() => {
        if (originalKey !== undefined) {
            process.env.APP_BACKEND_API_KEY = originalKey;
        } else {
            delete process.env.APP_BACKEND_API_KEY;
        }
    });

    it('allows requests with valid x-backend-key', async () => {
        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-backend-key', 'test-secret-key')
            .send({ contents: [] });
        // Should get past auth (may fail on missing GEMINI_API_KEY, but not 401)
        assert.notEqual(res.status, 401);
    });

    it('rejects requests with invalid x-backend-key', async () => {
        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-backend-key', 'wrong-key')
            .send({ contents: [] });
        assert.equal(res.status, 401);
    });

    it('rejects requests with no auth headers', async () => {
        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .send({ contents: [] });
        assert.equal(res.status, 401);
    });
});

describe('Legacy auth with missing APP_BACKEND_API_KEY', () => {
    const originalKey = process.env.APP_BACKEND_API_KEY;

    before(() => {
        delete process.env.APP_BACKEND_API_KEY;
    });

    after(() => {
        if (originalKey !== undefined) {
            process.env.APP_BACKEND_API_KEY = originalKey;
        }
    });

    it('returns 500 when APP_BACKEND_API_KEY is not configured', async () => {
        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-backend-key', 'any-key')
            .send({ contents: [] });
        assert.equal(res.status, 500);
    });
});
