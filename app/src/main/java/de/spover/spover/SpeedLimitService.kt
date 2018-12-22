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


typealias SpeedLimitChangedCallback = (String) -> Unit
typealias SpeedModeChangeCallback = () -> Unit

class SpeedLimitService(val context: Context, val speedLimitCallback: SpeedLimitChangedCallback, val speedModeCallback: SpeedModeChangeCallback) {

    companion object {
        private var TAG = SpeedLimitService::class.java.simpleName

        fun getClosestWay(location: Location, lastLocation: Location, wayMap: LinkedHashMap<Way, List<Node>>): Way? {
            if (wayMap.size == 0) return null

            var minDistance = Float.POSITIVE_INFINITY
            var result: Way? = null
            val nodeLocation = Location("")

            for ((way: Way, nodes:List<Node>) in wayMap) {
                var lastNodeLastLocDistance = Float.POSITIVE_INFINITY
                for (node: Node in nodes) {
                    nodeLocation.latitude = node.latitude
                    nodeLocation.longitude = node.longitude
                    val currNodeCurrLocDistance = location.distanceTo(nodeLocation)
                    if ((lastNodeLastLocDistance + currNodeCurrLocDistance) < minDistance) {
                        minDistance = lastNodeLastLocDistance + currNodeCurrLocDistance
                        result = way
                    }
                    lastNodeLastLocDistance = lastLocation.distanceTo(nodeLocation)
                }
            }

            // if the closest way is to far away we don't accept it
            if (minDistance/2 > 500) {
                Log.e(TAG, "Closest way is to far away ${minDistance/2}m")
                result = null
            }

            Log.d(TAG, "nearest way is around ${minDistance/2}m away and has a speed limit of ${extractSpeedLimit(result)}km/h")
            return result
        }

        /**
         * find the current speed limit (based on last two location and way data for the current bounding box)
         */
        fun extractSpeedLimit(way: Way?): Pair<Int, String> {
            return when (way) {
                null -> {
                    Log.e(TAG, "current way is undefined, no speed limit available")
                    Pair(Int.MAX_VALUE, "no_w")
                }
                else -> {
                    // Default case with speed given as integer
                    val result = way.maxSpeed.toIntOrNull()
                    if (result != null) {
                        return Pair(result, result.toString())
                    }

                    // special speed tag where tag corresponds to a certain speed
                    val maxSpeedTags: HashMap<String, Pair<Int, String>> = hashMapOf("none" to Pair(Int.MAX_VALUE, "inf"), "walk" to Pair(5, "walk"))
                    if (way.maxSpeed in maxSpeedTags) {
                        return maxSpeedTags[way.maxSpeed]!!
                    }

                    Log.e(TAG, "couldn't parse ways max speed tag")
                    Pair(Int.MAX_VALUE, "c_p")
                }
            }
        }
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
        //Log.d(TAG, "Loaded speed data for $request from DB")

        // check if request bounding box and the current bounding box are equal
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
        currentSpeedLimit = extractSpeedLimit(getClosestWay(location, lastLocation!!, wayMap))
        currentLocation = location

        speedLimitCallback(currentSpeedLimit.second)
    }

    fun updateSpeedMode(currentSpeed: Int) {
        val threshold = settingsStore.get(SpoverSettings.SPEED_THRESHOLD)

        speedMode = when {
            currentSpeed <= currentSpeedLimit.first + (threshold/2) -> SpeedMode.GREEN
            currentSpeed < currentSpeedLimit.first + threshold -> SpeedMode.YELLOW
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
        val db = AppDatabase.createBuilder(speedLimitService.context).build()
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