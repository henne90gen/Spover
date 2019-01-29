package de.spover.spover

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

typealias SpeedCallback = (Double) -> Unit
typealias SpeedUnavailable = () -> Unit
typealias LocationCallback = (Location) -> Unit
typealias TimeStamp = Long
typealias Speed = Double

class LocationService(var context: Context,
                      private val speedCallback: SpeedCallback?,
                      private val speedUnavailableCallback: SpeedUnavailable?,
                      private val locationCallback: LocationCallback?) : LocationListener {
    companion object {
        private val TAG = LocationService::class.java.simpleName
        private const val SPEED_THRESHOLD = 1 / 3.6
    }

    private var lastLocation: Location? = null
    private var lastLocationUpdateTime: Long = 0

    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val speedHistory = ArrayList<Pair<TimeStamp, Speed>>()

    init {
        if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        } else {
            Log.e(TAG, "Location permission was not granted")
        }
    }


    override fun onLocationChanged(location: Location) {
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "Received location update: $location")
        lastLocation?.let {
            val distance = location.distanceTo(it)
            val timeDiff = currentTime - lastLocationUpdateTime
            val timeDiffInSeconds = timeDiff / 1000.0
            var speed = distance / timeDiffInSeconds
            if (speed < SPEED_THRESHOLD) {
                // otherwise speed will sit at 1 km/h when we are not moving
                speed = 0.0
            }
            speed = calculateMovingWeightedAverage(speed)
            if (speed.isNaN()) {
                speed = 0.0
            }
            speedCallback?.invoke(speed)
        }

        lastLocation = location
        lastLocationUpdateTime = currentTime
        locationCallback?.invoke(location)
    }

    fun unregisterLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    private fun calculateMovingWeightedAverage(speed: Double): Double {
        speedHistory.add(Pair(System.currentTimeMillis(), speed))
        var median = 0.0
        var div = 0.0
        for (entry in speedHistory) {
            val weight = 1.0f / (System.currentTimeMillis() - entry.first + 1000)
            median += weight * entry.second
            div += weight
        }
        median /= div
        while (speedHistory.size > 5) {
            speedHistory.removeAt(0)
        }
        return median
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
            speedUnavailableCallback?.invoke()
        }
    }

    override fun onProviderEnabled(provider: String) {
        // do nothing
    }

    override fun onProviderDisabled(provider: String) {
        speedUnavailableCallback?.invoke()
    }
}
