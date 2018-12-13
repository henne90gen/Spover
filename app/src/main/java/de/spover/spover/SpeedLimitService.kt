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
import kotlin.math.min


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
    private var lastLocation: Location? = null

    private var currentSpeedLimit: Int = 0 // current speed limit in kph
    var speedMode: SpeedMode = SpeedMode.GREEN
    // linked since we want to preserve the node order
    private var wayMap: LinkedHashMap<Way, List<Node>> = linkedMapOf()

    fun loadSpeedData(boundingBox: BoundingBox) {
        val response = ReadFromDBAsyncTask(this, boundingBox).execute()
        Log.d(TAG, "should have loaded")
    }

    fun onSpeedDataLoaded(request: Request?, wayMap: LinkedHashMap<Way, List<Node>>) {
        Log.d(TAG, "Loaded speed data for $request from DB")
        this.wayMap = wayMap
    }

    /**
     * find the current speed limit
     * (based on last two location and way data for the current bounding box)
     */
    private fun findSpeedLimit(location: Location): Int {

        //
        // Todo needs to be tested for working correctly
        // how accurate is it for:
        //      - the end of streets
        //      - changes in direction
        //      - small/fast speeds
        //
        // when last street and wannabe new street are close it's maybe better to keep the old one
        // despite beeing a little bit closer to the new one
        //

        if (lastLocation == null) {
            // ToDo check if the current location is still inside the current bounding box
            return -1
        }

        var minDistance = Float.POSITIVE_INFINITY
        var minDistanceWay: Way? = null
        val nodeLocation = Location("")

        for ((way: Way, nodes:List<Node>) in wayMap) {
            var lastNodeLastLocDistance = Float.POSITIVE_INFINITY
            for (node: Node in nodes) {
                nodeLocation.latitude = node.latitude
                nodeLocation.longitude = node.longitude
                val currNodeCurrLocDistance = location.distanceTo(nodeLocation)
                if ((lastNodeLastLocDistance + currNodeCurrLocDistance) < minDistance) {
                    minDistance = lastNodeLastLocDistance + currNodeCurrLocDistance
                    minDistanceWay = way
                }
                lastNodeLastLocDistance = currNodeCurrLocDistance
            }
        }

        if (minDistanceWay != null) {
            Log.d(TAG, "nearest way is ${minDistance/2}m away")
            Log.d(TAG, "Current speed limit is: ${minDistanceWay.maxSpeed}")
            var speedLimit = minDistanceWay.maxSpeed.toIntOrNull()
            if (speedLimit == null) {
                // ToDo handle special maxSpeed cases (e.g. unlimited...)
                speedLimit = 999
            }
            return speedLimit
        }

        return -1
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

        lastLocation = Location(currentLocation)
        currentLocation = location
        currentSpeedLimit = findSpeedLimit(location)

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
        var wayMap: LinkedHashMap<Way, List<Node>> = linkedMapOf()
        var nodes: MutableList<Node> = mutableListOf()

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

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            speedLimitService.onSpeedDataLoaded(request, wayMap)
        }
    }
}