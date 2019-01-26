package de.spover.spover.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.spover.spover.BoundingBox
import de.spover.spover.OverlayWithHole
import de.spover.spover.R
import de.spover.spover.database.AppDatabase
import de.spover.spover.database.Request
import de.spover.spover.network.OpenStreetMapsClient
import java.util.*
import kotlin.concurrent.thread


class OfflineMapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        val TAG = OfflineMapFragment::class.simpleName
        const val MAX_BOUNDING_BOX_SIDE_LENGTH = 35000 // divide bounding box if it is larger than this
    }

    private lateinit var broadcastReceiver: BroadcastReceiver

    private var isMenuOpen = false
    private var currentPositionIndex = 0
    private lateinit var googleMap: GoogleMap

    private var requests: MutableList<Request> = Collections.emptyList()
    private var polygons: MutableMap<Long, PolygonOptions> = Collections.emptyMap()

    private lateinit var overlay: OverlayWithHole

    private lateinit var menuBtn: FloatingActionButton
    private lateinit var btnNextArea: FloatingActionButton
    private lateinit var btnDeleteArea: FloatingActionButton
    private lateinit var btnNewArea: FloatingActionButton

    // flag, to make sure view is only centered on initially entering the map fragment
    private var viewHasBeenCentered = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.offline_map, container, false)

        val layout = rootView as RelativeLayout
        overlay = OverlayWithHole(context!!)
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        layout.addView(overlay, params)

        val mapFragment = childFragmentManager.findFragmentById(R.id.offlineMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnNextArea = rootView.findViewById(R.id.btnNextArea)
        btnNextArea.setOnClickListener {
            selectNextAreaAndRedrawArea()
        }

        btnDeleteArea = rootView.findViewById(R.id.btnDeleteArea)
        btnDeleteArea.setOnClickListener {
            closeMenu()
            deleteSelectedArea()
        }

        btnNewArea = rootView.findViewById(R.id.btnNewArea)
        btnNewArea.setOnClickListener {
            startDownloadOfNewArea()
            closeMenu()
        }

        menuBtn = rootView.findViewById(R.id.btnMenu)
        menuBtn.setOnClickListener {
            if (!isMenuOpen) {
                openMenu()
            } else {
                closeMenu()
            }
        }

        registerBroadcastReceiver()

        return rootView
    }

    private fun openMenu() {
        isMenuOpen = true

        btnNextArea.animate().translationY(-resources.getDimension(R.dimen.next_area_position))
        btnDeleteArea.animate().translationY(-resources.getDimension(R.dimen.delete_area_position))
        btnNewArea.animate().translationY(-resources.getDimension(R.dimen.new_area_position))

        btnNextArea.show()
        btnDeleteArea.show()
        btnNewArea.show()

        menuBtn.animate().rotation(90.0f)
    }

    private fun closeMenu() {
        isMenuOpen = false

        btnNextArea.animate().translationY(0.0f)
        btnDeleteArea.animate().translationY(0.0f)
        btnNewArea.animate().translationY(0.0f)

        btnNextArea.hide()
        btnDeleteArea.hide()
        btnNewArea.hide()

        menuBtn.animate().rotation(0.0f)
    }

    override fun onDestroy() {
        super.onDestroy()
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity!!)
        localBroadcastManager.unregisterReceiver(broadcastReceiver)
    }

    private fun startDownloadOfNewArea() {
        val topRight = Point(overlay.cutout.right.toInt(), overlay.cutout.top.toInt())
        val topRightLocation = googleMap.projection.fromScreenLocation(topRight)

        val bottomLeft = Point(overlay.cutout.left.toInt(), overlay.cutout.bottom.toInt())
        val bottomLeftLocation = googleMap.projection.fromScreenLocation(bottomLeft)

        val boundingBox = BoundingBox(
                bottomLeftLocation.latitude,
                bottomLeftLocation.longitude,
                topRightLocation.latitude,
                topRightLocation.longitude
        )

        for (bbPart: BoundingBox in subdivideBoundingBox(boundingBox)) {
            Log.d(TAG, "bb $bbPart")
            OpenStreetMapsClient.scheduleBoundingBoxFetching(context!!, bbPart, isStartedManually = true)
        }
        // FIXME show loading indicator and disable user interactions
    }

    private fun subdivideBoundingBox(bb: BoundingBox): List<BoundingBox> {
        val bbList = mutableListOf<BoundingBox>()

        var bbMin = Location("")
        bbMin.latitude = bb.minLat
        bbMin.longitude = bb.minLon
        var bbMax = BoundingBox.translateLocationByMeters(bbMin, 0, MAX_BOUNDING_BOX_SIDE_LENGTH)

        var remainingAdded = false
        while (bbMax.latitude < bb.maxLat || !remainingAdded) {

            if (bbMax.latitude > bb.maxLat) {
                remainingAdded = true
                bbMax.latitude = bb.maxLat
            }
            bbMax = BoundingBox.translateLocationByMeters(bbMax, MAX_BOUNDING_BOX_SIDE_LENGTH, 0)

            while (bbMax.longitude < bb.maxLon) {
                val tmpBB = BoundingBox(bbMin.latitude, bbMin.longitude, bbMax.latitude, bbMax.longitude)
                bbList.add(tmpBB)

                bbMax = BoundingBox.translateLocationByMeters(bbMax, MAX_BOUNDING_BOX_SIDE_LENGTH, 0)
                bbMin = BoundingBox.translateLocationByMeters(bbMin, MAX_BOUNDING_BOX_SIDE_LENGTH, 0)
            }
            val remainingBB = BoundingBox(bbMin.latitude, bbMin.longitude, bbMax.latitude, bb.maxLon)
            bbList.add(remainingBB)

            bbMax.longitude = bb.minLon
            bbMax = BoundingBox.translateLocationByMeters(bbMax, 0, MAX_BOUNDING_BOX_SIDE_LENGTH)
            bbMin.longitude = bb.minLon
            bbMin = BoundingBox.translateLocationByMeters(bbMin, 0, MAX_BOUNDING_BOX_SIDE_LENGTH)
        }

        return bbList
    }

    private fun registerBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) {
                    return
                }
                when (intent.action) {
                    null -> return
                    OpenStreetMapsClient.AUTO_DOWNLOAD_COMPLETE_ACTION -> reloadAreas(false)
                    OpenStreetMapsClient.MANUAL_DOWNLOAD_COMPLETE_ACTION -> reloadAreas(true)
                    OpenStreetMapsClient.DOWNLOAD_FAILED_ACTION -> {
                        Toast.makeText(context!!, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity!!)
        val intentFilter = IntentFilter()
        intentFilter.addAction(OpenStreetMapsClient.MANUAL_DOWNLOAD_COMPLETE_ACTION)
        intentFilter.addAction(OpenStreetMapsClient.AUTO_DOWNLOAD_COMPLETE_ACTION)
        intentFilter.addAction(OpenStreetMapsClient.DOWNLOAD_FAILED_ACTION)
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap == null) {
            return
        }

        this.googleMap = googleMap

        // Disabling rotation, because OSM does not support rotated areas
        googleMap.uiSettings.isRotateGesturesEnabled = false
        if (ContextCompat.checkSelfPermission(context!!,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }

        @Suppress("DEPRECATION")
        googleMap.setOnMyLocationChangeListener {
            if (requests.isEmpty() && !viewHasBeenCentered) {
                val myLocation = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14.0f))

                viewHasBeenCentered = true
            }
        }

        reloadAreas()
    }

    private fun reloadAreas(moveCameraAfterUpdate: Boolean = true) {
        thread {
            val db = AppDatabase.getDatabase(context!!)
            requests = db.requestDao().findAllRequests().toMutableList()
            db.close()

            activity?.runOnUiThread {
                polygons = requests.map {
                    val rectOptions = PolygonOptions()
                            .add(
                                    LatLng(it.minLat, it.minLon),
                                    LatLng(it.maxLat, it.minLon),
                                    LatLng(it.maxLat, it.maxLon),
                                    LatLng(it.minLat, it.maxLon)
                            )
                    it.id!! to rectOptions
                }.toMap().toMutableMap()

                // currentPositionIndex will be increased before accessing the first element
                currentPositionIndex = requests.size - 2
                selectNextAreaAndRedrawArea(moveCameraAfterUpdate)
            }
        }
    }

    private fun deleteSelectedArea() {
        val selectedRequest = requests.removeAt(currentPositionIndex)
        polygons.remove(selectedRequest.id!!)

        selectNextAreaAndRedrawArea()

        thread {
            val db = AppDatabase.getDatabase(context!!)

            val ways = db.wayDao().findWaysByRequestId(selectedRequest.id!!)
            val deletedNodes = ways.map { way ->
                val nodes = db.nodeDao().findNodesByWayId(way.id!!)
                db.nodeDao().delete(*nodes.toTypedArray())
                nodes.size
            }.reduce { acc, i -> acc + i }

            db.wayDao().delete(*ways.toTypedArray())
            Log.i(TAG, "Deleted $deletedNodes nodes")
            Log.i(TAG, "Deleted ${ways.size} ways")

            db.requestDao().delete(selectedRequest)
            Log.i(TAG, "Deleted request with id=${selectedRequest.id}")

            db.close()

            activity!!.runOnUiThread {
                reloadAreas(false)
            }
        }
    }

    private fun selectNextAreaAndRedrawArea(moveCameraAfterUpdate: Boolean = true) {
        // Remove all polygons from map
        googleMap.clear()

        if (requests.isEmpty()) {
            btnDeleteArea.isEnabled = false
            btnNextArea.isEnabled = false
            return
        }
        btnDeleteArea.isEnabled = true
        btnNextArea.isEnabled = true

        currentPositionIndex++
        if (currentPositionIndex >= requests.size) {
            currentPositionIndex = 0
        }

        googleMap.apply {
            val selectedRequest = requests[currentPositionIndex]

            if (moveCameraAfterUpdate) {
                val boundingBox = selectedRequest.boundingBox()
                val southWest = LatLng(boundingBox.maxLat, boundingBox.maxLon)
                val northEast = LatLng(boundingBox.minLat, boundingBox.minLon)
                val bounds = LatLngBounds(northEast, southWest)
                animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 270))
            }

            polygons.forEach {
                if (it.key == selectedRequest.id!!) {
                    it.value.strokeColor(activity!!.getColor(R.color.colorPrimary))
                } else {
                    it.value.strokeColor(activity!!.getColor(R.color.black))
                }
                googleMap.addPolygon(it.value)
            }
        }
    }
}
