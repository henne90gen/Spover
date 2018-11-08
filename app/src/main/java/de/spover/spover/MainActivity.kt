package de.spover.spover

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.provider.Settings
import android.widget.*

class MainActivity : AppCompatActivity() {

    private val tag = LocationService::class.java.simpleName

    private lateinit var locationService: ILocationService
    private lateinit var latText: TextView
    private lateinit var lonText: TextView
    private lateinit var permissionSwitch: Switch
    private lateinit var overlayBtn: Button

    private lateinit var speedSwitch: Switch
    private lateinit var speedLimitSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationService = LocationService(this)
        latText = findViewById(R.id.lat)
        lonText = findViewById(R.id.lon)
        permissionSwitch = findViewById(R.id.switchLocationPermission)
        permissionSwitch.setOnCheckedChangeListener { _, isChecked -> this.checkLocationPermission(isChecked) }
        checkLocationPermission(true)

        overlayBtn = findViewById(R.id.btnOverlay)
        overlayBtn.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                // Launch service right away - the user has already previously granted permission
                launchOverlayService()
            } else {
                // Check that the user has granted permission, and prompt them if not
                checkDrawOverlayPermission()
            }
        }
        initUI()
    }

    private fun initUI() {
        val preferences = this.getPreferences(Context.MODE_PRIVATE)

        speedSwitch = findViewById(R.id.switch_show_speed)
        speedSwitch.isChecked =  preferences.getBoolean(getString(R.string.pref_show_speed), true)
        speedSwitch.setOnClickListener {
            with (preferences.edit()) {
                putBoolean(getString(R.string.pref_show_speed), speedSwitch.isChecked)
                apply()
            }
        }

        speedLimitSwitch = findViewById(R.id.switch_show_speed_limit)
        speedLimitSwitch.isChecked =  preferences.getBoolean(getString(R.string.pref_show_speed_limit), true)
        speedLimitSwitch.setOnClickListener {
            with (preferences.edit()) {
                putBoolean(getString(R.string.pref_show_speed_limit), speedLimitSwitch.isChecked)
                apply()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 0) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                permissionSwitch.isChecked = true
            } else {
                Log.e(tag, "Location permission not granted")
            }
        }
    }

    private fun checkLocationPermission(isChecked: Boolean) {
        Log.e(tag, "Location permission switch: $isChecked")
        if (!isChecked) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(tag, "Did not grant location permission")
            permissionSwitch.isChecked = false
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.e(tag, "We should show permission rationale, but I don't know what that means")
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        0)
            }
        } else {
            permissionSwitch.isChecked = true
        }
    }

    fun setLocation(view: View) {
        locationService.fetchLocation { loc ->
            if (loc != null) {
                latText.text = "${loc.latitude}"
                lonText.text = "${loc.longitude}"
            } else {
                latText.text = "Could not fetch location"
                lonText.text = "Could not fetch location"
            }
        }
    }

    private fun launchOverlayService() {
        val overlayService = Intent(this, OverlayService::class.java)
        startService(overlayService)
        finish()
    }

    private fun checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            // launch Intent for settings to give overlay permission
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                launchOverlayService()
            } else {
                Toast.makeText(this, "Sorry. Can't draw overlays without permission...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private var TAG = MainActivity::class.java.simpleName
        const val REQUEST_CODE = 10101
    }
}
