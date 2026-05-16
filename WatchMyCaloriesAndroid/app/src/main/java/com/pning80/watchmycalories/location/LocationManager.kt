package com.pning80.watchmycalories.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class LocationData(
    val location: Location?,
    val locality: String?
)

class LocationManager(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    suspend fun getCurrentLocation(): LocationData {
        if (!hasLocationPermissions()) {
            return LocationData(null, null)
        }

        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await()

            if (location != null) {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val locality = addresses?.firstOrNull()?.locality
                LocationData(location, locality)
            } else {
                LocationData(null, null)
            }
        } catch (e: Exception) {
            LocationData(null, null)
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocationPermission || coarseLocationPermission
    }
}
