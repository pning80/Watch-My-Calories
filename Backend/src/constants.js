const BUNDLE_ID = 'com.pning80.WatchMyCalories';
const ATTESTED_KEYS_COLLECTION = process.env.ATTESTED_KEYS_COLLECTION || 'attestedKeys-dev';
const KEY_PRELOAD_MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000; // 30 days
const CHALLENGE_TTL_MS = 60_000; // 60 seconds

module.exports = { BUNDLE_ID, ATTESTED_KEYS_COLLECTION, KEY_PRELOAD_MAX_AGE_MS, CHALLENGE_TTL_MS };
