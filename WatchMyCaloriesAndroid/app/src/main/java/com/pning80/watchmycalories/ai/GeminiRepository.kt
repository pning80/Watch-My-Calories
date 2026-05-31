package com.pning80.watchmycalories.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.pning80.watchmycalories.data.MenuItemResult
import com.pning80.watchmycalories.security.BackendConfig
import com.pning80.watchmycalories.security.JpegConfig
import com.pning80.watchmycalories.security.PlayIntegrityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class EstimationItem(
    val name: String,
    val quantity: String,
    val calories: Double,
    val confidence: Double,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null
)

data class EstimationResult(
    val mealName: String? = null,
    val items: List<EstimationItem>
)

val EstimationResult.totalCalories: Double
    get() = items.sumOf { it.calories }

val EstimationResult.totalProtein: Double
    get() = items.sumOf { it.protein ?: 0.0 }

val EstimationResult.totalCarbs: Double
    get() = items.sumOf { it.carbs ?: 0.0 }

val EstimationResult.totalFat: Double
    get() = items.sumOf { it.fat ?: 0.0 }


data class MenuAnalysisResult(
    val error: String? = null,
    val restaurantName: String? = null,
    val items: List<MenuItemResult>? = null
)

/**
 * Gemini orchestrator (PORTING_CRITERIA.md T1.5).
 *
 * Mirrors iOS `Services.swift`: bundles base64 image(s) + prompt into the
 * `contents.parts[]` shape Gemini expects, POSTs to the Cloud Run backend.
 * The client does not hold a Gemini API key — the backend forwards to Google.
 *
 * Authentication:
 *  - Production (release builds): Play Integrity per-request headers added by
 *    `PlayIntegrityManager` (wired in a follow-up — see PORTING_RUNBOOK.md
 *    Stage 2 step 3).
 *  - Dev (debug builds): falls back to legacy `x-backend-key` sourced from
 *    `BackendConfig.devLegacyKey` (local.properties → BuildConfig), matching
 *    the iOS App Attest simulator fallback.
 */
