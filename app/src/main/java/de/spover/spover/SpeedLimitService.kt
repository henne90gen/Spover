package de.spover.spover

import android.content.Context
import android.location.Location

typealias SpeedLimitChangedCallback = (Double) -> Unit

class SpeedLimitService(context: Context, val callback: SpeedLimitChangedCallback) {

    private val locationService: LocationService
    private var currentLocation: Location? = null

    init {
        locationService = LocationService(context, null, this::saveCurrentLocation)
    }

    private fun saveCurrentLocation(location: Location) {
        if (currentLocation == null) {
            return
        }
        if (location.distanceTo(currentLocation) < 20) {
            return
        }
        currentLocation = location
        // check whether speed limit has changed
        callback(123.0)
    }
}