package com.pning80.watchmycalories.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Android attestation manager (PORTING_CRITERIA.md T1.8).
 *
 * Mirrors iOS `AppAttestManager` in shape:
 *   1. On first use, fetches a challenge from /attest/challenge.
 *   2. Requests a Play Integrity token bound to that challenge.
 *   3. POSTs to /attest/verify with X-App-Platform=android.
 *   4. On success, persists the keyID + androidAssertionSecret to
 *      EncryptedSharedPreferences. Both survive process death.
 *   5. Every subsequent backend call calls [assertionHeaders] to attach:
 *        X-Android-Key-Id, X-Android-Counter, X-Android-Assertion
 *      where the assertion is HMAC-SHA256(secret, counter || ":" || sha256(body)).
 *
 * The counter is monotonic and persisted in the same encrypted prefs. On a
 * 401 `android_assertion_invalid` from the server, callers should invoke
 * [invalidateAndReattest] and retry once.
 *
 * Emulator / Play-Services-unavailable fallback: if Play Integrity setup fails,
 * the manager stays unverified. The HTTP client (GeminiRepository) handles
 * that case by falling back to BackendConfig.devLegacyKey (dev builds only).
 */
object PlayIntegrityManager {
    private const val TAG = "PlayIntegrity"
    private const val PREFS_NAME = "play_integrity_attestation"
    private const val KEY_ID = "keyID"
    private const val KEY_SECRET = "androidAssertionSecret"
    private const val KEY_COUNTER = "counter"
    private const val CLOUD_PROJECT_NUMBER = 657698311127L

    private val _isAttested = MutableStateFlow(false)
    val isAttested: StateFlow<Boolean> = _isAttested.asStateFlow()

    private val mutex = Mutex()
    private var tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null
    @Volatile private var prefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        prefs?.let { return it }
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sp = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        prefs = sp
        return sp
    }

    /**
     * True if a usable (keyID, secret) pair is persisted locally. Returns false
     * if EncryptedSharedPreferences can't be opened (e.g. Robolectric, where
     * AndroidKeyStore is unavailable) — the caller treats that the same as
     * "not yet attested" and falls back to legacy auth in dev.
     */
    fun hasStoredCredentials(context: Context): Boolean = try {
        val p = prefs(context)
        p.contains(KEY_ID) && p.contains(KEY_SECRET)
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences unavailable; treating as no stored credentials", e)
        false
    }

    /**
     * Ensure we have an attested keyID + secret. No-op if already attested
     * (in-memory flag) or if creds are loaded from disk. On failure, leaves
     * [isAttested] false — the caller's HTTP layer should fall back to legacy
     * auth in dev or surface the error in prod.
     */
    suspend fun ensureAttested(context: Context) = mutex.withLock {
        if (_isAttested.value) return@withLock
        if (hasStoredCredentials(context)) {
            _isAttested.value = true
            return@withLock
        }
        try {
            performAttestation(context)
            _isAttested.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Attestation failed; will fall back to legacy auth if available", e)
            _isAttested.value = false
        }
    }

    /**
     * Force re-attestation. Called by the HTTP client when the backend returns
     * 401 android_assertion_invalid (counter desync or secret rotation).
     */
    suspend fun invalidateAndReattest(context: Context) = mutex.withLock {
        prefs(context).edit().clear().apply()
        _isAttested.value = false
        try {
            performAttestation(context)
            _isAttested.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Re-attestation failed", e)
            _isAttested.value = false
            throw e
        }
    }

    /**
     * Build the per-request integrity headers for a Gemini call.
     * Returns null if not attested (caller should fall back or fail).
     *
     * Counter advances atomically — two concurrent callers each get a unique
     * counter value; the mutex serializes the read-modify-write.
     */
    suspend fun assertionHeaders(context: Context, requestBody: ByteArray): Map<String, String>? = mutex.withLock {
        val p = prefs(context)
        val keyID = p.getString(KEY_ID, null) ?: return@withLock null
        val secretHex = p.getString(KEY_SECRET, null) ?: return@withLock null
        val nextCounter = p.getLong(KEY_COUNTER, 0L) + 1
        p.edit().putLong(KEY_COUNTER, nextCounter).apply()

        val bodyHashHex = MessageDigest.getInstance("SHA-256").digest(requestBody).toHex()
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secretHex.hexToByteArray(), "HmacSHA256"))
        }
        val assertionHex = mac.doFinal("$nextCounter:$bodyHashHex".toByteArray()).toHex()

        mapOf(
            "X-Android-Key-Id" to keyID,
            "X-Android-Counter" to nextCounter.toString(),
            "X-Android-Assertion" to assertionHex,
        )
    }

    // ---- private helpers ----

    private suspend fun performAttestation(context: Context) {
        val manager = IntegrityManagerFactory.createStandard(context)

        val prepareRequest = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
            .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
            .build()
        tokenProvider = manager.prepareIntegrityToken(prepareRequest).await()

        val challenge = fetchChallenge()

        val tokenRequest = StandardIntegrityTokenRequest.builder()
            .setRequestHash(challenge)
            .build()
        val tokenResponse = tokenProvider?.request(tokenRequest)?.await()
        val token = tokenResponse?.token() ?: throw Exception("Play Integrity returned null token")

        val keyID = UUID.randomUUID().toString()
        val secret = verifyToken(keyID = keyID, token = token, challenge = challenge)

        prefs(context).edit()
            .putString(KEY_ID, keyID)
            .putString(KEY_SECRET, secret)
            .putLong(KEY_COUNTER, 0L)
            .apply()
    }

    private suspend fun fetchChallenge(): String = withContext(Dispatchers.IO) {
        val url = URL("${BackendConfig.baseURL}/attest/challenge")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("X-App-Platform", "android")
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Failed to fetch challenge: ${connection.responseCode}")
        }
        val responseStr = InputStreamReader(connection.inputStream).readText()
        JSONObject(responseStr).getString("challenge")
    }

    /**
     * POST /attest/verify. Returns the `androidAssertionSecret` from the
     * success response, which the caller persists in EncryptedSharedPreferences.
     */
    private suspend fun verifyToken(keyID: String, token: String, challenge: String): String = withContext(Dispatchers.IO) {
        val url = URL("${BackendConfig.baseURL}/attest/verify")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("X-App-Platform", "android")
        connection.doOutput = true

        val payload = JSONObject().apply {
            put("keyID", keyID)
            put("attestation", token)
            put("challenge", challenge)
        }.toString()
        connection.outputStream.write(payload.toByteArray())

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            val errorBody = connection.errorStream?.let { InputStreamReader(it).readText() }
            throw Exception("Verification failed: ${connection.responseCode} - $errorBody")
        }

        val responseStr = InputStreamReader(connection.inputStream).readText()
        val json = JSONObject(responseStr)
        if (!json.optBoolean("success", false)) {
            throw Exception("Verify response missing success=true")
        }
        json.optString("androidAssertionSecret").takeIf { it.isNotBlank() }
            ?: throw Exception("Verify response missing androidAssertionSecret")
    }

    // ---- hex helpers ----

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0) { "hex string must have even length" }
        return ByteArray(length / 2) { i ->
            ((Character.digit(this[i * 2], 16) shl 4) or Character.digit(this[i * 2 + 1], 16)).toByte()
        }
    }
}
