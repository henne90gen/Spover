package de.spover.spover

import android.app.Activity
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationService(activity: Activity) : ILocationService {

    private val tag = LocationService::class.java.simpleName

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

    override fun fetchLocation(callback: LocationCallback) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                callback(convert(location))
            }
        } catch (e: SecurityException) {
            Log.w(tag, "Could not fetch location!")
        }
    }

    private fun convert(location: android.location.Location?): Location? {
        return if (location != null) {
            Location(location.latitude, location.longitude)
        } else {
            null
        }
    }
}
