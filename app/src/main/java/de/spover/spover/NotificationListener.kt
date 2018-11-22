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

    private lateinit var settings: SettingsStore

    override fun onCreate() {
        super.onCreate()

        settings = SettingsStore(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Todo check what kind of notification had been posted
        if (isGmapsNavNotification(sbn)
                && shouldDisplayOverlay()
                && !isOverlayServiceRunning()) {
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

    /** ToDo refactor since same functionality is needed in NotificationListener
     * returns true if the overlay service displays an UI, since it's not useful
     * to open an overlay without an UI and therefore no possibility to close it
     */
    private fun shouldDisplayOverlay(): Boolean {
        return settings.get(SpoverSettings.SHOW_CURRENT_SPEED)
                && settings.get(SpoverSettings.SHOW_SPEED_LIMIT)
    }
}