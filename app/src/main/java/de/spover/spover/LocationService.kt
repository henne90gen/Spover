package de.spover.spover

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import android.util.Log

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
        private val TIMEOUT = 10000
    }

    private var lastLocation: Location? = null
    private var lastLocUpdateTime: Long = 0

    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val speedHistory = ArrayList<Pair<TimeStamp, Speed>>()

    // time in milliseconds with no location update before the current speed and speed limit gets removed
    private var timeout = TIMEOUT

    init {
        if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        } else {
            Log.e(TAG, "Location permission was not granted")
        }
        setupNoLocationTimeoutHandler()
    }

    private fun setupNoLocationTimeoutHandler() {
        val handler = Handler()
        val delay = 1000L //milliseconds
        handler.postDelayed(object : Runnable {
            override fun run() {
                timeout -= delay.toInt()
                if (timeout <= 0) {
                    speedUnavailableCallback?.invoke()
                }
                handler.postDelayed(this, delay)
            }
        }, delay)
    }

    private fun resetTimeout() {
        timeout = TIMEOUT
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Received location update: $location")
        val currentTime = System.currentTimeMillis()
        lastLocation?.let {
            val distance = location.distanceTo(it)
            val timeDiff = currentTime - lastLocUpdateTime
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
        lastLocUpdateTime = currentTime
        resetTimeout()
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
        //Log.d(TAG, "Status changed: $provider, $status, $extras")
    }

    override fun onProviderEnabled(provider: String) {
        //Log.d(TAG, "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        //Log.d(TAG, "Provider disabled: $provider")
    }
}
