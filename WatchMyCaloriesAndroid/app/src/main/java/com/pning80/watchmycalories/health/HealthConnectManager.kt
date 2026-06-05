package com.pning80.watchmycalories.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    private val _activeEnergyBurned = MutableStateFlow(0.0)
    val activeEnergyBurned: StateFlow<Double> = _activeEnergyBurned.asStateFlow()

    /**
     * Inject a fixed active-energy value for UI-testing / parity captures, mirroring
     * iOS's simulator HealthKit mock (`HealthKitManager.swift:17` sets
     * `activeEnergyBurned = 456` on the sim). Real launches read Health Connect;
     * `fetchTodayEnergyBurned()` is a no-op while unauthorized (the test case), so
     * this value persists. Called from MainActivity only when the launch Intent
     * carries `wmc.test.uitesting`.
     */
    fun setActiveEnergyBurnedForUiTesting(value: Double) {
        _activeEnergyBurned.value = value
    }

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    suspend fun checkPermissions() {
        try {
            val permissions = setOf(
                androidx.health.connect.client.permission.HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
            )
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            _isAuthorized.value = granted.containsAll(permissions)
            
            if (_isAuthorized.value) {
                fetchTodayEnergyBurned()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchTodayEnergyBurned() {
        if (!_isAuthorized.value) return
        
        try {
            val startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toInstant()
            val now = Instant.now()
            
            val request = ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
            
            val response = healthConnectClient.readRecords(request)
            var totalKcal = 0.0
            for (record in response.records) {
                totalKcal += record.energy.inKilocalories
            }
            
            _activeEnergyBurned.value = totalKcal
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
