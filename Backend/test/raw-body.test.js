const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const request = require('supertest');
const { captureRawBody } = require('../dist/server');

// Create a small express app that uses captureRawBody
function createTestApp() {
    const testApp = express();
    testApp.post('/test', captureRawBody, (req, res) => {
        res.json({
            rawBodyLength: req.rawBody.length,
            rawBodyHex: req.rawBody.toString('hex'),
            body: req.body,
        });
    });
    return testApp;
}

describe('captureRawBody middleware', () => {
    it('captures raw bytes and parses JSON body', async () => {
        const testApp = createTestApp();
        const payload = { hello: 'world', number: 42 };
        const res = await request(testApp)
            .post('/test')
            .set('Content-Type', 'application/json')
            .send(JSON.stringify(payload))
            .expect(200);

        assert.deepEqual(res.body.body, payload);
        assert.ok(res.body.rawBodyLength > 0);
        // Verify raw bytes match what was sent
        const rawBytes = Buffer.from(res.body.rawBodyHex, 'hex');
        assert.deepEqual(JSON.parse(rawBytes.toString()), payload);
    });

    it('captures raw bytes for non-JSON body', async () => {
        const testApp = createTestApp();
        const res = await request(testApp)
            .post('/test')
            .set('Content-Type', 'text/plain')
            .send('not json content')
            .expect(200);

        assert.ok(res.body.rawBodyLength > 0);
        assert.equal(res.body.body, undefined);
    });

    it('handles empty body', async () => {
        const testApp = createTestApp();
        const res = await request(testApp)
            .post('/test')
            .send('')
            .expect(200);

        assert.equal(res.body.rawBodyLength, 0);
    });

    it('preserves raw bytes for large body', async () => {
        const testApp = createTestApp();
        const largePayload = { data: 'x'.repeat(10000) };
        const res = await request(testApp)
            .post('/test')
            .set('Content-Type', 'application/json')
            .send(JSON.stringify(largePayload))
            .expect(200);

        assert.deepEqual(res.body.body, largePayload);
        const rawBytes = Buffer.from(res.body.rawBodyHex, 'hex');
        assert.deepEqual(JSON.parse(rawBytes.toString()), largePayload);
    });

    it('rejects body exceeding 12 MB limit', async () => {
        const testApp = createTestApp();
        const oversizedPayload = Buffer.alloc(12 * 1024 * 1024 + 1, 'x');
        const res = await request(testApp)
            .post('/test')
            .set('Content-Type', 'application/octet-stream')
            .send(oversizedPayload);

        assert.equal(res.status, 413);
        assert.equal(res.body.error, 'Request message body too large.');
    });

    it('accepts body just under 12 MB limit', async () => {
        const testApp = createTestApp();
        const payload = Buffer.alloc(1024 * 1024, 'x'); // 1 MB — well under limit
        const res = await request(testApp)
            .post('/test')
            .set('Content-Type', 'application/octet-stream')
            .send(payload)
            .expect(200);

        assert.equal(res.body.rawBodyLength, payload.length);
    });
});
