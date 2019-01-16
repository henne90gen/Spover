package de.spover.spover.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.spover.spover.R

class OfflineAreaFragment : Fragment() {

    companion object {
        private val TAG = OfflineAreaFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.offline_area, container, false)
        return initUI(rootView)
    }

    private fun initUI(rootView: View): View {
        val creationTimeLabel = rootView.findViewById<TextView>(R.id.creationTime)
        creationTimeLabel.text = arguments!!.getString("creationTime")

        val minLonLabel = rootView.findViewById<TextView>(R.id.minLon)
        minLonLabel.text = "${arguments!!.getDouble("minLon")}"

        val minLatLabel = rootView.findViewById<TextView>(R.id.minLat)
        minLatLabel.text = "${arguments!!.getDouble("minLat")}"

        val maxLonLabel = rootView.findViewById<TextView>(R.id.maxLon)
        maxLonLabel.text = "${arguments!!.getDouble("maxLon")}"

        val maxLatLabel = rootView.findViewById<TextView>(R.id.maxLat)
        maxLatLabel.text = "${arguments!!.getDouble("maxLat")}"

        return rootView
    }
}