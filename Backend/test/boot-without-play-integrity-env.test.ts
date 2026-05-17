/**
 * T1.10.e — Regression guard: the server boots and serves iOS-path requests
 * normally when the Play Integrity env vars are absent.
 *
 * Rationale: we want to be able to deploy the Android-supporting backend
 * upgrade decoupled from the Play Integrity secrets rollout. iOS must be
 * unaffected. This test asserts that no module-level code reads
 * PLAY_INTEGRITY_PROJECT_NUMBER or PLAY_INTEGRITY_PACKAGE_NAME in a way
 * that breaks the iOS path when those vars are missing.
 */
import { describe, it, before, beforeEach, after } from 'node:test';
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

describe('T1.10.e — boot without PLAY_INTEGRITY env vars', () => {
    const origProjectNumber = process.env.PLAY_INTEGRITY_PROJECT_NUMBER;
    const origPackageName = process.env.PLAY_INTEGRITY_PACKAGE_NAME;

    before(() => {
        // Explicitly unset so the assertions below mean what they say even
        // when the test runner inherits values from .env or shell.
        delete process.env.PLAY_INTEGRITY_PROJECT_NUMBER;
        delete process.env.PLAY_INTEGRITY_PACKAGE_NAME;
        process.env.APPLE_TEAM_ID = 'TESTTEAMID';
        setAppleRootCa('placeholder');
        setHmacSecret('test-hmac-secret');
    });

    beforeEach(() => {
        challenges.clear();
        attestedKeys.clear();
        setDb(null);
        resetRateLimits();
    });

    it('server module imported successfully (no env-var assertion at import time)', () => {
        // Reaching this line proves the import in the file header didn't throw.
        assert.ok(app, 'app should be exported from server module');
    });

    it('GET /attest/challenge returns 200 with a UUID-format challenge (iOS path, no platform header)', async () => {
        assert.equal(process.env.PLAY_INTEGRITY_PROJECT_NUMBER, undefined);
        assert.equal(process.env.PLAY_INTEGRITY_PACKAGE_NAME, undefined);
        const res = await request(app).get('/attest/challenge').expect(200);
        assert.match(res.body.challenge, UUID_RE);
    });

    it('POST /attest/verify (iOS path, no platform header) returns 400 for missing fields — server reached the handler logic', async () => {
        assert.equal(process.env.PLAY_INTEGRITY_PROJECT_NUMBER, undefined);
        assert.equal(process.env.PLAY_INTEGRITY_PACKAGE_NAME, undefined);
        const res = await request(app).post('/attest/verify').send({}).expect(400);
        assert.match(res.body.error, /Missing keyID/);
    });

    it('GET / health endpoint still serves', async () => {
        const res = await request(app).get('/').expect(200);
        assert.equal(res.body.status, 'ok');
    });

    it('Android path returns 503 play_integrity_not_configured when env is unset (T1.10.e)', async () => {
        // T1.9.b is implemented but requires PLAY_INTEGRITY_PACKAGE_NAME to be
        // set to call Google. When it isn't (this test's state), the Android
        // branch must fail cleanly with a stable error code — not 500 from an
        // unguarded env read, and not corrupt the iOS path.
        const res = await request(app)
            .post('/attest/verify')
            .set('X-App-Platform', 'android')
            .send({ keyID: 'k', attestation: 'a', challenge: 'c' })
            .expect(503);
        assert.equal(res.body.error, 'play_integrity_not_configured');
    });

    // Restore env after the suite so other tests aren't affected by the delete above.
    after(() => {
        if (origProjectNumber !== undefined) process.env.PLAY_INTEGRITY_PROJECT_NUMBER = origProjectNumber;
        if (origPackageName !== undefined) process.env.PLAY_INTEGRITY_PACKAGE_NAME = origPackageName;
    });
});
