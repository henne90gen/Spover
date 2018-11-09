package de.spover.spover

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

class OverlayService : Service(), View.OnTouchListener, SensorEventListener {

    enum class Mode {
        BRIGHT, DARK
    }

    private var lightMode = Mode.BRIGHT

    private lateinit var locationService: ILocationService

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    private lateinit var rlSpeed: RelativeLayout
    private lateinit var rlSpeedLimit: RelativeLayout
    private lateinit var ivSpeed: ImageView
    private lateinit var ivSpeedLimit: ImageView
    private lateinit var tvSpeed: TextView
    private lateinit var tvSpeedLimit: TextView

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "created")
        locationService = LocationService(this)
        locationService.registerLocationCallback {

        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initLightSensor()
        addOverlayView()
    }

    private fun initLightSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        Log.d(TAG, "Counted ${event.values[0]} photons")
        if (event.values[0] < 100 && lightMode != Mode.DARK) {
            lightMode = Mode.DARK
            adaptToLightMode(lightMode)
        } else {
            lightMode = Mode.BRIGHT
            adaptToLightMode(lightMode)
        }
    }

    private fun adaptToLightMode(mode: Mode) {
        if (mode == Mode.DARK) {
            ivSpeedLimit.setImageResource(R.drawable.ic_red_dark_icon)
            ivSpeed.setImageResource(R.drawable.ic_green_dark_icon)
            tvSpeed.setTextColor(getColor(R.color.colorTextLight))
            tvSpeedLimit.setTextColor(getColor(R.color.colorTextLight))
        } else {
            ivSpeedLimit.setImageResource(R.drawable.ic_red_icon)
            ivSpeed.setImageResource(R.drawable.ic_green_icon)
            tvSpeed.setTextColor(getColor(R.color.colorText))
            tvSpeedLimit.setTextColor(getColor(R.color.colorText))
        }
    }

    private fun addOverlayView() {
        params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)

        params!!.gravity = Gravity.TOP or Gravity.START// or Gravity.START
        val pos = getPrefPosition()
        params!!.x = pos.first
        params!!.y = pos.second

        floatingView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.floating_view, null)

        floatingView?.let {
            rlSpeed = it.findViewById(R.id.rl_speed)
            rlSpeed.visibility = if (getPrefSpeedVisible()) View.VISIBLE else View.GONE

            rlSpeed = it.findViewById(R.id.rl_speed)
            rlSpeed.visibility = if (getPrefSpeedVisible()) View.VISIBLE else View.GONE

            rlSpeedLimit = it.findViewById(R.id.rl_speed_limit)
            rlSpeedLimit.visibility = if (getPrefSpeedLimitVisible()) View.VISIBLE else View.GONE

            ivSpeed = it.findViewById(R.id.iv_speed)
            ivSpeedLimit = it.findViewById(R.id.iv_speed_limit)

            tvSpeed = it.findViewById(R.id.tv_speed)
            tvSpeedLimit = it.findViewById(R.id.tv_speed_limit)

            it.setOnTouchListener(this)
            windowManager!!.addView(floatingView, params)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager!!.removeView(floatingView)
            floatingView = null
        }
        sensorManager.unregisterListener(this)
        stopSelf()
    }

    private fun getPrefPosition(): Pair<Int, Int> {
        val preferences = applicationContext.getSharedPreferences(getString(R.string.pref_file_name), Context.MODE_PRIVATE)
        val x = preferences.getInt(getString(R.string.pref_overlay_x), 0)
        val y = preferences.getInt(getString(R.string.pref_overlay_y), 0)
        return Pair(x, y)
    }

    private fun storePrefPosition(x: Int, y: Int) {
        val preferences = applicationContext.getSharedPreferences(getString(R.string.pref_file_name), Context.MODE_PRIVATE)
        with(preferences.edit()) {
            putInt(getString(R.string.pref_overlay_x), x)
            putInt(getString(R.string.pref_overlay_y), y)
            apply()
        }
    }

    private fun getPrefSpeedVisible(): Boolean {
        val preferences = applicationContext.getSharedPreferences(getString(R.string.pref_file_name), Context.MODE_PRIVATE)
        return preferences.getBoolean(getString(R.string.pref_show_speed), true)
    }

    private fun getPrefSpeedLimitVisible(): Boolean {
        val preferences = applicationContext.getSharedPreferences(getString(R.string.pref_file_name), Context.MODE_PRIVATE)
        return preferences.getBoolean(getString(R.string.pref_show_speed_limit), true)
    }

    /**
     * returns if the overlay is close enough to the bottom so it should get closed
     *
     * @param y - y value of touch up position
     */
    private fun shouldClose(y: Int): Boolean {
        val displaySize = Point()
        windowManager!!.defaultDisplay.getSize(displaySize)
        val closedThresholdY = displaySize.y - floatingView!!.height
        Log.d(TAG, "threshold: $closedThresholdY, val: $y")
        return closedThresholdY - y <= 0
    }

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var viewStartX = 0
    private var viewStartY = 0
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val isEvent = { action: Int ->
            action == motionEvent.action
        }

        when {
            isEvent(MotionEvent.ACTION_DOWN) -> {
                touchStartX = motionEvent.rawX
                touchStartY = motionEvent.rawY

                viewStartX = params!!.x
                viewStartY = params!!.y
            }
            isEvent(MotionEvent.ACTION_MOVE) -> {
                val dX = motionEvent.rawX - touchStartX
                val dY = motionEvent.rawY - touchStartY

                params!!.x = (dX + viewStartX).toInt()
                params!!.y = (dY + viewStartY).toInt()

                try {
                    windowManager!!.updateViewLayout(floatingView, params)
                } catch (ignore: IllegalArgumentException) {
                    // FixMe
                }
            }
            isEvent(MotionEvent.ACTION_UP) -> {
                if (shouldClose(params!!.y)) {
                    onDestroy()
                } else {
                    storePrefPosition(params!!.x, params!!.y)
                }
            }
        }
        return true
    }

    companion object {
        private var TAG = OverlayService::class.java.simpleName
    }
}
