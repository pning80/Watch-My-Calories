const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const { app } = require('../dist/server');

describe('Health check endpoint', () => {
    it('GET / returns 200 with JSON status', async () => {
        const res = await request(app).get('/').expect(200);
        assert.equal(res.body.status, 'ok');
        assert.equal(typeof res.body.uptime, 'number');
        assert.equal(typeof res.body.attestedKeysCount, 'number');
        assert.ok(res.body.env);
    });

    it('GET / includes CORS header', async () => {
        const res = await request(app).get('/');
        assert.equal(res.headers['access-control-allow-origin'], '*');
    });

    it('GET /nonexistent returns 404', async () => {
        await request(app).get('/nonexistent').expect(404);
    });
});

describe('CORS middleware', () => {
    it('OPTIONS preflight returns CORS headers', async () => {
        const res = await request(app)
            .options('/v1beta/models/test:generateContent')
            .set('Origin', 'https://example.com')
            .set('Access-Control-Request-Method', 'POST');
        assert.equal(res.status, 204);
        assert.equal(res.headers['access-control-allow-origin'], '*');
    });

    it('POST response includes CORS headers', async () => {
        const res = await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('Origin', 'https://example.com')
            .send({ contents: [] });
        // May get 401 (no auth), but CORS headers should still be present
        assert.equal(res.headers['access-control-allow-origin'], '*');
    });
});
