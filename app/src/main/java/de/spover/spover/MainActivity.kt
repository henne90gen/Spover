package de.spover.spover

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsStore
    private lateinit var permissions : PermissionManager

    private lateinit var overlayBtn: Button
    private lateinit var speedSwitch: Switch
    private lateinit var speedLimitSwitch: Switch
    private lateinit var soundAlertSwitch: Switch

    private lateinit var locationPermissionSwitch: Switch
    private lateinit var overlayPermissionSwitch: Switch
    private lateinit var notificationPermissionSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = SettingsStore(this)
        permissions = PermissionManager(this)
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
            if (Settings.canDrawOverlays(this) && shouldDisplayOverlay()) {
                launchOverlayService()
            } else if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please allow Spover to draw over other apps", Toast.LENGTH_LONG).show()
            } else if (!shouldDisplayOverlay()) {
                Toast.makeText(this, "All UI elements got disabled, enable showing speed or speed limit first", Toast.LENGTH_LONG).show()
            }
        }

        speedSwitch = setupSettingsSwitch(R.id.switchShowSpeed, SpoverSettings.SHOW_CURRENT_SPEED)
        speedLimitSwitch = setupSettingsSwitch(R.id.switchShowSpeedLimit, SpoverSettings.SHOW_SPEED_LIMIT)
        soundAlertSwitch = setupSettingsSwitch(R.id.switchSoundAlert, SpoverSettings.SOUND_ALERT)
    }

    private fun setupSettingsSwitch(id: Int, setting: SpoverSettings<Boolean>): Switch {
        val switch = findViewById<Switch>(id)
        switch.isChecked = settings.get(setting)
        switch.setOnCheckedChangeListener { _, isChecked -> settings.set(setting, isChecked) }
        return switch
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                locationPermissionSwitch.isChecked = true
            } else {
                Log.e(TAG, "Location permission not granted")
            }
        }
    }

    private fun checkLocationPermission(isChecked: Boolean) {
        Log.e(TAG, "Location permission switch: $isChecked")
        if (!isChecked) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Did not grant location permission")
            locationPermissionSwitch.isChecked = false
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.e(TAG, "We should show permission rationale, but I don't know what that means")
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
            }
        } else {
            locationPermissionSwitch.isChecked = true
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

    private fun launchOverlayService() {
        val overlayService = Intent(this, OverlayService::class.java)
        startService(overlayService)
        finish()
    }

    /** ToDo refactor since same functionality is needed in NotificationListener
     * returns true if the overlay service displays an UI, since it's not useful
     * to open an overlay without an UI and therefore no possibility to close it
     */
    private fun shouldDisplayOverlay(): Boolean {
        return settings.get(SpoverSettings.SHOW_CURRENT_SPEED)
                || settings.get(SpoverSettings.SHOW_SPEED_LIMIT)
    }


    companion object {
        private var TAG = MainActivity::class.java.simpleName
        const val OVERLAY_PERMISSION_REQUEST = 0
        const val LOCATION_PERMISSION_REQUEST = 1
        const val NOTIFICATION_PERMISSION_REQUEST = 2
    }
}
