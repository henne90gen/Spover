package de.spover.spover

import android.content.Context
import android.location.Location
import android.util.Log

typealias SpeedLimitChangedCallback = (Int) -> Unit

class SpeedLimitService(context: Context, val callback: SpeedLimitChangedCallback) {

    companion object {
        private var TAG = SpeedLimitService::class.java.simpleName
    }

    private val locationService: LocationService
    private var currentLocation: Location? = null

    init {
        locationService = LocationService(context, null, this::updateCurrentLocation)
    }

    private fun updateCurrentLocation(location: Location) {
        if (currentLocation == null) {
            currentLocation = location
            return
        }
        Log.d(TAG, "distance ${location.distanceTo(currentLocation)}")

        if (location.distanceTo(currentLocation) < 20) {
            // ToDo uncomment after debugging
            // return
        }
        currentLocation = location

        // Todo if no bounding box exists create one
        // Todo if bounding box is coming close to end create new one in background

        // Todo if valid bounding box exists calculate which way we are on
        // Todo get max speed for that way and return it via the callback

        callback(100)
    }
}