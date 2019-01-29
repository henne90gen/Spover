package de.spover.spover.overlay

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings

/**
 * Helper class that encapsulates logic to start, stop and restart the [OverlayService]
 */
class OverlayServiceHelper(val context: Context) {

    companion object {
        private var TAG = OverlayServiceHelper::class.java.simpleName
    }

    private var settings = SettingsStore(context)

    /**
     * Searches through all running services to find one, who's classname matches the classname of [OverlayService]
     */
    fun isOverlayServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            OverlayService::class.java.name == it.service.className
        }
    }

    /**
     * returns true if the [OverlayService] will display an UI, since it's not useful
     * to open a service without an UI and therefore no possibility to close it
     * (except in app settings)
     */
    fun willDisplayAnUI(): Boolean {
        return (settings.get(SpoverSettings.SHOW_CURRENT_SPEED)
                || settings.get(SpoverSettings.SHOW_SPEED_LIMIT))
    }

    /**
     * Starts [OverlayService] if all of the the following conditions are met:
     * - it is not already running (see [isOverlayServiceRunning])
     * - we have the permission to draw overlays (see [Settings.canDrawOverlays])
     * - we would be displaying an UI (see [willDisplayAnUI])
     */
    fun launchOverlayService() {
        if (!isOverlayServiceRunning()
                && Settings.canDrawOverlays(context)
                && willDisplayAnUI()) {
            Log.d(TAG, "started overlay service")
            context.startService(Intent(context, OverlayService::class.java))
        }
    }

    /**
     * Stops [OverlayService], if it is running
     */
    fun stopOverlayService() {
        if (isOverlayServiceRunning()) {
            Log.d(TAG, "stopped overlay service")
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    /**
     * Restarts the [OverlayService]
     */
    fun restartOverlayService() {
        stopOverlayService()
        launchOverlayService()
    }
}