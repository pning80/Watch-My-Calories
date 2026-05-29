/**
 * Play Integrity token verifier (PORTING_CRITERIA.md T1.9.b).
 *
 * Decodes a Play Integrity token via Google's `playintegrity.googleapis.com`
 * API and applies the verdict checks the criteria requires:
 *
 *   - appIntegrity.appRecognitionVerdict === "PLAY_RECOGNIZED"
 *   - deviceIntegrity.deviceRecognitionVerdict includes "MEETS_DEVICE_INTEGRITY"
 *   - requestDetails.requestPackageName === PLAY_INTEGRITY_PACKAGE_NAME
 *   - requestDetails.requestHash (or .nonce on older tokens) === challenge issued by /attest/challenge
 *
 * Authentication uses Application Default Credentials via the existing Cloud
 * Run runtime service account (the same SA used for Firestore + Secret Manager).
 * No JSON key file is downloaded and no extra GCP IAM role binding is required:
 * linking the Cloud project in Play Console + enabling `playintegrity.googleapis.com`
 * on that project is the access grant. The SA just needs the `playintegrity`
 * OAuth scope (set below) and ADC. See T1.9.e.
 *
 * The `decoder` function is injectable so tests can mock the Google API call
 * without needing real network/auth. Production code passes the default
 * `decodeViaGoogleApi`.
 */
import { GoogleAuth } from 'google-auth-library';
import { createLogger } from './logger';

const log = createLogger('play-integrity');

/**
 * The shape of the `tokenPayloadExternal` we care about. The Play Integrity
 * API may return additional fields; we ignore them.
 */
export interface DecodedIntegrityPayload {
    requestDetails?: {
        requestPackageName?: string;
        requestHash?: string;
        nonce?: string;
        timestampMillis?: string;
    };
    appIntegrity?: {
        appRecognitionVerdict?: string;
        packageName?: string;
        certificateSha256Digest?: string[];
        versionCode?: string;
    };
    deviceIntegrity?: {
        deviceRecognitionVerdict?: string[];
    };
    accountDetails?: {
        appLicensingVerdict?: string;
    };
}

export type DecoderFn = (token: string, packageName: string) => Promise<DecodedIntegrityPayload>;

export interface VerifyArgs {
    token: string;
    challenge: string;
    packageName: string;
    decoder?: DecoderFn;
}

/**
 * Verify a Play Integrity token. Throws on any failure; returns the decoded
 * payload on success. Callers translate the throw into a 401 response without
 * leaking which specific check failed.
 */
export async function verifyPlayIntegrityToken({
    token,
    challenge,
    packageName,
    decoder = decodeViaGoogleApi,
}: VerifyArgs): Promise<DecodedIntegrityPayload> {
    if (!token || typeof token !== 'string') {
        throw new Error('missing_token');
    }
    if (!challenge || typeof challenge !== 'string') {
        throw new Error('missing_challenge');
    }
    if (!packageName) {
        throw new Error('missing_package_name_config');
    }

    const payload = await decoder(token, packageName);

    // requestDetails — package name + nonce/requestHash binding
    const reqDetails = payload.requestDetails;
    if (!reqDetails) throw new Error('missing_request_details');
    if (reqDetails.requestPackageName !== packageName) {
        throw new Error('package_name_mismatch');
    }
    // The newer Standard Play Integrity API uses requestHash; older tokens use nonce.
    // We accept either, but at least one must match the challenge we issued.
    const matchesRequestHash = reqDetails.requestHash === challenge;
    const matchesNonce = reqDetails.nonce === challenge;
    if (!matchesRequestHash && !matchesNonce) {
        throw new Error('nonce_mismatch');
    }

    // appIntegrity
    const appIntegrity = payload.appIntegrity;
    if (!appIntegrity) throw new Error('missing_app_integrity');
    if (appIntegrity.appRecognitionVerdict !== 'PLAY_RECOGNIZED') {
        throw new Error(`app_not_play_recognized:${appIntegrity.appRecognitionVerdict}`);
    }
    if (appIntegrity.packageName && appIntegrity.packageName !== packageName) {
        // Defense in depth: the appIntegrity.packageName should also match.
        throw new Error('app_package_mismatch');
    }

    // deviceIntegrity
    const deviceIntegrity = payload.deviceIntegrity;
    if (!deviceIntegrity) throw new Error('missing_device_integrity');
    const verdicts = deviceIntegrity.deviceRecognitionVerdict ?? [];
    if (!verdicts.includes('MEETS_DEVICE_INTEGRITY')) {
        throw new Error(`device_integrity_not_met:${verdicts.join(',')}`);
    }

    return payload;
}

// --- Default decoder: real Google API call via ADC ---

/**
 * Decode a Play Integrity token by calling Google's
 * `playintegrity.googleapis.com/v1/{packageName}:decodeIntegrityToken`.
 *
 * Auth: Application Default Credentials from the runtime service account in
 * the GCP project the Play Console app is linked to (no extra IAM role
 * binding required — linking + API enablement is the grant). If ADC is
 * unavailable (no creds at all), throws `play_integrity_auth_unavailable`.
 */
async function decodeViaGoogleApi(token: string, packageName: string): Promise<DecodedIntegrityPayload> {
    const auth = new GoogleAuth({
        scopes: ['https://www.googleapis.com/auth/playintegrity'],
    });
    let client;
    try {
        client = await auth.getClient();
    } catch (err: any) {
        log.error({ error: err.message }, 'ADC unavailable for Play Integrity');
        throw new Error('play_integrity_auth_unavailable');
    }

    const url = `https://playintegrity.googleapis.com/v1/${encodeURIComponent(packageName)}:decodeIntegrityToken`;
    const res = await client.request<{ tokenPayloadExternal?: DecodedIntegrityPayload }>({
        url,
        method: 'POST',
        data: { integrityToken: token },
    });

    const payload = res.data?.tokenPayloadExternal;
    if (!payload) {
        throw new Error('decode_returned_empty');
    }
    return payload;
}
