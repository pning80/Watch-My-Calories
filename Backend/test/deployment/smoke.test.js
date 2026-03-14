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

const { describe, it, before } = require('node:test');
const assert = require('node:assert/strict');
const { execSync } = require('child_process');

const TARGET_ENV = process.env.TARGET_ENV || 'dev';
const SERVICE_NAME = TARGET_ENV === 'prod'
    ? 'watchmycalories-backend'
    : 'watchmycalories-backend-dev';
const REGION = 'us-central1';

let BASE_URL = null;

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
});