open class GeminiRepository(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private suspend fun <T> withRetry(maxAttempts: Int = 3, block: suspend () -> T): T {
        var lastException: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts) {
                    val delayMs = (1L shl (attempt - 1)) * 1000L
                    delay(delayMs)
                }
            }
        }
        throw lastException ?: Exception("Unknown error during retry")
    }

    open suspend fun estimateCalories(
        images: List<Bitmap>,
        isMetric: Boolean = true
    ): Result<EstimationResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildFoodPrompt(isMetric)
            val responseText = withRetry { callBackend(prompt, images) }
            val cleanText = GeminiParser.cleanMarkdownJson(responseText)
                ?: throw Exception("Empty response")
            Result.success(GeminiParser.parseGeminiResponse(cleanText))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun analyzeMenu(
        image: Bitmap,
        locality: String? = null,
        coordinates: String? = null,
        isMetric: Boolean = true
    ): Result<MenuAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildMenuPrompt(isMetric, locality, coordinates)
            val responseText = withRetry { callBackend(prompt, listOf(image)) }
            val cleanText = GeminiParser.cleanMarkdownJson(responseText)
                ?: throw Exception("Empty response")
            val parsed = gson.fromJson(cleanText, MenuAnalysisResult::class.java)
            if (parsed.error == "not_a_menu") throw Exception("not_a_menu")
            val clampedItems = parsed.items?.map { item ->
                item.copy(
                    calories = maxOf(0.0, item.calories),
                    protein = item.protein?.let { maxOf(0.0, it) },
                    carbs = item.carbs?.let { maxOf(0.0, it) },
                    fat = item.fat?.let { maxOf(0.0, it) }
                )
            }
            Result.success(parsed.copy(items = clampedItems))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- HTTP plumbing ----

    private fun callBackend(prompt: String, images: List<Bitmap>): String {
        val partsArray = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            images.forEach { bitmap ->
                val bytes = ByteArrayOutputStream().also {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JpegConfig.QUALITY, it)
                }.toByteArray()
                put(JSONObject().put("inline_data", JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                ))
            }
        }
        val bodyBytes = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
            .toString()
            .toByteArray()

        // First attempt: if attested, use Play Integrity per-request HMAC. Else
        // fall back to legacy x-backend-key (dev only). On 401 from the Android
        // path, re-attest once and retry.
        return runBlocking {
            executeWithAttestation(bodyBytes, allowReattest = true)
        }
    }

    /**
     * POST the Gemini body to the backend. If [PlayIntegrityManager] has live
     * credentials, sign the body with X-Android-* per the T1.8 protocol. On 401
     * with `android_assertion_invalid` and [allowReattest] true, invalidate the
     * cached credentials, re-attest, and retry once.
     */
    private suspend fun executeWithAttestation(bodyBytes: ByteArray, allowReattest: Boolean): String {
        val builder = Request.Builder()
            .url("${BackendConfig.baseURL}/v1beta/models/default:generateContent")
            .header("Content-Type", "application/json")
            .header("X-App-Platform", "android")
            .post(bodyBytes.toRequestBody("application/json".toMediaType()))

        val attestHeaders = PlayIntegrityManager.assertionHeaders(context, bodyBytes)
        if (attestHeaders != null) {
            attestHeaders.forEach { (k, v) -> builder.header(k, v) }
        } else {
            // No Play Integrity creds — try the dev legacy key (dev builds only).
            BackendConfig.devLegacyKey?.let { builder.header("x-backend-key", it) }
        }

        return withContext(Dispatchers.IO) {
            client.newCall(builder.build()).execute().use { response ->
                if (response.code == 429) {
                    val retryAfter = response.header("Retry-After")?.toIntOrNull()
                    throw Exception(
                        if (retryAfter != null) "Rate limited. Try again in $retryAfter seconds."
                        else "Rate limited. Please try again shortly."
                    )
                }
                if (response.code == 401) {
                    val raw = response.body?.string()
                    // T1.8: a 401 with `android_assertion_invalid` means our counter is
                    // out of sync or the server-side secret has rotated. Re-attest and
                    // retry once. Any other 401 is genuine auth failure.
                    if (allowReattest && attestHeaders != null && raw?.contains("android_assertion_invalid") == true) {
                        PlayIntegrityManager.invalidateAndReattest(context)
                        return@use executeWithAttestation(bodyBytes, allowReattest = false)
                    }
                    throw Exception("Unauthorized (401): ${raw ?: "no body"}")
                }
                if (!response.isSuccessful) {
                    throw Exception("Backend error ${response.code}: ${response.message}")
                }
                val raw = response.body?.string() ?: throw Exception("Empty body")
                val json = JSONObject(raw)
                val candidates = json.optJSONArray("candidates")
                    ?: throw Exception("Missing candidates in response")
                if (candidates.length() == 0) throw Exception("No candidates in response")
                val parts = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                if (parts.length() == 0) throw Exception("No parts in response")
                parts.getJSONObject(0).getString("text")
            }
        }
    }

    private fun buildFoodPrompt(isMetric: Boolean): String {
        val unitInstruction = if (isMetric) {
            "Prefer metric units for quantities (g, kg, ml, L, pieces, slices) when possible."
        } else {
            "Prefer US customary units for quantities. Use oz for weight, fl oz for liquid volume, and cups/tbsp/tsp for other volumes. Examples: 6 oz, 8 fl oz, 1 cup, 2 pieces."
        }
        val quantityExample = if (isMetric) "200 g, 250 ml, 2 pieces" else "1 cup, 6 oz, 2 pieces"
        return """
            Analyze these food images. Identify the food items, estimate the portion size, and calculate the calories.
            $unitInstruction
            Return ONLY a raw JSON object (no markdown, no code blocks) with this structure:
            {
              "items": [
                {
                  "name": "Food Name",
                  "quantity": "Estimated Quantity (e.g. $quantityExample)",
                  "calories": 150,
                  "protein": 10,
                  "carbs": 20,
                  "fat": 5,
                  "confidence": 0.95
                }
              ]
            }
        """.trimIndent()
    }

    private fun buildMenuPrompt(isMetric: Boolean, locality: String?, coordinates: String?): String {
        val unitInstruction = if (isMetric) {
            "Prefer metric units for quantities (g, kg, ml, L, pieces, slices) when possible."
        } else {
            "Prefer US customary units for quantities. Use oz for weight, fl oz for liquid volume, and cups/tbsp/tsp for other volumes."
        }
        return """
            Analyze this photo of a restaurant menu. Identify the dishes listed and estimate the calorie content for each based on typical serving sizes.
            $unitInstruction

            Context:
            Locality: ${locality ?: "Unknown"}
            Coordinates: ${coordinates ?: "Unknown"}

            A restaurant menu includes printed menus, chalkboard specials, digital menu displays, drink lists, and similar documents listing food or drink items offered by a food service establishment. Receipts, grocery lists, nutrition labels, and non-food documents are NOT menus.

            If the image does NOT appear to be a restaurant menu, respond with ONLY:
            {"error": "not_a_menu"}

            Otherwise, return ONLY a raw JSON object (no markdown, no code blocks):
            {
              "restaurantName": "Name if visible or identifiable, otherwise null",
              "items": [
                {
                  "name": "Dish Name",
                  "description": "Brief description if visible on menu",
                  "calories": 500,
                  "protein": 30,
                  "carbs": 50,
                  "fat": 15
                }
              ]
            }
        """.trimIndent()
    }

}
