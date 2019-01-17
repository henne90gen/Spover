package de.spover.spover.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
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
import de.spover.spover.network.OpenStreetMapsClient
import java.util.*


class OfflineMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var broadcastReceiver: BroadcastReceiver

    private var currentPositionIndex = 0
    private var googleMap: GoogleMap? = null
    private var positions: List<LatLng> = Collections.emptyList()

    private lateinit var overlay: OverlayWithHole

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.offline_map, container, false)

        val layout = rootView as RelativeLayout
        overlay = OverlayWithHole(context!!)
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        layout.addView(overlay, params)

        val mapFragment = childFragmentManager.findFragmentById(R.id.offlineMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnNextArea = rootView.findViewById<FloatingActionButton>(R.id.btnNextArea)
        btnNextArea.setOnClickListener {
            moveToNextPosition()
        }

        val btnNewArea = rootView.findViewById<FloatingActionButton>(R.id.btnNewArea)
        btnNewArea.setOnClickListener {
            startDownloadOfNewArea()
        }

        registerBroadcastReceiver()

        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity!!)
        localBroadcastManager.unregisterReceiver(broadcastReceiver)
    }

    private fun startDownloadOfNewArea() {
        val topRight = Point(overlay.cutout.right.toInt(), overlay.cutout.top.toInt())
        val topRightLocation = googleMap!!.projection.fromScreenLocation(topRight)

        val bottomLeft = Point(overlay.cutout.left.toInt(), overlay.cutout.bottom.toInt())
        val bottomLeftLocation = googleMap!!.projection.fromScreenLocation(bottomLeft)

        val boundingBox = BoundingBox(
                bottomLeftLocation.latitude,
                bottomLeftLocation.longitude,
                topRightLocation.latitude,
                topRightLocation.longitude
        )
//            googleMap!!.addMarker(MarkerOptions().position(topLeftLocation))
//            googleMap!!.addMarker(MarkerOptions().position(bottomRightLocation))

        OpenStreetMapsClient.scheduleBoundingBoxFetching(context!!, boundingBox)
    }

    private fun registerBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && intent.action == OpenStreetMapsClient.DOWNLOAD_COMPLETE_ACTION) {
                    activity!!.supportFragmentManager.popBackStack()
                }
            }
        }
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity!!)
        val intentFilter = IntentFilter(OpenStreetMapsClient.DOWNLOAD_COMPLETE_ACTION)
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap == null) {
            return
        }

        this.googleMap = googleMap

        // Disabling rotation, because OSM does not support rotated areas
        googleMap.uiSettings.isRotateGesturesEnabled = false

        val ids = arguments!!.getStringArrayList("ids")
                ?: Collections.emptyList<String>()

        positions = ids.map {
            val minLon = arguments!!.getDouble("$it-minLon")
            val minLat = arguments!!.getDouble("$it-minLat")
            val maxLon = arguments!!.getDouble("$it-maxLon")
            val maxLat = arguments!!.getDouble("$it-maxLat")
            val rectOptions = PolygonOptions()
                    .add(LatLng(minLat, minLon),
                            LatLng(maxLat, minLon),
                            LatLng(maxLat, maxLon),
                            LatLng(minLat, maxLon))
            googleMap.addPolygon(rectOptions)
            val centerLon = (minLon + maxLon) / 2
            val centerLat = (minLat + maxLat) / 2
            LatLng(centerLat, centerLon)
        }

        moveToNextPosition()

        // This is how we can find the visible region
        // googleMap.projection.visibleRegion.latLngBounds.northeast
    }

    private fun moveToNextPosition() {
        if (googleMap == null) {
            return
        }

        googleMap?.apply {
            moveCamera(CameraUpdateFactory.newLatLngZoom(positions[currentPositionIndex++], 12.0f))
            if (currentPositionIndex >= positions.size) {
                currentPositionIndex = 0
            }
        }
    }
}
