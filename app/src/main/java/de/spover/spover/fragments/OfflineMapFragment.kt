package de.spover.spover.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
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
            selectNextArea()
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
//            googleMap!!.addMarker(MarkerOptions().position(topLeftLocation))
//            googleMap!!.addMarker(MarkerOptions().position(bottomRightLocation))

        OpenStreetMapsClient.scheduleBoundingBoxFetching(context!!, boundingBox, isStartedManually = true)
        // FIXME show loading indicator and disable user interactions
    }

    private fun subdivideBoundingBox(bb: BoundingBox): List<BoundingBox> {
        var maxSideLength = 10000 // divide if bounding box is larger
        var bbList = mutableListOf<BoundingBox>()

        var tmpLoc = Location("")
        tmpLoc.latitude = bb.minLat
        tmpLoc.longitude = bb.minLon

        var bbMax = BoundingBox.translateLocationByMeters(tmpLoc, maxSideLength, maxSideLength)
        var currBB = BoundingBox(bb.minLat, bb.minLon, bbMax.latitude, bbMax.longitude)
        while (currBB.maxLat < bb.maxLat) {
            bbList.add(currBB)

            bbMax = BoundingBox.translateLocationByMeters(bbMax, maxSideLength, 0)
            currBB = BoundingBox(currBB.minLat, currBB.maxLon, currBB.maxLat, bbMax.longitude)
            while (currBB.maxLon < bb.maxLon) {
                bbList.add(currBB)
                bbMax = BoundingBox.translateLocationByMeters(bbMax, maxSideLength, 0)
                currBB = BoundingBox(currBB.minLat, currBB.maxLon, currBB.maxLat, bbMax.longitude)
            }
            // todo add remaining part smaller 10.000

            currBB = BoundingBox(currBB.minLat, bb.minLon, currBB.maxLat, bb.maxLon)
            bbMax.latitude = currBB.maxLat
            bbMax.longitude = currBB.maxLon
            bbMax = BoundingBox.translateLocationByMeters(bbMax, 0, maxSideLength)
            // todo add remaining part smaller 10.000
        }
        return bbList
    }

    private fun registerBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null || intent.action != OpenStreetMapsClient.MANUAL_DOWNLOAD_COMPLETE_ACTION) {
                    return
                }

                reloadAreas()
            }
        }
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity!!)
        val intentFilter = IntentFilter(OpenStreetMapsClient.MANUAL_DOWNLOAD_COMPLETE_ACTION)
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

        reloadAreas()
    }

    private fun reloadAreas() {
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

                drawPolygons()

                // currentPositionIndex index will be increased before accessing the first element
                currentPositionIndex = requests.size - 2
                selectNextArea()
            }
        }
    }

    private fun drawPolygons() {
        googleMap.clear()
        polygons.forEach {
            googleMap.addPolygon(it.value)
        }
    }

    private fun deleteSelectedArea() {
        val selectedRequest = requests.removeAt(currentPositionIndex)
        currentPositionIndex--

        polygons.remove(selectedRequest.id!!)
        drawPolygons()

        thread {
            val db = AppDatabase.getDatabase(context!!)

            val ways = db.wayDao().findWaysByRequestId(selectedRequest.id!!)
            ways.forEach { way ->
                val nodes = db.nodeDao().findNodesByWayId(way.id!!)
                db.nodeDao().delete(*nodes.toTypedArray())
                Log.i(TAG, "Deleted ${nodes.size} nodes")
            }
            db.wayDao().delete(*ways.toTypedArray())
            Log.i(TAG, "Deleted ${ways.size} ways")

            db.requestDao().delete(selectedRequest)
            Log.i(TAG, "Deleted request with id=${selectedRequest.id}")

            db.close()

            activity!!.runOnUiThread {
                reloadAreas()
            }
        }
    }

    private fun selectNextArea() {
        if (requests.isEmpty()) {
            return
        }

        currentPositionIndex++
        if (currentPositionIndex >= requests.size) {
            currentPositionIndex = 0
        }

        googleMap.apply {
            val selectedRequest = requests[currentPositionIndex]

            val boundingBox = selectedRequest.boundingBox()
            val centerLon = (boundingBox.minLon + boundingBox.maxLon) / 2
            val centerLat = (boundingBox.minLat + boundingBox.maxLat) / 2
            val position = LatLng(centerLat, centerLon)
            moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14.0f))

            polygons.forEach {
                activity!!.getColor(R.color.colorPrimary)
                if (it.key == selectedRequest.id!!) {
                    it.value.strokeColor(activity!!.getColor(R.color.colorPrimary))
                } else {
                    it.value.strokeColor(activity!!.getColor(R.color.black))
                }
            }
            drawPolygons()
        }
    }
}
