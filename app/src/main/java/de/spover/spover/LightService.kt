package de.spover.spover

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

enum class LightMode {
    BRIGHT,
    DARK
}

typealias LightCallback = () -> Unit

class LightService(context: Context, val callback: LightCallback) : SensorEventListener {
    companion object {
        private var TAG = LightService::class.java.simpleName
        // light sensor value in Lux which has to be exceeded or undercut
        private const val LIGHT_MODE_THRESHOLD = 13
        private const val DARK_MODE_THRESHOLD = 3
        // min time between changing the theme again
        private const val CHANGE_THEME_TIMEOUT = 15
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    var lightMode = LightMode.BRIGHT
    private var lastTimePlayed = System.currentTimeMillis()


    init {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent) {
        Log.d(TAG, "light value: ${event.values[0]}")
        val timeDiff = System.currentTimeMillis() - lastTimePlayed
        val timeoutExceeded = timeDiff > (CHANGE_THEME_TIMEOUT * 1000)

        if (timeoutExceeded && event.values[0] < DARK_MODE_THRESHOLD && lightMode != LightMode.DARK) {
            lightMode = LightMode.DARK
            lastTimePlayed = System.currentTimeMillis()
            Log.d(TAG, "Set light mode to $lightMode")
            callback()
        } else if (timeoutExceeded && event.values[0] >= LIGHT_MODE_THRESHOLD && lightMode != LightMode.BRIGHT) {
            lightMode = LightMode.BRIGHT
            lastTimePlayed = System.currentTimeMillis()
            Log.d(TAG, "Set light mode to $lightMode")
            callback()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    fun destroy() {
        sensorManager.unregisterListener(this)
    }
}
