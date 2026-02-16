package com.pning80.caloriewatcherandroid.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodAnalysisResult(
    val items: List<IdentifiedFoodItem>
)

@Serializable
data class IdentifiedFoodItem(
    val name: String,
    val calories: Double,
    val quantity: String,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val imageIndex: Int = 0
)
