package de.spover.spover

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log

class OverlayServiceHelper(val context: Context) {

    companion object {
        private var TAG = OverlayServiceHelper::class.java.simpleName
    }

    private var settings = SettingsStore(context)

    fun isOverlayServiceRunning(): Boolean {
        // FixMe can probably be done with some fancy "any" lambda expression...
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (OverlayService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * returns true if the overlay service displays an UI, since it's not useful
     * to open an overlay without an UI and therefore no possibility to close it
     */
    fun displaysAnUI(): Boolean {
        return (settings.get(SpoverSettings.SHOW_CURRENT_SPEED)
                || settings.get(SpoverSettings.SHOW_SPEED_LIMIT))
    }

    fun launchOverlayService() {
        Log.d(TAG, "started overlay service")
        context.startService(Intent(context, OverlayService::class.java))
    }

    fun stopOverlayService() {
        Log.d(TAG, "stopped overlay service")
        context.stopService(Intent(context, OverlayService::class.java))
    }
}