package de.spover.spover

import android.content.Context
import android.location.Location
import android.util.Log
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings

enum class SpeedMode {
    GREEN,
    YELLOW,
    RED
}

typealias SpeedLimitChangedCallback = (Int) -> Unit
typealias SpeedModeChangeCallback = () -> Unit

class SpeedLimitService(val context: Context, val speedLimitCallback: SpeedLimitChangedCallback, val speedModeCallback: SpeedModeChangeCallback) {

    companion object {
        private var TAG = SpeedLimitService::class.java.simpleName
    }

    private var settingsStore: SettingsStore = SettingsStore(context)

    private var currentLocation: Location? = null
    private var currentSpeedLimit: Int = 0 // current speed limit in kph
    var speedMode: SpeedMode = SpeedMode.GREEN

    fun updateCurrentLocation(location: Location) {
        if (currentLocation == null) {
            currentLocation = location
            return
        }

        if (location.distanceTo(currentLocation) < 10) {
            // ToDo uncomment after debugging
            // return
        }
        currentLocation = location
        currentSpeedLimit = 5

        // Todo if no bounding box exists create one
        // Todo if bounding box is coming close to end create new one in background

        // Todo if valid bounding box exists calculate which way we are on
        // Todo get max speed for that way and return it via the speedLimitCallback

        speedLimitCallback(currentSpeedLimit)

    }

    fun updateSpeedMode(currentSpeed: Int) {
        val threshold = settingsStore.get(SpoverSettings.SPEED_THRESHOLD)

        speedMode = when {
            currentSpeed <= currentSpeedLimit + (threshold/2) -> SpeedMode.GREEN
            currentSpeed < currentSpeedLimit + threshold -> SpeedMode.YELLOW
            currentSpeed > currentSpeedLimit + threshold -> SpeedMode.RED
            else -> {
                Log.e(TAG, "Unknown speed mode for speed: $currentSpeed and speed limit: $currentSpeedLimit")
                SpeedMode.GREEN
            }
        }

        Log.d(TAG, "mode $speedMode, curr $currentSpeed limit $currentSpeedLimit")

        speedModeCallback()
    }
}