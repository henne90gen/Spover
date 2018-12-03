package de.spover.spover

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log

typealias SpeedCallback = (Double) -> Unit
typealias LocationCallback = (Location) -> Unit

class LocationService(context: Context, val speedCallback: SpeedCallback?, val locationCallback: LocationCallback?) : LocationListener {
    companion object {
        private val TAG = LocationService::class.java.simpleName
        private const val SPEED_THRESHOLD = 1 / 3.6
    }

    private var lastLocation: Location? = null
    private var lastTime: Long = 0

    private val speedList = ArrayList<Double>()

    init {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        } else {
            Log.e(TAG, "Location permission was not granted")
        }
    }

    override fun onLocationChanged(location: Location) {
        //Log.d(TAG, "Received location update: $location")
        val currentTime = System.currentTimeMillis()
        lastLocation?.let {
            val distance = location.distanceTo(it)
            val timeDiff = currentTime - lastTime
            val timeDiffInSeconds = timeDiff / 1000.0
            var speed = distance / timeDiffInSeconds
            if (speed < SPEED_THRESHOLD) {
                // otherwise speed will sit at 1 km/h when we are not moving
                speed = 0.0
            }
            speed = calculateMovingWeightedAverage(speed)
            if (speed == Double.NaN) {
                speed = 0.0
            }
            speedCallback?.invoke(speed)
        }
        lastLocation = location
        lastTime = currentTime
        locationCallback?.invoke(location)
    }

    private fun calculateMovingWeightedAverage(speed: Double): Double {
        speedList.add(speed)
        var median = 0.0
        var div = 0.0
        for (i in 1..speedList.size) {
            median += speedList[i - 1] * i
            div += i
        }
        median /= div
        while (speedList.size > 10) {
            speedList.removeAt(0)
        }
        return median
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        Log.d(TAG, "Status changed: $provider, $status, $extras")
    }

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Provider disabled: $provider")
    }
}
