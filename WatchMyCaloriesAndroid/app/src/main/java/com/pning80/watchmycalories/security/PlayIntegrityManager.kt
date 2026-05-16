package com.pning80.watchmycalories.security

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object PlayIntegrityManager {
    private const val TAG = "PlayIntegrity"

    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()
    
    // Fallback ID to pass instead of KeyID from Apple App Attest
    private val installationId = UUID.randomUUID().toString()

    private var tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    suspend fun ensureAttested(context: Context) {
        if (_isVerified.value) return

        try {
            val manager = IntegrityManagerFactory.createStandard(context)
            
            // Cloud project number is usually embedded in strings or inferred if using standard
            // We use the basic prepare request. Usually needs cloudProjectNumber but basic setup might infer it
            // if linked directly via Play Console.
            val prepareRequest = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                // .setCloudProjectNumber(123456789) // Assume inferred for now or placeholder
                .build()

            tokenProvider = manager.prepareIntegrityToken(prepareRequest).await()

            // 1. Fetch backend challenge
            val challenge = fetchChallenge()
            
            // 2. Request Integrity Token from Play Services using the challenge
            val tokenRequest = StandardIntegrityTokenRequest.builder()
                .setRequestHash(challenge)
                .build()

            val tokenResponse = tokenProvider?.request(tokenRequest)?.await()
            val token = tokenResponse?.token() ?: throw Exception("Null token")

            // 3. Verify with backend
            verifyToken(token, challenge)
            
            _isVerified.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Attestation failed", e)
            _isVerified.value = false
        }
    }

    private suspend fun fetchChallenge(): String = withContext(Dispatchers.IO) {
        val url = URL("${BackendConfig.baseURL}/attest/challenge")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Failed to fetch challenge: ${connection.responseCode}")
        }
        
        val responseStr = InputStreamReader(connection.inputStream).readText()
        val json = JSONObject(responseStr)
        json.getString("challenge")
    }

    private suspend fun verifyToken(token: String, challenge: String) = withContext(Dispatchers.IO) {
        val url = URL("${BackendConfig.baseURL}/attest/verify")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val payload = JSONObject().apply {
            put("keyID", installationId)
            put("attestation", token)
            put("challenge", challenge)
        }.toString()

        connection.outputStream.write(payload.toByteArray())

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            val errorResponse = connection.errorStream?.let { InputStreamReader(it).readText() }
            throw Exception("Verification failed: ${connection.responseCode} - $errorResponse")
        }
    }
}
