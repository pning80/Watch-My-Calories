package com.pning80.caloriewatcherandroid.data.repository

import android.graphics.Bitmap
import com.pning80.caloriewatcherandroid.data.model.FoodAnalysisResult
import javax.inject.Inject

class GeminiLocalDataSource @Inject constructor(){

    // TODO: Implement local model
    fun isAvailable(): Boolean {
        return false
    }

    suspend fun analyze(images: List<Bitmap>): Result<FoodAnalysisResult> {
        return Result.failure(NotImplementedError("Local model not implemented yet"))
    }
}
