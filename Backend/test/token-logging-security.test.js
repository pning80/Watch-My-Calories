const { describe, it, before, after, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const { app, setLogger } = require('../dist/server');
const pino = require('pino');

describe('Token logging security', () => {
    const originalKey = process.env.APP_BACKEND_API_KEY;
    const originalEnv = process.env.BACKEND_ENV;
    let logOutput;
    let originalStream;

    before(() => {
        process.env.APP_BACKEND_API_KEY = 'super-secret-api-key-12345';
        delete process.env.BACKEND_ENV; // ensure non-prod so legacy key path is exercised
    });

    after(() => {
        if (originalKey !== undefined) {
            process.env.APP_BACKEND_API_KEY = originalKey;
        } else {
            delete process.env.APP_BACKEND_API_KEY;
        }
        if (originalEnv !== undefined) {
            process.env.BACKEND_ENV = originalEnv;
        } else {
            delete process.env.BACKEND_ENV;
        }
    });

    beforeEach(() => {
        logOutput = [];
        // Create a pino logger that writes to our capture array
        const stream = {
            write(chunk) {
                logOutput.push(chunk);
            },
        };
        setLogger(pino({ level: 'trace' }, stream));
    });

    afterEach(() => {
        // Restore default logger
        setLogger(pino({ level: 'info' }));
    });

    it('does NOT log token value on unauthorized request', async () => {
        const badToken = 'attacker-token-value-xyz';

        await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-backend-key', badToken)
            .send({ contents: [] });

        const allLogs = logOutput.join('');
        assert.ok(!allLogs.includes(badToken), `Log output must not contain the token value. Got: ${allLogs.substring(0, 500)}`);
    });

    it('does NOT log the valid token on successful auth', async () => {
        const validToken = 'super-secret-api-key-12345';

        await request(app)
            .post('/v1beta/models/test:generateContent')
            .set('x-backend-key', validToken)
            .send({ contents: [] });

        const allLogs = logOutput.join('');
        assert.ok(!allLogs.includes(validToken), `Log output must not contain the valid token. Got: ${allLogs.substring(0, 500)}`);
    });
});
