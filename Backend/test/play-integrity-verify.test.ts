/**
 * T1.9.b — Play Integrity verifier tests.
 *
 * Two layers of coverage:
 *  1. Unit tests against `verifyPlayIntegrityToken` directly, using an injected
 *     mock decoder. This exercises every verdict check independently without
 *     touching the network, ADC, or Google API.
 *  2. Integration test against `/attest/verify` with `X-App-Platform: android`
 *     and a mocked decoder via `setAndroidDecoderForTest`. This proves the
 *     route end-to-end: challenge validation, secret issuance, Firestore write,
 *     and the response shape the Android client consumes.
 */
import { describe, it, before, beforeEach, after } from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'crypto';
import request from 'supertest';
import {
    app,
    attestedKeys,
    challenges,
    setDb,
    setHmacSecret,
    setAppleRootCa,
    setAndroidDecoderForTest,
    globalLimiter,
    attestLimiter,
} from '../dist/server';
import { verifyPlayIntegrityToken, DecodedIntegrityPayload, DecoderFn } from '../dist/src/play-integrity';

const PACKAGE_NAME = 'com.pning80.watchmycalories';

function makeValidPayload(challenge: string, packageName = PACKAGE_NAME): DecodedIntegrityPayload {
    return {
        requestDetails: {
            requestPackageName: packageName,
            requestHash: challenge,
            timestampMillis: String(Date.now()),
        },
        appIntegrity: {
            appRecognitionVerdict: 'PLAY_RECOGNIZED',
            packageName,
            versionCode: '141',
        },
        deviceIntegrity: {
            deviceRecognitionVerdict: ['MEETS_DEVICE_INTEGRITY'],
        },
    };
}

function fixedDecoder(payload: DecodedIntegrityPayload | (() => DecodedIntegrityPayload)): DecoderFn {
    return async () => (typeof payload === 'function' ? (payload as () => DecodedIntegrityPayload)() : payload);
}

function resetRateLimits() {
    for (const ip of ['::ffff:127.0.0.1', '127.0.0.1', '::1']) {
        globalLimiter.resetKey(ip);
        attestLimiter.resetKey(ip);
    }
}

