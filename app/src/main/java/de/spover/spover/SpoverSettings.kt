package de.spover.spover

class SpoverSettings<T> private constructor(val defaultValue: T, val name: String) {
    companion object {
        val FIRST_LAUNCH = SpoverSettings(true, "FIRST_LAUNCH")
        val SHOW_CURRENT_SPEED = SpoverSettings(true, "SHOW_CURRENT_SPEED")
        val SHOW_SPEED_LIMIT = SpoverSettings(true, "SHOW_SPEED_LIMIT")
        val OVERLAY_X = SpoverSettings(0, "OVERLAY_X")
        val OVERLAY_Y = SpoverSettings(0, "OVERLAY_Y")
        val REOPEN_FLAG = SpoverSettings(true, "REOPEN_FLAG")
        val SPEED_THRESHOLD = SpoverSettings(0, "SPEED_THRESHOLD")
        val SOUND_ALERT = SpoverSettings(false, "SOUND_ALERT")
    }
}