package de.spover.spover

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.nfc.Tag
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.util.Log
import de.spover.spover.network.BoundingBox
import de.spover.spover.network.OpenStreetMapsClient
import kotlin.math.cos

typealias SpeedCallback = (Double) -> Unit
typealias LocationCallback = (Location) -> Unit
typealias BoundingBoxCallback = (BoundingBox) -> Unit

class LocationService(var context: Context, val speedCallback: SpeedCallback?, val locationCallback: LocationCallback?, val bBoxCallback: BoundingBoxCallback?) : LocationListener {
    companion object {
        private val TAG = LocationService::class.java.simpleName
        private const val SPEED_THRESHOLD = 1 / 3.6
    }

    private var lastLocation: Location? = null
    private var lastTime: Long = 0

    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val speedList = ArrayList<Double>()

    var boundingBox: BoundingBox? = null
    private var newBoundingBoxDistFromLocation = 1000
    private var minBoundingBoxDistFromEdge = 200

    init {
        if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        } else {
            Log.e(TAG, "Location permission was not granted")
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Received location update: $location")
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
            if (speed.isNaN()) {
                speed = 0.0
            }
            speedCallback?.invoke(speed)
        }

        if (boundingBox != null) {
            Log.d(TAG, "Bounding box valid ${boundingBox!!.isBoundingBoxValid(location, minBoundingBoxDistFromEdge)}")
            Log.d(TAG, "bb is $boundingBox and location is ${location.latitude}, ${location.longitude}")
        }

        if (boundingBox == null || !boundingBox!!.isBoundingBoxValid(location, minBoundingBoxDistFromEdge)) {
            val newBoundingBox = calcBoundingBox(location, newBoundingBoxDistFromLocation )
            fetchNewData(newBoundingBox)
            Log.d(TAG, "is new bb $newBoundingBox valid: ${newBoundingBox.isBoundingBoxValid(location, minBoundingBoxDistFromEdge)}")
            boundingBox = newBoundingBox
            bBoxCallback?.invoke(newBoundingBox)
        }

        lastLocation = location
        lastTime = currentTime
        locationCallback?.invoke(location)
    }

    /**
     * returns a bounding box with each edge being 'distance' meters away from the given location
     */
    private fun calcBoundingBox(location: Location, distance: Int): BoundingBox {
        val minLoc = translateLocationByMeters(location, -distance, -distance)
        val maxLoc = translateLocationByMeters(location, distance, distance)
        return BoundingBox(minLoc.latitude, minLoc.longitude, maxLoc.latitude, maxLoc.longitude)
    }

    // https://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
    private fun translateLocationByMeters(location: Location, transX: Int, transY: Int): Location {
        val tmpLocation = Location("")
        tmpLocation.latitude = location.latitude + (transY / 111111.0f)
        tmpLocation.longitude = location.longitude - (transX / (111111.0f * cos(location.latitude)))
        return tmpLocation
    }

    private fun fetchNewData(boundingBox: BoundingBox) {
        Log.d(TAG, "started new request for $boundingBox")
        OpenStreetMapsClient.scheduleBoundingBoxFetching(
                context,
                boundingBox
        )
    }

    fun unregisterLocationUpdates() {
        locationManager.removeUpdates(this)
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
        //Log.d(TAG, "Status changed: $provider, $status, $extras")
    }

    override fun onProviderEnabled(provider: String) {
        //Log.d(TAG, "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        //Log.d(TAG, "Provider disabled: $provider")
    }
}
