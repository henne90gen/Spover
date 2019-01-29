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
        private const val LIGHT_MODE_THRESHOLD = 70
        private const val DARK_MODE_THRESHOLD = 3
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    var lightMode = LightMode.BRIGHT

    init {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values[0] < DARK_MODE_THRESHOLD && lightMode != LightMode.DARK) {
            changeLightMode(LightMode.DARK)
        } else if (event.values[0] >= LIGHT_MODE_THRESHOLD && lightMode != LightMode.BRIGHT) {
            changeLightMode(LightMode.BRIGHT)
        }
    }

    private fun changeLightMode(newLightMode: LightMode) {
        lightMode = newLightMode
        Log.d(TAG, "Set light mode to $lightMode")
        callback()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // do nothing
    }

    fun destroy() {
        sensorManager.unregisterListener(this)
    }
}
