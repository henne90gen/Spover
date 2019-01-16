package de.spover.spover.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions
import de.spover.spover.R


class OfflineMapFragment : Fragment(), OnMapReadyCallback {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.offline_map, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.offlineMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return rootView
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap == null) {
            return
        }

        var lastPosition = LatLng(-33.852, 151.211)
        arguments!!.getStringArrayList("ids")?.forEach {
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
            lastPosition = LatLng(minLat, minLon)
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(lastPosition))

        // This is how we can find the visible region
        // googleMap.projection.visibleRegion.latLngBounds.northeast
    }
}
