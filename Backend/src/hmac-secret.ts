import { createLogger } from './logger';
const log = createLogger('hmac-secret');

let cachedSecret: string | null = null;

/**
 * Initialize the HMAC secret. Call once at startup before any attestation operations.
 *
 * Resolution order:
 *   1. process.env.ATTEST_HMAC_SECRET (local dev / tests)
 *   2. Secret Manager (production — deploy.sh must have created the version)
 */
export async function initHmacSecret(): Promise<void> {
    // 1. Env-var override (local dev / tests)
    if (process.env.ATTEST_HMAC_SECRET) {
        cachedSecret = process.env.ATTEST_HMAC_SECRET;
        return;
    }

    // 2. Secret Manager (read-only — deploy.sh generates the value)
    const secretName = process.env.ATTEST_HMAC_SECRET_NAME;
    if (!secretName) {
        log.warn('ATTEST_HMAC_SECRET_NAME not set — HMAC operations disabled');
        return;
    }

    try {
        const { SecretManagerServiceClient } = require('@google-cloud/secret-manager');
        const client = new SecretManagerServiceClient();
        const projectId = await client.getProjectId();
        const fullName = `projects/${projectId}/secrets/${secretName}/versions/latest`;

        const [version] = await client.accessSecretVersion({ name: fullName });
        cachedSecret = version.payload.data.toString('utf8');
        log.info({ secretName }, 'HMAC secret loaded from Secret Manager');
    } catch (err: any) {
        log.error({ error: err.message }, 'HMAC secret: Secret Manager error');
    }
}

/** Returns the cached HMAC secret, or null if unavailable. */
export function getHmacSecret(): string | null {
    return cachedSecret;
}

/** Test-only setter for injecting a mock value (mirrors setDb pattern). */
export function setHmacSecret(value: string | null): void {
    cachedSecret = value;
}
