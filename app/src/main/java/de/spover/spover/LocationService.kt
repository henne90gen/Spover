package de.spover.spover

import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class LocationService(context: Context) : ILocationService {
    companion object {
        private val TAG = LocationService::class.java.simpleName
    }

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    override fun fetchLocation(callback: LocationCallback) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                location?.let {
                    callback(convert(it))
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not fetch location!")
        }
    }

    override fun registerLocationCallback(callback: LocationCallback) {
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    callback(convert(location))
                }
            }
        }

        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 1
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null /* Looper */)
        } catch (e: SecurityException) {
            Log.e(TAG, "Could not register location callback!")
        }
    }

    private fun convert(location: android.location.Location): Location {
        return Location(location.latitude, location.longitude, location.speed.toDouble())
    }
}
