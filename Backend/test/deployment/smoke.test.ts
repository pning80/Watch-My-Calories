/**
 * Deployment smoke tests — hit real Cloud Run endpoints.
 *
 * NOT included in `npm test`. Run via:
 *   npm run test:smoke          (dev)
 *   npm run test:smoke:prod     (prod)
 *
 * URL resolution:
 *   1. CLOUD_RUN_URL env var (for CI)
 *   2. gcloud CLI: `gcloud run services describe <service> --region us-central1`
 */

import { describe, it, before } from 'node:test';
import assert from 'node:assert/strict';
import { execSync } from 'child_process';

const TARGET_ENV = process.env.TARGET_ENV || 'dev';
const SERVICE_NAME = TARGET_ENV === 'prod'
    ? 'watchmycalories-backend'
    : 'watchmycalories-backend-dev';
const REGION = 'us-central1';

let BASE_URL: string | null = null;

describe(`Deployment smoke tests (${TARGET_ENV})`, () => {
    before(() => {
        // 1. Env var override (CI)
        if (process.env.CLOUD_RUN_URL) {
            BASE_URL = process.env.CLOUD_RUN_URL.replace(/\/$/, '');
            return;
        }

        // 2. Resolve via gcloud CLI
        try {
            const url = execSync(
                `gcloud run services describe ${SERVICE_NAME} --region ${REGION} --format="value(status.url)"`,
                { encoding: 'utf8', timeout: 15_000 }
            ).trim();
            if (url) BASE_URL = url;
        } catch {
            // gcloud not available or service not found
        }

        if (!BASE_URL) {
            console.warn(`\nSkipping smoke tests: could not resolve URL for ${SERVICE_NAME}.`);
            console.warn('Set CLOUD_RUN_URL env var or ensure gcloud CLI is configured.\n');
        }
    });

    it('GET / returns 200 with JSON health status', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/`);
        assert.equal(res.status, 200);
        const body = await res.json();
        assert.equal(body.status, 'ok');
        assert.equal(typeof body.uptime, 'number');
        assert.equal(typeof body.attestedKeysCount, 'number');
        assert.ok(body.env, 'Should include env field');
    });

    it('responses include CORS headers', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/`);
        assert.equal(res.headers.get('access-control-allow-origin'), '*');
    });

    it('GET /attest/challenge returns 200 with UUID challenge', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/attest/challenge`);
        assert.equal(res.status, 200);

        const body = await res.json();
        assert.ok(body.challenge, 'Response should contain challenge field');

        // Validate UUID v4 format
        const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
        assert.match(body.challenge, uuidRegex, `Challenge should be UUID v4: ${body.challenge}`);
    });

    it('POST /v1beta/models/* with no auth returns 401', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/v1beta/models/gemini-pro:generateContent`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contents: [] }),
        });
        assert.equal(res.status, 401);
    });

    it('POST /v1beta/models/* with invalid legacy key returns 401', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/v1beta/models/gemini-pro:generateContent`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'x-backend-key': 'invalid-key-value-12345',
            },
            body: JSON.stringify({ contents: [] }),
        });
        assert.equal(res.status, 401);
    });

    it('responses include rate limit headers', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/`);
        assert.ok(res.headers.has('ratelimit-limit'), 'Should have ratelimit-limit header');
        assert.ok(res.headers.has('ratelimit-remaining'), 'Should have ratelimit-remaining header');
    });

    // --- New tests ---

    it('POST /attest/verify with missing fields returns 400', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/attest/verify`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ keyID: 'test' }),
        });
        assert.equal(res.status, 400);
        const body = await res.json();
        assert.ok(body.error.includes('Missing'), `Expected error to include 'Missing', got: ${body.error}`);
    });

    it('POST /attest/verify with fabricated challenge returns 400', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/attest/verify`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                keyID: 'test',
                attestation: 'dGVzdA==',
                challenge: '00000000-0000-4000-8000-000000000000',
            }),
        });
        assert.equal(res.status, 400);
        const body = await res.json();
        assert.ok(
            body.error.includes('Invalid or expired challenge'),
            `Expected error about invalid challenge, got: ${body.error}`
        );
    });

    it('GET /nonexistent returns 404', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/nonexistent`);
        assert.equal(res.status, 404);
    });

    it('health endpoint env field matches TARGET_ENV', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/`);
        const body = await res.json();
        assert.equal(body.env, TARGET_ENV, `Expected env '${TARGET_ENV}', got '${body.env}'`);
    });

    it('CORS preflight OPTIONS request returns proper headers', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/`, {
            method: 'OPTIONS',
            headers: {
                'Origin': 'https://example.com',
                'Access-Control-Request-Method': 'POST',
            },
        });
        assert.equal(res.status, 204);
        assert.ok(res.headers.has('access-control-allow-origin'), 'Should have access-control-allow-origin');
        assert.ok(res.headers.has('access-control-allow-methods'), 'Should have access-control-allow-methods');
    });

    it('error responses include { error: string } shape', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/v1beta/models/gemini-pro:generateContent`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contents: [] }),
        });
        assert.equal(res.status, 401);
        const body = await res.json();
        assert.equal(typeof body.error, 'string', 'error field should be a string');
        assert.ok(body.error.length > 0, 'error field should not be empty');
    });

    it('health endpoint uptime is positive', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/`);
        const body = await res.json();
        assert.ok(body.uptime > 0, `Expected positive uptime, got ${body.uptime}`);
    });

    it('large request body (>12MB) returns 413', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const largePayload = 'x'.repeat(12.5 * 1024 * 1024);
        const res = await fetch(`${BASE_URL}/v1beta/models/gemini-pro:generateContent`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contents: largePayload }),
        });
        assert.equal(res.status, 413);
        const body = await res.json();
        assert.ok(
            body.error.includes('too large'),
            `Expected error about body too large, got: ${body.error}`
        );
    });

    it('POST /attest/verify with valid challenge but garbage attestation returns 400', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        // Step 1: Get a real challenge
        const challengeRes = await fetch(`${BASE_URL}/attest/challenge`);
        assert.equal(challengeRes.status, 200);
        const { challenge } = await challengeRes.json();

        // Step 2: POST with real challenge but garbage attestation
        const res = await fetch(`${BASE_URL}/attest/verify`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                keyID: 'test',
                attestation: 'dGVzdA==',
                challenge,
            }),
        });
        assert.equal(res.status, 400);
        const body = await res.json();
        assert.ok(
            body.error.includes('Attestation verification failed'),
            `Expected attestation verification failure, got: ${body.error}`
        );
    });

    it('rate limit header values are reasonable', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res = await fetch(`${BASE_URL}/`);
        const limit = parseInt(res.headers.get('ratelimit-limit')!, 10);
        const remaining = parseInt(res.headers.get('ratelimit-remaining')!, 10);
        assert.ok(limit > 0, `Expected positive limit, got ${limit}`);
        assert.ok(remaining >= 0, `Expected non-negative remaining, got ${remaining}`);
        assert.ok(remaining <= limit, `remaining (${remaining}) should not exceed limit (${limit})`);
    });

    it('multiple challenges are unique', async (t) => {
        if (!BASE_URL) return t.skip('No Cloud Run URL');

        const res1 = await fetch(`${BASE_URL}/attest/challenge`);
        const res2 = await fetch(`${BASE_URL}/attest/challenge`);
        if (res1.status === 429 || res2.status === 429) return t.skip('Rate limited');
        assert.equal(res1.status, 200);
        assert.equal(res2.status, 200);
        const { challenge: c1 } = await res1.json();
        const { challenge: c2 } = await res2.json();
        assert.notEqual(c1, c2, 'Consecutive challenges should be unique');
    });
});
