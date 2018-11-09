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

typealias LightCallback = (LightMode) -> Unit

class LightService(context: Context, val callback: LightCallback) : SensorEventListener {
    companion object {
        private var TAG = LightService::class.java.simpleName
        private const val LIGHT_MODE_THRESHOLD = 70
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var lightMode = LightMode.BRIGHT

    init {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values[0] < LIGHT_MODE_THRESHOLD && lightMode != LightMode.DARK) {
            lightMode = LightMode.DARK
            Log.d(TAG, "Set light mode to $lightMode")
            callback(lightMode)
        } else if (event.values[0] >= LIGHT_MODE_THRESHOLD && lightMode != LightMode.BRIGHT) {
            lightMode = LightMode.BRIGHT
            Log.d(TAG, "Set light mode to $lightMode")
            callback(lightMode)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    fun destroy() {
        sensorManager.unregisterListener(this)
    }
}
