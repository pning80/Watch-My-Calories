/**
 * Tests for the X-App-Platform header dispatch on the attestation routes.
 *
 * Covers PORTING_CRITERIA.md T1.9.a (platform discriminator) and T1.10.b
 * (missing header defaults to iOS so in-field clients keep working).
 */
import { describe, it, before, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';
import {
    app,
    attestedKeys,
    challenges,
    setAppleRootCa,
    setDb,
    setHmacSecret,
    globalLimiter,
    attestLimiter,
} from '../dist/server';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function resetRateLimits() {
    for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
        globalLimiter.resetKey(ip);
        attestLimiter.resetKey(ip);
    }
}

describe('X-App-Platform header dispatch', () => {
    before(() => {
        // Minimal setup so the iOS path doesn't 500 on env-var checks.
        process.env.APPLE_TEAM_ID = 'TESTTEAMID';
        setAppleRootCa('placeholder'); // value isn't read on the early-return paths we test
        setHmacSecret('test-hmac-secret');
    });

    beforeEach(() => {
        challenges.clear();
        attestedKeys.clear();
        setDb(null);
        resetRateLimits();
    });

    describe('GET /attest/challenge', () => {
        it('returns a challenge with no X-App-Platform header (backward compat)', async () => {
            const res = await request(app).get('/attest/challenge').expect(200);
            assert.match(res.body.challenge, UUID_RE);
        });

        it('returns a challenge with X-App-Platform: ios', async () => {
            const res = await request(app)
                .get('/attest/challenge')
                .set('X-App-Platform', 'ios')
                .expect(200);
            assert.match(res.body.challenge, UUID_RE);
        });

        it('returns a challenge with X-App-Platform: android', async () => {
            const res = await request(app)
                .get('/attest/challenge')
                .set('X-App-Platform', 'android')
                .expect(200);
            assert.match(res.body.challenge, UUID_RE);
        });

        it('treats an unknown X-App-Platform value as ios (default)', async () => {
            const res = await request(app)
                .get('/attest/challenge')
                .set('X-App-Platform', 'symbian')
                .expect(200);
            assert.match(res.body.challenge, UUID_RE);
        });
    });

    describe('POST /attest/verify', () => {
        // The Android branch is now a real handler (T1.9.b). Dispatch is verified
        // by checking the response *shape*: the Android branch returns
        // `{ error: "missing_field" }`, the iOS branch returns `{ error: "Missing keyID..." }`.
        it('with X-App-Platform: android, routes to Android branch (Android-shaped error)', async () => {
            const res = await request(app)
                .post('/attest/verify')
                .set('X-App-Platform', 'android')
                .send({})
                .expect(400);
            assert.equal(res.body.error, 'missing_field');
        });

        it('with no X-App-Platform header, takes the iOS path (iOS-shaped 400)', async () => {
            const res = await request(app)
                .post('/attest/verify')
                .send({})
                .expect(400);
            assert.match(res.body.error, /Missing keyID/);
        });

        it('with X-App-Platform: ios, takes the iOS path (iOS-shaped 400)', async () => {
            const res = await request(app)
                .post('/attest/verify')
                .set('X-App-Platform', 'ios')
                .send({})
                .expect(400);
            assert.match(res.body.error, /Missing keyID/);
        });

        it('with an unknown X-App-Platform value, takes the iOS path (default)', async () => {
            const res = await request(app)
                .post('/attest/verify')
                .set('X-App-Platform', 'symbian')
                .send({})
                .expect(400);
            assert.match(res.body.error, /Missing keyID/);
        });

        it('case-insensitive: X-App-Platform: ANDROID still routes to Android branch', async () => {
            const res = await request(app)
                .post('/attest/verify')
                .set('X-App-Platform', 'ANDROID')
                .send({})
                .expect(400);
            assert.equal(res.body.error, 'missing_field');
        });
    });
});
