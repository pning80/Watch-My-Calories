package com.pning80.caloriewatcherandroid.data.repository

import android.graphics.Bitmap
import com.pning80.caloriewatcherandroid.data.model.FoodAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GeminiRepository @Inject constructor(
    private val remoteDataSource: GeminiRemoteDataSource,
    private val localDataSource: GeminiLocalDataSource
) {

    suspend fun estimateCalories(
        images: List<Bitmap>,
        apiKey: String,
        modelName: String
    ): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        
        var remoteError: Throwable? = null

        if (apiKey.isNotBlank()) {
            val remoteResult = remoteDataSource.analyze(images, apiKey, modelName)
            if (remoteResult.isSuccess) {
                return@withContext remoteResult
            } else {
                remoteError = remoteResult.exceptionOrNull()
            }
        }

        if (localDataSource.isAvailable()) {
            val localResult = localDataSource.analyze(images)
            if (localResult.isSuccess) {
                return@withContext localResult
            }
        }

        val errorMessage = buildString {
            append("AI Analysis failed. ")
            if (apiKey.isBlank()) {
                append("Remote: No API Key provided. ")
            } else {
                append("Remote Error: ${remoteError?.message ?: "Unknown"}. ")
            }
            append("Local: Not available.")
        }

        return@withContext Result.failure(Exception(errorMessage))
    }
}
