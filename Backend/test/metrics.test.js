const { describe, it, before, after, afterEach, mock } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const { app, Counter, timer, counters } = require('../dist/server');

describe('Counter', () => {
    it('increments and retrieves counts', () => {
        const c = new Counter();
        c.increment('test_metric', { status: 'ok' });
        c.increment('test_metric', { status: 'ok' });
        c.increment('test_metric', { status: 'error' });
        assert.equal(c.get('test_metric', { status: 'ok' }), 2);
        assert.equal(c.get('test_metric', { status: 'error' }), 1);
    });

    it('returns 0 for unknown metric', () => {
        const c = new Counter();
        assert.equal(c.get('nonexistent'), 0);
    });

    it('flush clears counts', () => {
        const c = new Counter();
        c.increment('test_metric', { status: 'ok' });
        assert.equal(c.get('test_metric', { status: 'ok' }), 1);
        c.flush();
        assert.equal(c.get('test_metric', { status: 'ok' }), 0);
    });

    it('handles labels with consistent ordering', () => {
        const c = new Counter();
        c.increment('m', { b: '2', a: '1' });
        // Same labels in different order should match
        assert.equal(c.get('m', { a: '1', b: '2' }), 1);
    });
});

describe('timer', () => {
    it('returns elapsed duration in ms', async () => {
        const end = timer('test_duration');
        // Small delay
        await new Promise(r => setTimeout(r, 10));
        const duration = end({ op: 'test' });
        assert.ok(duration >= 5, `Expected duration >= 5ms, got ${duration}`);
    });
});

describe('Gemini relay metrics', () => {
    const originalKey = process.env.APP_BACKEND_API_KEY;
    const originalGeminiKey = process.env.GEMINI_API_KEY;
    const originalModel = process.env.GEMINI_MODEL_NAME;

    before(() => {
        process.env.APP_BACKEND_API_KEY = 'test-key';
        process.env.GEMINI_API_KEY = 'fake-gemini-key';
        process.env.GEMINI_MODEL_NAME = 'gemini-2.0-flash';
    });

    after(() => {
        if (originalKey !== undefined) process.env.APP_BACKEND_API_KEY = originalKey;
        else delete process.env.APP_BACKEND_API_KEY;
        if (originalGeminiKey !== undefined) process.env.GEMINI_API_KEY = originalGeminiKey;
        else delete process.env.GEMINI_API_KEY;
        if (originalModel !== undefined) process.env.GEMINI_MODEL_NAME = originalModel;
        else delete process.env.GEMINI_MODEL_NAME;
    });

    afterEach(() => {
        mock.restoreAll();
    });

    it('increments gemini_relay_total on successful relay', async () => {
        const before = counters.get('gemini_relay_total', { status: 'success' });
        mock.method(globalThis, 'fetch', async () => ({
            ok: true,
            status: 200,
            json: async () => ({ candidates: [] }),
        }));

        await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-backend-key', 'test-key')
            .send({ contents: [] });

        const after = counters.get('gemini_relay_total', { status: 'success' });
        assert.equal(after, before + 1);
    });
});
