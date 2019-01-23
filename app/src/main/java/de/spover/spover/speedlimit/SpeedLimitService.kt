package de.spover.spover.speedlimit

import android.content.Context
import android.location.Location
import android.util.Log
import de.spover.spover.database.AppDatabase
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings
import android.os.AsyncTask
import de.spover.spover.BoundingBox
import de.spover.spover.database.Node
import de.spover.spover.database.Request
import de.spover.spover.database.Way

enum class SpeedMode {
    GREEN,
    YELLOW,
    RED
}


typealias SpeedLimitChangedCallback = (String) -> Unit
typealias SpeedModeChangeCallback = () -> Unit

class SpeedLimitService(val context: Context, val speedLimitCallback: SpeedLimitChangedCallback, val speedModeCallback: SpeedModeChangeCallback) {

    companion object {
        private var TAG = SpeedLimitService::class.java.simpleName
    }

    private var settingsStore: SettingsStore = SettingsStore(context)

    private var currentLocation: Location? = null
    private var lastLocation: Location? = null

    private var currentSpeedLimit: Pair<Int, String> = Pair(Int.MAX_VALUE, "unknown") // current speed limit as number (in kph) and textual description
    var speedMode: SpeedMode = SpeedMode.GREEN
    // linked since we want to preserve the node order
    private var wayMap: LinkedHashMap<Way, List<Node>> = linkedMapOf()

    private var boundingBox: BoundingBox = BoundingBox(0.0, 0.0, 0.0, 0.0)
    private var isDataUpToDate: Boolean = true

    fun updateBoundingBox(newBoundingBox: BoundingBox) {
        boundingBox = newBoundingBox
        isDataUpToDate = false
    }

    fun loadSpeedData(boundingBox: BoundingBox) {
        ReadFromDBAsyncTask(this, boundingBox).execute()
    }

    /**
     * callback for async reading task, updates the wayMap contents if the read data
     * corresponds to the current bounding box
     */
    fun onSpeedDataLoaded(request: Request, wayMap: LinkedHashMap<Way, List<Node>>) {
        val bbEqual = boundingBox.compareTo(BoundingBox(request.minLat, request.minLon, request.maxLat, request.maxLon))
        if (bbEqual) {
            Log.d(TAG, "Got updated data from database for $boundingBox")
            this.wayMap = wayMap
            isDataUpToDate = true
        }
    }

    fun updateCurrentLocation(location: Location) {
        // when our current data doesn't correspond to the current bounding box
        // try reading data for the new bounding box from db with each location update
        if (!isDataUpToDate) {
            loadSpeedData(boundingBox)
        }

        if (currentLocation == null) {
            currentLocation = location
            return
        }

        // no need to update the speed limit when moving just a tiny bit
        if (location.distanceTo(currentLocation) < 10) {
            // ToDo uncomment after debugging
            // return
        }

        lastLocation = Location(currentLocation)
        currentSpeedLimit = SpeedLimitExtractor.extractSpeedLimit(SpeedLimitExtractor.getClosestWay(location, lastLocation!!, wayMap))
        currentLocation = location

        speedLimitCallback(currentSpeedLimit.second)
    }

    fun updateSpeedMode(currentSpeed: Int) {
        val threshold = settingsStore.get(SpoverSettings.SPEED_THRESHOLD)
        speedMode = when {
            currentSpeedLimit.first + threshold < 0
                    || currentSpeed <= currentSpeedLimit.first + (threshold/2) -> SpeedMode.GREEN
            currentSpeed <= currentSpeedLimit.first + threshold -> SpeedMode.YELLOW
            currentSpeed > currentSpeedLimit.first + threshold -> SpeedMode.RED
            else -> {
                Log.e(TAG, "Unknown speed mode for speed: $currentSpeed and speed limit: $currentSpeedLimit")
                SpeedMode.GREEN
            }
        }
        speedModeCallback()
    }

    /** if a request with the given bounding box exists return all ways and nodes corresponding
     * to that request */
    private class ReadFromDBAsyncTask(var speedLimitService: SpeedLimitService, var boundingBox: BoundingBox) : AsyncTask<Void, Void, String>() {
        val db = AppDatabase.getDatabase(speedLimitService.context)
        var request: Request? = null
        var wayMap: LinkedHashMap<Way, List<Node>> = linkedMapOf()

        private var TAG = ReadFromDBAsyncTask::class.java.simpleName

        override fun doInBackground(vararg params: Void?): String? {
            request = db.requestDao().findRequestByBoundingBox(boundingBox.minLon, boundingBox.minLat, boundingBox.maxLon, boundingBox.maxLat)
            if (request == null) {
                Log.e(TAG, "Fetching data for given bounding box failed, request not found!")
                return null
            }
            val ways: List<Way> = db.wayDao().findWaysByRequestId(request!!.id!!)

            for (way: Way in ways) {
                wayMap[way] = db.nodeDao().findNodesByWayId(way.id!!)
            }
            return null
        }

        // called after doInBackground finished
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (request != null) {
                speedLimitService.onSpeedDataLoaded(request!!, wayMap)
            }
            db.close()
        }
    }
}