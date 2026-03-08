package com.pning80.watchmycalories.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun getTodayActiveCalories(): Double = withContext(Dispatchers.IO) {
        if (!isHealthConnectAvailable()) return@withContext 0.0

        // Define start and end time for today
        val endTime = Instant.now()
        val startTime = endTime.truncatedTo(ChronoUnit.DAYS)

        try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            // Return total calories or 0.0 if null
             response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
        } catch (e: Exception) {
            // Handle exceptions (permissions, etc.)
            0.0
        }
    }
    
    fun isHealthConnectAvailable(): Boolean {
        // Simple check, specific logic depends on SDK version
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE 
    }
}
