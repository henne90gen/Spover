package de.spover.spover.speedlimit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.spover.spover.BoundingBox
import de.spover.spover.database.DatabaseHelper
import de.spover.spover.database.Node
import de.spover.spover.database.Request
import de.spover.spover.database.Way
import de.spover.spover.network.OpenStreetMapsClient
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings
import java.util.concurrent.atomic.AtomicBoolean

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
        private val NEW_BB_DIST_FROM_LOCATION = 1000
        private val MIN_BB_DIST_FROM_EDGE = 200
    }

    private var settingsStore: SettingsStore = SettingsStore(context)

    private var currentLocation: Location? = null
    private var lastLocation: Location? = null

    private var currentSpeedLimit: Pair<Int, String> = Pair(Int.MAX_VALUE, "unknown") // current speed limit as number (in kph) and textual description
    var speedMode: SpeedMode = SpeedMode.GREEN

    // linked since we want to preserve the node order
    private var wayMap: LinkedHashMap<Way, List<Node>> = linkedMapOf()
    private var availableBoundingBoxes: MutableList<BoundingBox> = ArrayList()

    private var boundingBox: BoundingBox = BoundingBox(0.0, 0.0, 0.0, 0.0)
    private var downloading: AtomicBoolean = AtomicBoolean(false)
    private var updatingSpeedData: AtomicBoolean = AtomicBoolean(false)

    init {
        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
        val intentFilter = IntentFilter()
        intentFilter.addAction(OpenStreetMapsClient.AUTO_DOWNLOAD_COMPLETE_ACTION)
        intentFilter.addAction(OpenStreetMapsClient.MANUAL_DOWNLOAD_COMPLETE_ACTION)
        intentFilter.addAction(OpenStreetMapsClient.DOWNLOAD_FAILED_ACTION)
        localBroadcastManager.registerReceiver(this, intentFilter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }

        if (intent.action == OpenStreetMapsClient.DOWNLOAD_FAILED_ACTION) {
            downloading.set(false)
        }

        if (intent.action == OpenStreetMapsClient.AUTO_DOWNLOAD_COMPLETE_ACTION) {
            downloading.set(false)

            if (currentLocation != null) {
                if (isLocationOutsideCurrentBoundingBox(currentLocation!!)) {
                    loadSpeedDataFromLocalDatabase(currentLocation!!)
                }
            }
        }
        if (intent.action == OpenStreetMapsClient.AUTO_DOWNLOAD_COMPLETE_ACTION || intent.action == OpenStreetMapsClient.MANUAL_DOWNLOAD_COMPLETE_ACTION) {
            val downloadedBoundingBox = OpenStreetMapsClient.convert(intent.extras!!)
            availableBoundingBoxes.add(downloadedBoundingBox)
        }
    }

    /**
     * If a request with the given bounding box exists return all ways and nodes corresponding
     * to that request
     */
    private fun loadSpeedDataFromLocalDatabase(location: Location) {
        updatingSpeedData.set(true)
        DatabaseHelper.INSTANCE.executeTransaction(context) { db ->
            Log.i(TAG, "Loading speed data from local database")

            val requests = db.requestDao().findAllRequests()
            this.availableBoundingBoxes = requests.map(Request::boundingBox).toMutableList()

            Log.i(TAG, "Found ${requests.size} available areas")

            val request = requests.firstOrNull {
                it.boundingBox().contains(location)
            }

            Log.i(TAG, "New area selected: $request")

            if (request == null) {
                Log.w(TAG, "There is no new area that could be used")
                this.updateCurrentSpeedData(null, wayMap)
            } else {
                val ways: List<Way> = db.wayDao().findWaysByRequestId(request.id!!)

                for (way: Way in ways) {
                    wayMap[way] = db.nodeDao().findNodesByWayId(way.id!!)
                }
                this.updateCurrentSpeedData(request, wayMap)
            }
        }
    }

    private fun updateCurrentSpeedData(request: Request?, wayMap: LinkedHashMap<Way, List<Node>>) {
        this.wayMap = wayMap
        updatingSpeedData.set(false)
        if (request == null) {
            currentLocation?.let {
                downloadNewArea(it, NEW_BB_DIST_FROM_LOCATION)
            }
        } else {
            this.boundingBox = request.boundingBox()
        }
    }

    fun updateCurrentLocation(location: Location) {
        // initializing the current location
        if (currentLocation == null) {
            currentLocation = location
            return
        }

        if (updatingSpeedData.get()) {
            return
        }

        // when our current data doesn't correspond to the current bounding box
        // try reading data for the new bounding box from db with each location update
        if (isLocationOutsideCurrentBoundingBox(location)) {
            loadSpeedDataFromLocalDatabase(location)
            return
        }

        if (!downloading.get()
                && !updatingSpeedData.get()
                && !boundingBox.isBoundingBoxValid(location, MIN_BB_DIST_FROM_EDGE)
                && hasNoMoreValidBoundingBoxes(location)) {
            downloadNewArea(location, NEW_BB_DIST_FROM_LOCATION)
            return
        }

        Log.d(TAG, "update speed limit")
        lastLocation = Location(currentLocation)
        updateSpeedLimit(location)
    }

    private fun updateSpeedLimit(location: Location) {
        // no need to update the speed limit when moving just a tiny bit
        if (location.distanceTo(currentLocation) < 10) {
            // FIXME uncomment after debugging
            // return
        }

        val closestWay = SpeedLimitExtractor.findClosestWay(location, lastLocation!!, wayMap)
        currentSpeedLimit = SpeedLimitExtractor.extractSpeedLimit(closestWay)
        currentLocation = location

        speedLimitCallback(currentSpeedLimit.second)
    }

    private fun downloadNewArea(location: Location, newBoundingBoxDistFromLocation: Int) {
        if (downloading.get() || updatingSpeedData.get()) {
            return
        }
        downloading.set(true)
        val newBoundingBox = BoundingBox.createBoundingBox(location, newBoundingBoxDistFromLocation)
        OpenStreetMapsClient.scheduleBoundingBoxFetching(
                context,
                newBoundingBox
        )
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
            currentSpeed <= currentSpeedLimit.first -> SpeedMode.GREEN
            currentSpeed <= currentSpeedLimit.first + threshold -> SpeedMode.YELLOW
            currentSpeed > currentSpeedLimit.first + threshold -> SpeedMode.RED
            else -> {
                Log.e(TAG, "Unknown speed mode for speed: $currentSpeed and speed limit: $currentSpeedLimit")
                SpeedMode.GREEN
            }
        }
        speedModeCallback()
    }
}