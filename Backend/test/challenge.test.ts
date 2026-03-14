import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';
import { app, challenges } from '../dist/server';

describe('GET /attest/challenge', () => {
    beforeEach(() => {
        challenges.clear();
    });

    it('returns 200 with a UUID-format challenge', async () => {
        const res = await request(app).get('/attest/challenge').expect(200);
        assert.ok(res.body.challenge);
        // UUID v4 format
        assert.match(res.body.challenge, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    });

    it('returns unique challenges on each call', async () => {
        const res1 = await request(app).get('/attest/challenge').expect(200);
        const res2 = await request(app).get('/attest/challenge').expect(200);
        assert.notEqual(res1.body.challenge, res2.body.challenge);
    });

    it('stores the challenge in the challenges Map', async () => {
        const res = await request(app).get('/attest/challenge').expect(200);
        assert.ok(challenges.has(res.body.challenge));
        assert.ok(challenges.get(res.body.challenge).createdAt);
    });
});
