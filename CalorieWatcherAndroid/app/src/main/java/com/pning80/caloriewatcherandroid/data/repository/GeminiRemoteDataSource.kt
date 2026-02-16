package com.pning80.caloriewatcherandroid.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.pning80.caloriewatcherandroid.data.model.FoodAnalysisResult
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GeminiRemoteDataSource @Inject constructor() {

    suspend fun analyze(images: List<Bitmap>, apiKey: String, modelName: String): Result<FoodAnalysisResult> {
        val generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )

        val prompt = """
            Analyze these food images. Identify the food items, estimate calories, quantity, and macros (protein, carbs, fat).
            Return strict JSON with the following structure:
            {
              "items": [
                {
                  "name": "...",
                  "calories": 0.0,
                  "quantity": "...",
                  "protein": 0.0,
                  "carbs": 0.0,
                  "fat": 0.0,
                  "imageIndex": 0
                }
              ]
            }
            "imageIndex" must be the 0-based index of the image in the provided list that contains this item.
            Do not include markdown formatting like ```json.
        """.trimIndent()

        return try {
            val input = content {
                images.forEach { image(it) }
                text(prompt)
            }

            val response = generativeModel.generateContent(input)
            val responseText = response.text?.trim() ?: throw Exception("Empty response from AI")
            
            val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
            val result = Json { ignoreUnknownKeys = true }.decodeFromString<FoodAnalysisResult>(cleanJson)
            Result.success(result)
        } catch (e: Exception) {
            Log.e("GeminiRemoteDataSource", "Error analyzing images: ${e.javaClass.name}: ${e.localizedMessage}", e)
            val errorMessage = "An unexpected error occurred during AI analysis. Please check your API key and network connection. Model: $modelName. Details: ${e.javaClass.simpleName}"
            Result.failure(Exception(errorMessage, e))
        }
    }
}
