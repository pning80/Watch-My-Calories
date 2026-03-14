import { describe, it, before, after, afterEach, mock } from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';
import { app } from '../dist/server';

describe('Gemini proxy endpoint', () => {
    const originalKey = process.env.APP_BACKEND_API_KEY;
    const originalGeminiKey = process.env.GEMINI_API_KEY;
    const originalModel = process.env.GEMINI_MODEL_NAME;

    before(() => {
        process.env.APP_BACKEND_API_KEY = 'test-key';
    });

    after(() => {
        if (originalKey !== undefined) {
            process.env.APP_BACKEND_API_KEY = originalKey;
        } else {
            delete process.env.APP_BACKEND_API_KEY;
        }
        if (originalGeminiKey !== undefined) {
            process.env.GEMINI_API_KEY = originalGeminiKey;
        } else {
            delete process.env.GEMINI_API_KEY;
        }
        if (originalModel !== undefined) {
            process.env.GEMINI_MODEL_NAME = originalModel;
        } else {
            delete process.env.GEMINI_MODEL_NAME;
        }
    });

    afterEach(() => {
        mock.restoreAll();
    });

    function authedPost(path = '/v1beta/models/test:generateContent') {
        return request(app)
            .post(path)
            .set('x-backend-key', 'test-key');
    }

    // --- Environment variable validation ---

    describe('environment variable validation', () => {
        it('returns 500 when GEMINI_API_KEY missing', async () => {
            delete process.env.GEMINI_API_KEY;
            process.env.GEMINI_MODEL_NAME = 'gemini-pro';

            const res = await authedPost().send({ contents: [] });
            assert.equal(res.status, 500);
            assert.equal(res.body.error, 'Server Configuration Error');
        });

        it('returns 500 when GEMINI_MODEL_NAME missing', async () => {
            process.env.GEMINI_API_KEY = 'fake-key';
            delete process.env.GEMINI_MODEL_NAME;

            const res = await authedPost().send({ contents: [] });
            assert.equal(res.status, 500);
            assert.equal(res.body.error, 'Server Configuration Error');
        });
    });

    // --- Successful proxy relay ---

    describe('successful proxy relay', () => {
        before(() => {
            process.env.GEMINI_API_KEY = 'fake-gemini-key';
            process.env.GEMINI_MODEL_NAME = 'gemini-2.0-flash';
        });

        it('relays request to Gemini and returns response', async () => {
            const geminiResponse = { candidates: [{ content: { parts: [{ text: 'hello' }] } }] };
            mock.method(globalThis, 'fetch', async () => ({
                ok: true,
                status: 200,
                json: async () => geminiResponse,
            }));

            const res = await authedPost().send({ contents: [{ parts: [{ text: 'test' }] }] });
            assert.equal(res.status, 200);
            assert.deepEqual(res.body, geminiResponse);
        });

        it('constructs correct Gemini URL', async () => {
            const mockFetch = mock.method(globalThis, 'fetch', async () => ({
                ok: true,
                status: 200,
                json: async () => ({ candidates: [] }),
            }));

            await authedPost().send({ contents: [] });

            const calledUrl = mockFetch.mock.calls[0].arguments[0];
            assert.equal(
                calledUrl,
                'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=fake-gemini-key'
            );
        });

        it('sends request body as JSON', async () => {
            const mockFetch = mock.method(globalThis, 'fetch', async () => ({
                ok: true,
                status: 200,
                json: async () => ({ candidates: [] }),
            }));

            const body = { contents: [{ parts: [{ text: 'describe this food' }] }] };
            await authedPost().send(body);

            const opts = mockFetch.mock.calls[0].arguments[1];
            assert.equal(opts.method, 'POST');
            assert.equal(opts.headers['Content-Type'], 'application/json');
            assert.deepEqual(JSON.parse(opts.body), body);
        });

        it('uses GEMINI_MODEL_NAME regardless of path param', async () => {
            const mockFetch = mock.method(globalThis, 'fetch', async () => ({
                ok: true,
                status: 200,
                json: async () => ({ candidates: [] }),
            }));

            // Path says "gemini-pro" but env says "gemini-2.0-flash"
            await authedPost('/v1beta/models/gemini-pro:generateContent').send({ contents: [] });

            const calledUrl = mockFetch.mock.calls[0].arguments[0];
            assert.ok(calledUrl.includes('gemini-2.0-flash'), `URL should use env var model, got: ${calledUrl}`);
            assert.ok(!calledUrl.includes('gemini-pro:'), `URL should not use path model, got: ${calledUrl}`);
        });
    });

    // --- Error relay ---

    describe('error relay from Gemini', () => {
        before(() => {
            process.env.GEMINI_API_KEY = 'fake-gemini-key';
            process.env.GEMINI_MODEL_NAME = 'gemini-2.0-flash';
        });

        for (const status of [400, 429, 500]) {
            it(`relays ${status} from Gemini`, async () => {
                const errorBody = { error: { message: `Error ${status}`, code: status } };
                mock.method(globalThis, 'fetch', async () => ({
                    ok: false,
                    status,
                    json: async () => errorBody,
                }));

                const res = await authedPost().send({ contents: [] });
                assert.equal(res.status, status);
                assert.deepEqual(res.body, errorBody);
            });
        }
    });

    // --- Network errors ---

    describe('network errors', () => {
        before(() => {
            process.env.GEMINI_API_KEY = 'fake-gemini-key';
            process.env.GEMINI_MODEL_NAME = 'gemini-2.0-flash';
        });

        it('returns 502 when fetch throws', async () => {
            mock.method(globalThis, 'fetch', async () => {
                throw new Error('ECONNREFUSED');
            });

            const res = await authedPost().send({ contents: [] });
            assert.equal(res.status, 502);
            assert.equal(res.body.error, 'Bad Gateway');
        });

        it('returns 502 on network rejection', async () => {
            mock.method(globalThis, 'fetch', () => Promise.reject(new Error('DNS resolution failed')));

            const res = await authedPost().send({ contents: [] });
            assert.equal(res.status, 502);
            assert.equal(res.body.error, 'Bad Gateway');
        });
    });

    // --- Body handling ---

    describe('body handling', () => {
        before(() => {
            process.env.GEMINI_API_KEY = 'fake-gemini-key';
            process.env.GEMINI_MODEL_NAME = 'gemini-2.0-flash';
        });

        it('handles empty request body', async () => {
            mock.method(globalThis, 'fetch', async () => ({
                ok: true,
                status: 200,
                json: async () => ({ candidates: [] }),
            }));

            const res = await authedPost().send({});
            assert.equal(res.status, 200);
        });

        it('handles large body (base64 image)', async () => {
            const largeBase64 = 'A'.repeat(100_000);
            const body = {
                contents: [{ parts: [{ inlineData: { mimeType: 'image/jpeg', data: largeBase64 } }] }],
            };

            const mockFetch = mock.method(globalThis, 'fetch', async () => ({
                ok: true,
                status: 200,
                json: async () => ({ candidates: [{ content: { parts: [{ text: 'food' }] } }] }),
            }));

            const res = await authedPost().send(body);
            assert.equal(res.status, 200);

            const sentBody = JSON.parse(mockFetch.mock.calls[0].arguments[1].body);
            assert.equal(sentBody.contents[0].parts[0].inlineData.data, largeBase64);
        });
    });
});
