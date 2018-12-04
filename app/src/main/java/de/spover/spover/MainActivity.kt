package de.spover.spover

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import de.spover.spover.network.BoundingBox
import de.spover.spover.network.OpenStreetMapsClient
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings


class MainActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = SettingsStore(this)
        permissions = PermissionManager(this)
        overlayHelper = OverlayServiceHelper(this)

        OpenStreetMapsClient.scheduleBoundingBoxFetching(
                this,
                BoundingBox(51.6655, 14.7248, 51.6681, 14.7321)
        )

        initUI()
    }

    private fun initUI() {
        locationPermissionSwitch = findViewById(R.id.switchLocationPermission)
        locationPermissionSwitch.setOnCheckedChangeListener { _, isChecked -> this.checkLocationPermission(isChecked) }
        checkLocationPermission(true)

        overlayPermissionSwitch = findViewById(R.id.switchOverlayPermission)
        overlayPermissionSwitch.isChecked = permissions.canDrawOverlays()
        overlayPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            checkDrawOverlayPermission(isChecked)
        }

        notificationPermissionSwitch = findViewById(R.id.switchNotificationPermission)
        notificationPermissionSwitch.isChecked = permissions.canReadNotifications()
        notificationPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            checkNotificationPermission(isChecked)
        }

        overlayBtn = findViewById(R.id.btnOverlay)
        overlayBtn.setOnClickListener {
            if (Settings.canDrawOverlays(this) && overlayHelper.displaysAnUI()) {
                overlayHelper.launchOverlayService()
            } else if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, getString(R.string.toast_overlay_perm_missing), Toast.LENGTH_LONG).show()
            } else if (!overlayHelper.displaysAnUI()) {
                Toast.makeText(this, getString(R.string.toast_no_ui_visible), Toast.LENGTH_LONG).show()
            }
        }

        speedSwitch = setupSettingsSwitch(R.id.switchShowSpeed, SpoverSettings.SHOW_CURRENT_SPEED)
        speedLimitSwitch = setupSettingsSwitch(R.id.switchShowSpeedLimit, SpoverSettings.SHOW_SPEED_LIMIT)
        soundAlertSwitch = setupSettingsSwitch(R.id.switchSoundAlert, SpoverSettings.SOUND_ALERT)

        speedThresholdET = findViewById(R.id.etWarningThreshold)
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
    }

    private fun setupSettingsSwitch(id: Int, setting: SpoverSettings<Boolean>): Switch {
        val switch = findViewById<Switch>(id)
        switch.isChecked = settings.get(setting)
        switch.setOnCheckedChangeListener { _, isChecked ->
            settings.set(setting, isChecked)
            overlayHelper.restartOverlayService()
        }
        return switch
    }

    private fun checkLocationPermission(isChecked: Boolean) {
        if (!permissions.canAccessLocation() && isChecked) {
            Log.e(TAG, "Did not grant location permission")
            locationPermissionSwitch.isChecked = false
            // if permission got denied the first time the next time the permission dialog opens
            // a "never ask again" option appears. That case can somehow get covered by PermissionRationale
            //if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) { } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        } else if (permissions.canAccessLocation() && !isChecked) {
            // ToDo maybe it's possible to open the specific settings activity with an intent
            Toast.makeText(this, getString(R.string.toast_remove_location_access), Toast.LENGTH_LONG).show()
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
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
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


    companion object {
        private var TAG = MainActivity::class.java.simpleName
        const val OVERLAY_PERMISSION_REQUEST = 0
        const val LOCATION_PERMISSION_REQUEST = 1
        const val NOTIFICATION_PERMISSION_REQUEST = 2
    }
}
