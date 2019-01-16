package de.spover.spover.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import de.spover.spover.R
import de.spover.spover.database.AppDatabase
import de.spover.spover.database.Request

class OfflineAreasFragment : Fragment() {

    companion object {
        private val TAG = OfflineAreasFragment::class.simpleName
    }

    private lateinit var requests: List<Request>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.offline_areas, container, false)
        return initUI(rootView)
    }

    private fun initUI(rootView: View): View {
        val addOfflineAreaBtn = rootView.findViewById<Button>(R.id.btnAddOfflineArea)
        addOfflineAreaBtn.setOnClickListener(this::goToOfflineMap)

        val deleteFirstOfflineAreaBtn = rootView.findViewById<Button>(R.id.btnDeleteFirstOfflineArea)
        deleteFirstOfflineAreaBtn.setOnClickListener {
            deleteFirstOfflineArea(deleteFirstOfflineAreaBtn, addOfflineAreaBtn)
        }

        reloadAreaFragments(deleteFirstOfflineAreaBtn, addOfflineAreaBtn)

        return rootView
    }

    private fun deleteFirstOfflineArea(deleteFirstOfflineAreaBtn: Button, addOfflineAreaBtn: Button) {
        Log.e(TAG, "Deleting first Request")

        deleteFirstOfflineAreaBtn.isEnabled = false

        val thread = Thread {
            val db = AppDatabase.getDatabase(context!!)
            val request = db.requestDao().findRequestById(requests[0].id!!)
            db.requestDao().delete(request)

            Log.e(TAG, "Deleted Request with id=${requests[0].id}")

            activity!!.runOnUiThread {
                deleteFirstOfflineAreaBtn.isEnabled = requests.size > 1
                reloadAreaFragments(deleteFirstOfflineAreaBtn, addOfflineAreaBtn)
            }
        }
        thread.start()
    }

    private fun reloadAreaFragments(deleteFirstOfflineAreaBtn: Button, addOfflineAreaBtn: Button) {
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
                if (requests.isNotEmpty()) {
                    deleteFirstOfflineAreaBtn.isEnabled = true
                }
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

    private fun goToOfflineMap(target: View) {
        val offlineMapFragment = OfflineMapFragment()
        offlineMapFragment.arguments = packToBundle(requests)
        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, offlineMapFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
