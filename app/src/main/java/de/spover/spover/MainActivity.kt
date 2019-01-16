package de.spover.spover

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import de.spover.spover.fragments.SettingsFragment


class MainActivity : AppCompatActivity() {

    companion object {
        private var TAG = MainActivity::class.java.simpleName
        const val OVERLAY_PERMISSION_REQUEST = 0
        const val LOCATION_PERMISSION_REQUEST = 1
        const val NOTIFICATION_PERMISSION_REQUEST = 2
    }

    private lateinit var permissions: PermissionManager

    private lateinit var locationPermissionSwitch: Switch
    private lateinit var overlayPermissionSwitch: Switch
    private lateinit var notificationPermissionSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSettingsFragment(savedInstanceState)

        permissions = PermissionManager(this)
    }

    private fun initSettingsFragment(savedInstanceState: Bundle?) {
        // The fragment container has already been replaced by a fragment
        if (findViewById<FrameLayout>(R.id.fragmentContainer) == null) {
            return
        }

        // If we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return
        }

        val settingsFragment = SettingsFragment()

        // In case this activity was started with special instructions from an Intent,
        // pass the Intent's extras to the fragment as arguments
        settingsFragment.arguments = intent.extras

        val transaction = supportFragmentManager.beginTransaction()
        val settingsFragmentTag = "settingsFragmentTag"
        transaction.add(R.id.fragmentContainer, settingsFragment, settingsFragmentTag)
        transaction.commit()
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
