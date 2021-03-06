package de.spover.spover

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import de.spover.spover.overlay.OverlayServiceHelper
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings

class NotificationListener : NotificationListenerService() {
    companion object {
        private var TAG = NotificationListener::class.java.simpleName
        private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }

    private lateinit var settings: SettingsStore
    private lateinit var overlayHelper: OverlayServiceHelper

    override fun onCreate() {
        super.onCreate()

        settings = SettingsStore(this)
        overlayHelper = OverlayServiceHelper(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isGmapsNavNotification(sbn)
                && settings.get(SpoverSettings.REOPEN_FLAG)) {
            overlayHelper.launchOverlayService()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (isGmapsNavNotification(sbn)) {
            setReopenFlag(true)
            overlayHelper.stopOverlayService()
        }
    }

    private fun isGmapsNavNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName == GOOGLE_MAPS_PACKAGE && !sbn.isClearable
    }

    private fun setReopenFlag(value: Boolean) {
        settings.set(SpoverSettings.REOPEN_FLAG, value)
    }
}