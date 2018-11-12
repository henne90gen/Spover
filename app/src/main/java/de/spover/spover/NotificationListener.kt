package de.spover.spover

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.app.ActivityManager
import android.content.Context


class NotificationListener : NotificationListenerService() {
    companion object {
        private var TAG = NotificationListener::class.java.simpleName
        private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Implement what you want here
        if (isGmapsNavNotification(sbn) && !isOverlayServiceRunning()) {
            launchOverlayService()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (isOverlayServiceRunning()) {
            stopOverlayService()
        }
    }

    private fun isGmapsNavNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName == GOOGLE_MAPS_PACKAGE
    }

    private fun launchOverlayService() {
        Log.d(TAG, "detected Gmaps nav start, started overlay")
        startService(Intent(this, OverlayService::class.java))
    }

    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun isOverlayServiceRunning(): Boolean {
        // FixMe can probably be done with some fancy "any" lambda expression...
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (OverlayService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}