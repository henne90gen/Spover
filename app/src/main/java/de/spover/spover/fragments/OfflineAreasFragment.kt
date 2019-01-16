package de.spover.spover.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.spover.spover.MainActivity
import de.spover.spover.R
import de.spover.spover.database.AppDatabase
import de.spover.spover.database.Request

class OfflineAreasFragment : Fragment() {

    private lateinit var requests: List<Request>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.offline_areas, container, false)
        return initUI(rootView)
    }

    private fun initUI(rootView: View): View {
        val addOfflineAreaBtn = rootView.findViewById<FloatingActionButton>(R.id.btnAddOfflineArea)
        addOfflineAreaBtn.setOnClickListener(this::goToOfflineMap)

        reloadAreaFragments(addOfflineAreaBtn)

        return rootView
    }

    private fun reloadAreaFragments(addOfflineAreaBtn: FloatingActionButton) {
        addOfflineAreaBtn.isEnabled = false

        // Remove all old data
        for (fragment in childFragmentManager.fragments) {
            if (fragment !is OfflineAreaFragment) {
                continue
            }

            childFragmentManager.beginTransaction().remove(fragment).commit()
        }

        // Load new data from database and display it
        val thread = Thread {
            val transaction = childFragmentManager.beginTransaction()
            val db = AppDatabase.getDatabase(context!!)
            requests = db.requestDao().findAllRequests()
            // TODO use these bundles to pass the bounding boxes to offline map for display
            requests.forEach {
                val offlineAreaFragment = OfflineAreaFragment()
                offlineAreaFragment.arguments = packToBundle(it)
                transaction.add(R.id.offlineAreasContainer, offlineAreaFragment)
            }
            transaction.commit()

            activity!!.runOnUiThread {
                addOfflineAreaBtn.isEnabled = true
            }
        }
        thread.start()
    }

    private fun packToBundle(request: Request): Bundle {
        val bundle = Bundle()
        bundle.putDouble("minLon", request.minLon)
        bundle.putDouble("minLat", request.minLat)
        bundle.putDouble("maxLon", request.maxLon)
        bundle.putDouble("maxLat", request.maxLat)
        bundle.putString("creationTime", request.creationTime.toString())
        return bundle
    }

    private fun packToBundle(requests: List<Request>): Bundle {
        val bundle = Bundle()

        val ids = requests.map {
            bundle.putDouble("${it.id}-minLon", it.minLon)
            bundle.putDouble("${it.id}-minLat", it.minLat)
            bundle.putDouble("${it.id}-maxLon", it.maxLon)
            bundle.putDouble("${it.id}-maxLat", it.maxLat)
            it.id.toString()
        }
        bundle.putStringArrayList("ids", ArrayList(ids))
        return bundle
    }

    private fun goToOfflineMap(it: View) {
        val offlineMapFragment = OfflineMapFragment()
        offlineMapFragment.arguments = packToBundle(requests)
        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, offlineMapFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
