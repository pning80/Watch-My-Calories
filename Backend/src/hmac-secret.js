let cachedSecret = null;

/**
 * Initialize the HMAC secret. Call once at startup before any attestation operations.
 *
 * Resolution order:
 *   1. process.env.ATTEST_HMAC_SECRET (local dev / tests)
 *   2. Secret Manager (production — deploy.sh must have created the version)
 */
async function initHmacSecret() {
    // 1. Env-var override (local dev / tests)
    if (process.env.ATTEST_HMAC_SECRET) {
        cachedSecret = process.env.ATTEST_HMAC_SECRET;
        return;
    }

    // 2. Secret Manager (read-only — deploy.sh generates the value)
    const secretName = process.env.ATTEST_HMAC_SECRET_NAME;
    if (!secretName) {
        console.warn('HMAC secret: ATTEST_HMAC_SECRET_NAME not set — HMAC operations disabled.');
        return;
    }

    try {
        const { SecretManagerServiceClient } = require('@google-cloud/secret-manager');
        const client = new SecretManagerServiceClient();
        const projectId = await client.getProjectId();
        const fullName = `projects/${projectId}/secrets/${secretName}/versions/latest`;

        const [version] = await client.accessSecretVersion({ name: fullName });
        cachedSecret = version.payload.data.toString('utf8');
        console.log(`HMAC secret: loaded from Secret Manager (${secretName}).`);
    } catch (err) {
        console.error('HMAC secret: Secret Manager error:', err.message);
    }
}

/** Returns the cached HMAC secret, or null if unavailable. */
function getHmacSecret() {
    return cachedSecret;
}

/** Test-only setter for injecting a mock value (mirrors setDb pattern). */
function setHmacSecret(value) {
    cachedSecret = value;
}

module.exports = { initHmacSecret, getHmacSecret, setHmacSecret };