describe('verifyPlayIntegrityToken (unit)', () => {
    const challenge = 'a-valid-challenge';

    it('accepts a well-formed payload', async () => {
        await verifyPlayIntegrityToken({
            token: 'any-token',
            challenge,
            packageName: PACKAGE_NAME,
            decoder: fixedDecoder(makeValidPayload(challenge)),
        });
    });

    it('accepts older tokens using `nonce` instead of `requestHash`', async () => {
        const payload = makeValidPayload(challenge);
        delete payload.requestDetails!.requestHash;
        payload.requestDetails!.nonce = challenge;
        await verifyPlayIntegrityToken({
            token: 'any-token',
            challenge,
            packageName: PACKAGE_NAME,
            decoder: fixedDecoder(payload),
        });
    });

    it('throws on missing token', async () => {
        await assert.rejects(
            verifyPlayIntegrityToken({ token: '', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(makeValidPayload(challenge)) }),
            /missing_token/,
        );
    });

    it('throws on missing challenge', async () => {
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge: '', packageName: PACKAGE_NAME, decoder: fixedDecoder(makeValidPayload(challenge)) }),
            /missing_challenge/,
        );
    });

    it('throws on missing package name config', async () => {
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: '', decoder: fixedDecoder(makeValidPayload(challenge)) }),
            /missing_package_name_config/,
        );
    });

    it('throws on package_name_mismatch', async () => {
        const payload = makeValidPayload(challenge, 'com.attacker.fake');
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(payload) }),
            /package_name_mismatch/,
        );
    });

    it('throws on nonce_mismatch when neither requestHash nor nonce match', async () => {
        const payload = makeValidPayload(challenge);
        payload.requestDetails!.requestHash = 'not-the-challenge';
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(payload) }),
            /nonce_mismatch/,
        );
    });

    it('throws on app_not_play_recognized when verdict is UNRECOGNIZED_VERSION', async () => {
        const payload = makeValidPayload(challenge);
        payload.appIntegrity!.appRecognitionVerdict = 'UNRECOGNIZED_VERSION';
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(payload) }),
            /app_not_play_recognized/,
        );
    });

    it('throws on app_package_mismatch when appIntegrity.packageName disagrees', async () => {
        const payload = makeValidPayload(challenge);
        payload.appIntegrity!.packageName = 'com.attacker.fake';
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(payload) }),
            /app_package_mismatch/,
        );
    });

    it('throws on device_integrity_not_met when verdict array is empty', async () => {
        const payload = makeValidPayload(challenge);
        payload.deviceIntegrity!.deviceRecognitionVerdict = [];
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(payload) }),
            /device_integrity_not_met/,
        );
    });

    it('throws on device_integrity_not_met when only weaker verdicts are present', async () => {
        const payload = makeValidPayload(challenge);
        payload.deviceIntegrity!.deviceRecognitionVerdict = ['MEETS_BASIC_INTEGRITY'];
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(payload) }),
            /device_integrity_not_met/,
        );
    });

    it('throws on missing_app_integrity', async () => {
        const payload: DecodedIntegrityPayload = {
            requestDetails: { requestPackageName: PACKAGE_NAME, requestHash: challenge },
        };
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(payload) }),
            /missing_app_integrity/,
        );
    });

    it('throws on missing_device_integrity', async () => {
        const payload: DecodedIntegrityPayload = {
            requestDetails: { requestPackageName: PACKAGE_NAME, requestHash: challenge },
            appIntegrity: { appRecognitionVerdict: 'PLAY_RECOGNIZED' },
        };
        await assert.rejects(
            verifyPlayIntegrityToken({ token: 't', challenge, packageName: PACKAGE_NAME, decoder: fixedDecoder(payload) }),
            /missing_device_integrity/,
        );
    });
});

