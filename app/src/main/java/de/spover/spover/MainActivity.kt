package de.spover.spover

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val tag = LocationService::class.java.simpleName

    private lateinit var locationService: ILocationService
    private lateinit var latText: TextView
    private lateinit var lonText: TextView
    private lateinit var permissionSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationService = LocationService(this)
        latText = findViewById(R.id.lat)
        lonText = findViewById(R.id.lon)
        permissionSwitch = findViewById(R.id.locationPermissionSwitch)
        permissionSwitch.setOnCheckedChangeListener { _, isChecked -> this.checkLocationPermission(isChecked) }
        checkLocationPermission(true)
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
}
