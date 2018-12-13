package de.spover.spover

import android.content.Context
import android.location.Location
import android.util.Log
import de.spover.spover.database.AppDatabase
import de.spover.spover.network.BoundingBox
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings
import android.os.AsyncTask
import de.spover.spover.database.Node
import de.spover.spover.database.Request
import de.spover.spover.database.Way


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


    fun loadSpeedData(boundingBox: BoundingBox) {
        val response = ReadFromDBAsyncTask(this, boundingBox).execute()
        Log.d(TAG, "should have loaded")
    }

    fun onSpeedDataLoaded(speedData: Request?, ways: List<Way>, nodes: List<Node>) {
        Log.d(TAG, "my speed data: $speedData")
        Log.d(TAG, "ways: ${ways.size} nodes: ${nodes.size}")
        // ToDo replace old data by new one

    }

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

    /** if a request with the given bounding box exists return all ways and nodes corresponding
     * to that request */
    private class ReadFromDBAsyncTask(var speedLimitService: SpeedLimitService, var boundingBox: BoundingBox) : AsyncTask<Void, Void, String>() {
        val db = AppDatabase.createBuilder(speedLimitService.context).build()
        var request: Request? = null
        var ways: List<Way> = listOf()
        var nodes: MutableList<Node> = mutableListOf()

        private var TAG = ReadFromDBAsyncTask::class.java.simpleName

        override fun doInBackground(vararg params: Void?): String? {
            request = db.requestDao().findRequestByBoundingBox(boundingBox.minLon, boundingBox.minLat, boundingBox.maxLon, boundingBox.maxLat)
            if (request == null) {
                Log.e(TAG, "Fetching data for given bounding box failed, request not found!")
                return null
            }
            ways = db.wayDao().findWaysByRequestId(request!!.id!!)

            for (way: Way in ways) {
                nodes.addAll(db.nodeDao().findNodesByWayId(way.id!!))
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            speedLimitService.onSpeedDataLoaded(request, ways, nodes)
        }
    }
}