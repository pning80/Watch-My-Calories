package com.pning80.watchmycalories.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import java.util.UUID

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

data class MenuAnalysisResult(
    val error: String? = null,
    val restaurantName: String? = null,
    val items: List<com.pning80.watchmycalories.data.MenuItemResult>? = null
)

class GeminiRepository(private val apiKey: String) {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    private val gson = Gson()

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts) {
                    val delayMs = (1L shl (attempt - 1)) * 1000L
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }
        throw lastException ?: Exception("Unknown error during retry")
    }

    suspend fun estimateCalories(
        images: List<Bitmap>,
        isMetric: Boolean = true
    ): Result<EstimationResult> = withContext(Dispatchers.IO) {
        try {
            val unitInstruction = if (isMetric) {
                "Prefer metric units for quantities (g, kg, ml, L, pieces, slices) when possible."
            } else {
                "Prefer US customary units for quantities. Use oz for weight, fl oz for liquid volume, and cups/tbsp/tsp for other volumes. Examples: 6 oz, 8 fl oz, 1 cup, 2 pieces."
            }
            val quantityExample = if (isMetric) "200 g, 250 ml, 2 pieces" else "1 cup, 6 oz, 2 pieces"

            val prompt = """
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

            val result = withRetry {
                val response = generativeModel.generateContent(
                    content {
                        images.forEach { image(it) }
                        text(prompt)
                    }
                )

                val cleanText = GeminiParser.cleanMarkdownJson(response.text)
                    ?: throw Exception("Empty response")
                    
                GeminiParser.parseGeminiResponse(cleanText)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeMenu(
        image: Bitmap,
        locality: String? = null,
        coordinates: String? = null,
        isMetric: Boolean = true
    ): Result<MenuAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val unitInstruction = if (isMetric) {
                "Prefer metric units for quantities (g, kg, ml, L, pieces, slices) when possible."
            } else {
                "Prefer US customary units for quantities. Use oz for weight, fl oz for liquid volume, and cups/tbsp/tsp for other volumes."
            }

            val prompt = """
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

            val result = withRetry {
                val response = generativeModel.generateContent(
                    content {
                        image(image)
                        text(prompt)
                    }
                )

                val cleanText = GeminiParser.cleanMarkdownJson(response.text)
                    ?: throw Exception("Empty response")

                val res = gson.fromJson(cleanText, MenuAnalysisResult::class.java)
                if (res.error == "not_a_menu") {
                    throw Exception("not_a_menu")
                }
                
                // Clamp menu items
                val clamedItems = res.items?.map { item ->
                    item.copy(
                        calories = maxOf(0.0, item.calories),
                        protein = item.protein?.let { maxOf(0.0, it) },
                        carbs = item.carbs?.let { maxOf(0.0, it) },
                        fat = item.fat?.let { maxOf(0.0, it) }
                    )
                }
                res.copy(items = clamedItems)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
