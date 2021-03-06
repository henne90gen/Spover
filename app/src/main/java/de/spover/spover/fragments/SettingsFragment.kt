package de.spover.spover.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.spover.spover.PermissionManager
import de.spover.spover.R
import de.spover.spover.overlay.OverlayServiceHelper
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings

class SettingsFragment : Fragment() {

    companion object {
        private var TAG = SettingsFragment::class.java.simpleName
        const val OVERLAY_PERMISSION_REQUEST = 0
        const val LOCATION_PERMISSION_REQUEST = 1
        const val NOTIFICATION_PERMISSION_REQUEST = 2
    }

    private lateinit var settings: SettingsStore
    private lateinit var permissions: PermissionManager
    private lateinit var overlayHelper: OverlayServiceHelper

    private lateinit var locationPermissionSwitch: Switch
    private lateinit var overlayPermissionSwitch: Switch
    private lateinit var notificationPermissionSwitch: Switch

    private lateinit var overlayBtn: Button
    private lateinit var speedSwitch: Switch
    private lateinit var speedLimitSwitch: Switch
    private lateinit var soundAlertSwitch: Switch
    private lateinit var speedThresholdET: EditText
    private lateinit var offlineAreasBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        settings = SettingsStore(context!!)
        permissions = PermissionManager(context!!)
        overlayHelper = OverlayServiceHelper(context!!)

        val rootView = inflater.inflate(R.layout.settings, container, false)
        return initUI(rootView)
    }

    private fun initUI(rootView: View): View {
        locationPermissionSwitch = rootView.findViewById(R.id.switchLocationPermission)
        locationPermissionSwitch.isChecked = permissions.canAccessLocation()
        locationPermissionSwitch.setOnCheckedChangeListener { _, isChecked -> this.checkLocationPermission(isChecked) }

        overlayPermissionSwitch = rootView.findViewById(R.id.switchOverlayPermission)
        overlayPermissionSwitch.isChecked = permissions.canDrawOverlays()
        overlayPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            checkDrawOverlayPermission(isChecked)
        }

        notificationPermissionSwitch = rootView.findViewById(R.id.switchNotificationPermission)
        notificationPermissionSwitch.isChecked = permissions.canReadNotifications()
        notificationPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            checkNotificationPermission(isChecked)
        }

        overlayBtn = rootView.findViewById(R.id.btnOverlay)
        overlayBtn.setOnClickListener(this::launchOverlay)

        speedSwitch = setupSettingsSwitch(rootView, R.id.switchShowSpeed, SpoverSettings.SHOW_CURRENT_SPEED)
        speedLimitSwitch = setupSettingsSwitch(rootView, R.id.switchShowSpeedLimit, SpoverSettings.SHOW_SPEED_LIMIT)
        soundAlertSwitch = setupSettingsSwitch(rootView, R.id.switchSoundAlert, SpoverSettings.SOUND_ALERT)

        speedThresholdET = rootView.findViewById(R.id.etWarningThreshold)
        speedThresholdET.setText("${settings.get(SpoverSettings.SPEED_THRESHOLD)}")
        speedThresholdET.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(c: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(c: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(c: Editable) {
                val value = c.toString().toIntOrNull()
                if (value != null) {
                    settings.set(SpoverSettings.SPEED_THRESHOLD, value)
                }
            }
        })

        offlineAreasBtn = rootView.findViewById(R.id.btnOfflineAreas)
        offlineAreasBtn.setOnClickListener {
            goToOfflineMap()
        }

        return rootView
    }

    private fun launchOverlay(view: View) {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, getString(R.string.toast_overlay_perm_missing), Toast.LENGTH_LONG).show()
            return
        }
        if (!overlayHelper.willDisplayAnUI()) {
            Toast.makeText(context, getString(R.string.toast_no_ui_visible), Toast.LENGTH_LONG).show()
            return
        }

        overlayHelper.launchOverlayService()
    }

    private fun goToOfflineMap() {
        val offlineMapFragment = OfflineMapFragment()
        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, offlineMapFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun setupSettingsSwitch(rootView: View, id: Int, setting: SpoverSettings<Boolean>): Switch {
        val switch = rootView.findViewById<Switch>(id)
        switch.isChecked = settings.get(setting)
        switch.setOnCheckedChangeListener { _, isChecked ->
            settings.set(setting, isChecked)
            if (overlayHelper.isOverlayServiceRunning()) {
                overlayHelper.restartOverlayService()
            }
        }
        return switch
    }

    private fun checkLocationPermission(isChecked: Boolean) {
        if (!permissions.canAccessLocation() && isChecked) {
            Log.e(TAG, "Did not grant location permission")
            locationPermissionSwitch.isChecked = false
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        } else if (permissions.canAccessLocation() && !isChecked) {
            // ToDo maybe it's possible to open the specific settings activity with an intent
            Toast.makeText(context, getString(R.string.toast_remove_location_access), Toast.LENGTH_LONG).show()
            locationPermissionSwitch.isChecked = permissions.canAccessLocation()
        }
    }

    private fun checkNotificationPermission(isChecked: Boolean) {
        if (permissions.canReadNotifications().xor(isChecked)) {
            startActivityForResult(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    private fun checkDrawOverlayPermission(isChecked: Boolean) {
        if (permissions.canDrawOverlays().xor(isChecked)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context!!.packageName}"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // double check in case the user hasn't given the permission in the settings activity
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            overlayPermissionSwitch.isChecked = permissions.canDrawOverlays()
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            notificationPermissionSwitch.isChecked = permissions.canReadNotifications()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            locationPermissionSwitch.isChecked =
                    (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }
}
