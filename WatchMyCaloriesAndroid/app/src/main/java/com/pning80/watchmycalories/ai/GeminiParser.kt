package com.pning80.watchmycalories.ai

import com.google.gson.Gson

object GeminiParser {
    private val gson = Gson()

    fun cleanMarkdownJson(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        return rawText.replace("```json", "")
            .replace("```", "")
            .trim()
    }

    fun parseGeminiResponse(cleanText: String): EstimationResult {
        val raw = gson.fromJson(cleanText, EstimationResult::class.java)
        val clampedItems = raw.items.map { item ->
            item.copy(
                calories = maxOf(0.0, item.calories),
                protein = item.protein?.let { maxOf(0.0, it) },
                carbs = item.carbs?.let { maxOf(0.0, it) },
                fat = item.fat?.let { maxOf(0.0, it) }
            )
        }
        return raw.copy(items = clampedItems)
    }
}
