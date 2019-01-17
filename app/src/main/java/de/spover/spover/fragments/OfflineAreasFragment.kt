package de.spover.spover.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.spover.spover.R
import de.spover.spover.database.AppDatabase
import de.spover.spover.database.Request

class OfflineAreasFragment : Fragment() {

    companion object {
        private val TAG = OfflineAreasFragment::class.simpleName
        private const val AREA_CHECKBOX_TAG = "areaCheckBox"
    }

    private lateinit var requests: List<Request>
    private val selectedRequestIds: MutableList<Request> = ArrayList()

    private lateinit var areasContainer: LinearLayout
    private lateinit var addOfflineAreaBtn: Button
    private lateinit var deleteOfflineAreasBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.offline_areas, container, false)
        return initUI(rootView)
    }

    private fun initUI(rootView: View): View {
        areasContainer = rootView.findViewById(R.id.offlineAreasContainer)

        addOfflineAreaBtn = rootView.findViewById(R.id.btnAddOfflineArea)
        addOfflineAreaBtn.setOnClickListener { goToOfflineMap() }

        deleteOfflineAreasBtn = rootView.findViewById(R.id.btnDeleteOfflineAreas)
        deleteOfflineAreasBtn.setOnClickListener { deleteOfflineAreas() }

        reloadAreaFragments()

        return rootView
    }

    private fun deleteOfflineAreas() {
        Log.e(TAG, "Deleting all selected Requests")

        deleteOfflineAreasBtn.isEnabled = false

        // Remove all data that will be old soon
        // FIXME instead of just removing everything we should freeze the UI and display a loading animation
        areasContainer.removeAllViews()

        val thread = Thread {
            val db = AppDatabase.getDatabase(context!!)

            db.requestDao().delete(*selectedRequestIds.toTypedArray())
            selectedRequestIds.forEach {
                Log.w(TAG, "Deleted request with id=${it.id}")
            }
            selectedRequestIds.clear()

            db.close()

            activity!!.runOnUiThread {
                reloadAreaFragments()
            }
        }
        thread.start()
    }

    private fun reloadAreaFragments() {
        addOfflineAreaBtn.isEnabled = false

        // Remove all old data
        areasContainer.removeAllViews()

        // Load new data from database and display it
        val thread = Thread {
            val db = AppDatabase.getDatabase(context!!)
            requests = db.requestDao().findAllRequests()

            requests = requests.sortedBy { it.creationTime }.reversed()

            activity!!.runOnUiThread {
                requests.forEach(this::createOfflineArea)
                addOfflineAreaBtn.isEnabled = true
            }
        }
        thread.start()
    }

    private fun createOfflineArea(request: Request) {
        val newAreaContainer = LinearLayout(context!!)
        newAreaContainer.orientation = LinearLayout.HORIZONTAL

        val creationTime = TextView(context!!)
        creationTime.text = request.creationTime.toString()
        newAreaContainer.addView(creationTime)

        val checkBox = CheckBox(context!!)
        checkBox.tag = AREA_CHECKBOX_TAG
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRequestIds.add(request)
            } else {
                selectedRequestIds.remove(request)
            }
            deleteOfflineAreasBtn.isEnabled = selectedRequestIds.isNotEmpty()
        }
        newAreaContainer.addView(checkBox)

        areasContainer.addView(newAreaContainer)
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

    private fun goToOfflineMap() {
        val offlineMapFragment = OfflineMapFragment()
        offlineMapFragment.arguments = packToBundle(requests)
        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, offlineMapFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