describe('POST /attest/verify (Android path, integration)', () => {
    const origTeamId = process.env.APPLE_TEAM_ID;
    const origPackage = process.env.PLAY_INTEGRITY_PACKAGE_NAME;

    before(() => {
        process.env.APPLE_TEAM_ID = 'TESTTEAMID';
        process.env.PLAY_INTEGRITY_PACKAGE_NAME = PACKAGE_NAME;
        setAppleRootCa('placeholder');
        setHmacSecret('play-integrity-test-hmac');
    });

    beforeEach(() => {
        challenges.clear();
        attestedKeys.clear();
        setDb(null);
        setAndroidDecoderForTest(null);
        resetRateLimits();
    });

    after(() => {
        if (origTeamId !== undefined) process.env.APPLE_TEAM_ID = origTeamId; else delete process.env.APPLE_TEAM_ID;
        if (origPackage !== undefined) process.env.PLAY_INTEGRITY_PACKAGE_NAME = origPackage; else delete process.env.PLAY_INTEGRITY_PACKAGE_NAME;
        setAndroidDecoderForTest(null);
        setHmacSecret(null);
    });

    async function getChallenge(): Promise<string> {
        const r = await request(app).get('/attest/challenge').expect(200);
        return r.body.challenge;
    }

    it('returns success + androidAssertionSecret on a valid token', async () => {
        const challenge = await getChallenge();
        setAndroidDecoderForTest(fixedDecoder(makeValidPayload(challenge)));

        const keyID = crypto.randomBytes(32).toString('base64');
        const res = await request(app)
            .post('/attest/verify')
            .set('X-App-Platform', 'android')
            .send({ keyID, attestation: 'irrelevant-with-stub', challenge })
            .expect(200);

        assert.equal(res.body.success, true);
        assert.ok(res.body.androidAssertionSecret, 'response must include the secret');
        assert.match(res.body.androidAssertionSecret, /^[0-9a-f]{64}$/, 'secret is 32 bytes hex');

        // Server cached the key with the secret
        const cached = attestedKeys.get(keyID);
        assert.ok(cached);
        assert.equal(cached.platform, 'android');
        assert.equal(cached.androidAssertionSecret, res.body.androidAssertionSecret);
        assert.equal(cached.counter, 0);
    });

    it('persists the key to Firestore with platform="android" and the secret', async () => {
        const challenge = await getChallenge();
        setAndroidDecoderForTest(fixedDecoder(makeValidPayload(challenge)));

        let captured: any = null;
        setDb({
            collection: () => ({
                doc: () => ({
                    set: async (data: any) => { captured = data; },
                }),
            }),
        });

        const keyID = crypto.randomBytes(32).toString('base64');
        await request(app)
            .post('/attest/verify')
            .set('X-App-Platform', 'android')
            .send({ keyID, attestation: 't', challenge })
            .expect(200);

        assert.ok(captured);
        assert.equal(captured.platform, 'android');
        assert.match(captured.androidAssertionSecret, /^[0-9a-f]{64}$/);
        assert.equal(captured.counter, 0);
        assert.ok(captured.publicKeyPem.includes('BEGIN PUBLIC KEY'));
    });

    it('returns 401 attestation_invalid when the decoder throws', async () => {
        const challenge = await getChallenge();
        setAndroidDecoderForTest((async () => { throw new Error('app_not_play_recognized:UNRECOGNIZED_VERSION'); }) as DecoderFn);

        const keyID = crypto.randomBytes(32).toString('base64');
        const res = await request(app)
            .post('/attest/verify')
            .set('X-App-Platform', 'android')
            .send({ keyID, attestation: 't', challenge })
            .expect(401);
        assert.equal(res.body.error, 'attestation_invalid');
    });

    it('returns 401 attestation_invalid on bad challenge (not issued)', async () => {
        setAndroidDecoderForTest(fixedDecoder(makeValidPayload('a-valid-challenge')));
        const res = await request(app)
            .post('/attest/verify')
            .set('X-App-Platform', 'android')
            .send({ keyID: 'k', attestation: 't', challenge: 'never-issued' })
            .expect(401);
        assert.equal(res.body.error, 'attestation_invalid');
    });

    it('returns 400 missing_field on empty body', async () => {
        const res = await request(app)
            .post('/attest/verify')
            .set('X-App-Platform', 'android')
            .send({})
            .expect(400);
        assert.equal(res.body.error, 'missing_field');
    });

    it('challenge is one-time use (second attempt with same challenge → 401)', async () => {
        const challenge = await getChallenge();
        setAndroidDecoderForTest(fixedDecoder(makeValidPayload(challenge)));

        const keyID1 = crypto.randomBytes(32).toString('base64');
        await request(app)
            .post('/attest/verify')
            .set('X-App-Platform', 'android')
            .send({ keyID: keyID1, attestation: 't', challenge })
            .expect(200);

        const keyID2 = crypto.randomBytes(32).toString('base64');
        const res = await request(app)
            .post('/attest/verify')
            .set('X-App-Platform', 'android')
            .send({ keyID: keyID2, attestation: 't', challenge })
            .expect(401);
        assert.equal(res.body.error, 'attestation_invalid');
    });

    it('returns 503 play_integrity_not_configured when PLAY_INTEGRITY_PACKAGE_NAME is unset', async () => {
        const saved = process.env.PLAY_INTEGRITY_PACKAGE_NAME;
        delete process.env.PLAY_INTEGRITY_PACKAGE_NAME;
        try {
            const challenge = await getChallenge();
            const res = await request(app)
                .post('/attest/verify')
                .set('X-App-Platform', 'android')
                .send({ keyID: 'k', attestation: 't', challenge })
                .expect(503);
            assert.equal(res.body.error, 'play_integrity_not_configured');
        } finally {
            if (saved !== undefined) process.env.PLAY_INTEGRITY_PACKAGE_NAME = saved;
        }
    });
});
