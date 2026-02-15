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
        apiKey: String
    ): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        
        // Strategy:
        // 1. If API Key is present, try Remote.
        // 2. If Remote fails or no Key, try Local.
        // 3. Fail if both unavailable.

        if (apiKey.isNotBlank()) {
            val remoteResult = remoteDataSource.analyze(images, apiKey)
            if (remoteResult.isSuccess) {
                return@withContext remoteResult
            }
            // If remote failed, fall through to local (optional strategy, currently falling through)
        }

        if (localDataSource.isAvailable()) {
            return@withContext localDataSource.analyze(images)
        }

        return@withContext Result.failure(Exception("AI Analysis failed. remote: ${if(apiKey.isBlank()) "No Key" else "Error"}, local: Not available"))
    }
}
