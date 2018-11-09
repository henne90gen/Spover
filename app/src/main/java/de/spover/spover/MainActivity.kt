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

    private lateinit var locationService: ILocationService

    private lateinit var overlayBtn: Button
    private lateinit var overlaySwitch: Switch
    private lateinit var speedSwitch: Switch
    private lateinit var speedLimitSwitch: Switch
    private lateinit var locationPermissionSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationService = LocationService(this)
        initUI()
    }

    private fun initUI() {
        locationPermissionSwitch = findViewById(R.id.switchLocationPermission)
        locationPermissionSwitch.setOnCheckedChangeListener { _, isChecked -> this.checkLocationPermission(isChecked) }
        checkLocationPermission(true)

        overlayBtn = findViewById(R.id.btnOverlay)
        overlayBtn.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                // Launch service right away - the user has already previously granted permission
                launchOverlayService()
            } else {
                Toast.makeText(this, "Please allow Spover to draw over other apps", Toast.LENGTH_LONG).show()
                // Check that the user has granted permission, and prompt them if not
                checkDrawOverlayPermission(true)
            }
        }

        val settings = SettingsStore(this)

        overlaySwitch = findViewById(R.id.switchOverlayPermission)
        overlaySwitch.isChecked = Settings.canDrawOverlays(this)
        overlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            checkDrawOverlayPermission(isChecked)
        }

        speedSwitch = findViewById(R.id.switchShowSpeed)
        speedSwitch.isChecked = settings.get(SpoverSettings.SHOW_CURRENT_SPEED)!!
        speedSwitch.setOnCheckedChangeListener { _, isChecked -> settings.set(SpoverSettings.SHOW_CURRENT_SPEED, isChecked) }

        speedLimitSwitch = findViewById(R.id.switchShowSpeedLimit)
        speedLimitSwitch.isChecked = settings.get(SpoverSettings.SHOW_SPEED_LIMIT)!!
        speedLimitSwitch.setOnCheckedChangeListener { _, isChecked -> settings.set(SpoverSettings.SHOW_SPEED_LIMIT, isChecked) }
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

    private fun launchOverlayService() {
        val overlayService = Intent(this, OverlayService::class.java)
        startService(overlayService)
        finish()
    }

    private fun checkDrawOverlayPermission(isChecked: Boolean) {
        if ((!Settings.canDrawOverlays(this) && isChecked)
                || (Settings.canDrawOverlays(this) && !isChecked)) {
            // launch Intent for settings to give overlay permission
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            overlaySwitch.isChecked = Settings.canDrawOverlays(this)
        }
    }

    companion object {
        private var TAG = MainActivity::class.java.simpleName
        const val OVERLAY_PERMISSION_REQUEST = 0
        const val LOCATION_PERMISSION_REQUEST = 1
    }
}
