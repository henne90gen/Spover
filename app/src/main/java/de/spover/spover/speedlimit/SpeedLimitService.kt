package de.spover.spover.speedlimit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.util.Log
import de.spover.spover.database.AppDatabase
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings
import android.os.AsyncTask
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.spover.spover.BoundingBox
import de.spover.spover.database.Node
import de.spover.spover.database.Request
import de.spover.spover.database.Way
import de.spover.spover.network.OpenStreetMapsClient

enum class SpeedMode {
    GREEN,
    YELLOW,
    RED
}


typealias SpeedLimitChangedCallback = (String) -> Unit
typealias SpeedModeChangeCallback = () -> Unit

class SpeedLimitService(val context: Context, val speedLimitCallback: SpeedLimitChangedCallback, val speedModeCallback: SpeedModeChangeCallback) : BroadcastReceiver() {

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
    private var availableBoundingBoxes: List<BoundingBox> = emptyList()

    private var boundingBox: BoundingBox = BoundingBox(0.0, 0.0, 0.0, 0.0)
    private var downloading: Boolean = false
    private var updatingSpeedData: Boolean = false

    init {
        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
        val intentFilter = IntentFilter(OpenStreetMapsClient.AUTO_DOWNLOAD_COMPLETE_ACTION)
        localBroadcastManager.registerReceiver(this, intentFilter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action != OpenStreetMapsClient.AUTO_DOWNLOAD_COMPLETE_ACTION) {
            return
        }

        downloading = false

        currentLocation?.let {
            if (isLocationOutsideCurrentBoundingBox(it)) {
                loadSpeedDataFromLocalDatabase(it)
                return
            }
        }
    }

    private fun loadSpeedDataFromLocalDatabase(location: Location) {
        Log.i(TAG, "Loading speed data from local database")
        updatingSpeedData = true
        ReadFromDBAsyncTask(this, location).execute()
    }

    private fun updateCurrentSpeedData(request: Request, wayMap: LinkedHashMap<Way, List<Node>>) {
        this.wayMap = wayMap
        this.boundingBox = request.boundingBox()
        updatingSpeedData = false
        Log.d(TAG, "Got updated data from database for $boundingBox")
    }

    fun updateCurrentLocation(location: Location) {
        Log.i(TAG, "Current location: $location")
        Log.i(TAG, "Current boundingBox: $boundingBox")

        val newBoundingBoxDistFromLocation = 1000
        val minBoundingBoxDistFromEdge = 200
        if (!downloading
                && !boundingBox.isBoundingBoxValid(location, minBoundingBoxDistFromEdge)
                && hasNoMoreValidBoundingBoxes(location)) {
            val newBoundingBox = BoundingBox.createBoundingBox(location, newBoundingBoxDistFromLocation)
            Log.d(TAG, "Started download for $newBoundingBox")
            OpenStreetMapsClient.scheduleBoundingBoxFetching(
                    context,
                    newBoundingBox
            )
            downloading = true
        }

        // when our current data doesn't correspond to the current bounding box
        // try reading data for the new bounding box from db with each location update
        if (isLocationOutsideCurrentBoundingBox(location)) {
            loadSpeedDataFromLocalDatabase(location)
            return
        }

        // initializing the current location
        if (currentLocation == null) {
            currentLocation = location
            return
        }

        // no need to update the speed limit when moving just a tiny bit
        if (location.distanceTo(currentLocation) < 10) {
            // FIXME uncomment after debugging
            // return
        }

        lastLocation = Location(currentLocation)
        val closestWay = SpeedLimitExtractor.findClosestWay(location, lastLocation!!, wayMap)
        currentSpeedLimit = SpeedLimitExtractor.extractSpeedLimit(closestWay)
        currentLocation = location

        speedLimitCallback(currentSpeedLimit.second)
    }

    private fun hasNoMoreValidBoundingBoxes(location: Location): Boolean {
        return availableBoundingBoxes.filter { boundingBox != it }.firstOrNull { it.contains(location) } == null
    }

    private fun isLocationOutsideCurrentBoundingBox(location: Location): Boolean {
        return !boundingBox.contains(location)
    }

    fun updateSpeedMode(currentSpeed: Int) {
        val threshold = settingsStore.get(SpoverSettings.SPEED_THRESHOLD)
        speedMode = when {
            currentSpeedLimit.first + threshold < 0
                    || currentSpeed <= currentSpeedLimit.first + (threshold / 2) -> SpeedMode.GREEN
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
    private class ReadFromDBAsyncTask(val speedLimitService: SpeedLimitService, val location: Location) : AsyncTask<Void, Void, Void>() {
        val db = AppDatabase.getDatabase(speedLimitService.context)
        var request: Request? = null
        var wayMap: LinkedHashMap<Way, List<Node>> = linkedMapOf()

        private var TAG = ReadFromDBAsyncTask::class.java.simpleName

        override fun doInBackground(vararg params: Void?): Void? {
            val requests = db.requestDao().findAllRequests()
            speedLimitService.availableBoundingBoxes = requests.map(Request::boundingBox).toList()

            Log.i(TAG, "Found ${requests.size} available areas")

            request = requests.firstOrNull {
                it.boundingBox().contains(location)
            }

            Log.i(TAG, "New area selected: $request")

            if (request == null) {
                Log.w(TAG, "There is no new area that could be used")
                return null
            }
            val ways: List<Way> = db.wayDao().findWaysByRequestId(request!!.id!!)

            for (way: Way in ways) {
                wayMap[way] = db.nodeDao().findNodesByWayId(way.id!!)
            }
            return null
        }

        // called after doInBackground finished
        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            request?.let {
                speedLimitService.updateCurrentSpeedData(it, wayMap)
            }
            db.close()
        }
    }
}