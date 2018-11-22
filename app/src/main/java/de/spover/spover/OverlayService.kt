package de.spover.spover

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import kotlin.math.roundToInt

class OverlayService : Service(), View.OnTouchListener {

    companion object {
        private var TAG = OverlayService::class.java.simpleName
    }

    private lateinit var settingsStore: SettingsStore
    private lateinit var locationService: LocationService
    private lateinit var lightService: LightService

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    private var params: WindowManager.LayoutParams? = null
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

        settingsStore = SettingsStore(this)

        locationService = LocationService(this) {
            tvSpeed.text = formatSpeed(it)
        }

        lightService = LightService(this, this::adaptToLightMode)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addOverlayView()
    }

    private fun formatSpeed(speedInMetersPerSecond: Double): String {
        val speedInKilometersPerHour: Int = (speedInMetersPerSecond * 3.6).roundToInt()
        return speedInKilometersPerHour.toString()
    }

    private fun addOverlayView() {
        params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // FLAG_SHOW_WHEN_LOCKED is deprecated, but the new method is only available for activities and the overlay is a service
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT)

        params!!.gravity = Gravity.TOP or Gravity.START

        if (settingsStore.get(SpoverSettings.FIRST_LAUNCH)) {
            setFirstLaunchPos()
        }

        params!!.x = settingsStore.get(SpoverSettings.OVERLAY_X)
        params!!.y = settingsStore.get(SpoverSettings.OVERLAY_Y)

        floatingView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.floating_view, null)

        floatingView?.let {
            rlSpeed = it.findViewById(R.id.rl_speed)
            rlSpeed.visibility = if (isPrefSpeedVisible()) View.VISIBLE else View.GONE

            rlSpeedLimit = it.findViewById(R.id.rl_speed_limit)
            rlSpeedLimit.visibility = if (isPrefSpeedLimitVisible()) View.VISIBLE else View.GONE

            ivSpeed = it.findViewById(R.id.iv_speed)
            ivSpeedLimit = it.findViewById(R.id.iv_speed_limit)

            tvSpeed = it.findViewById(R.id.tv_speed)
            tvSpeedLimit = it.findViewById(R.id.tv_speed_limit)

            it.setOnTouchListener(this)
            windowManager?.addView(floatingView, params)
        }
    }

    /**
     * set overlay position on first launch to somewhere right around vertical center
     */
    private fun setFirstLaunchPos() {
        val displaySize = Point()
        windowManager!!.defaultDisplay.getSize(displaySize)
        val y = (displaySize.y) / 2
        val x = (displaySize.x)
        storePrefPosition(x, y)
        settingsStore.set(SpoverSettings.FIRST_LAUNCH, false)
    }

    private fun adaptToLightMode(mode: LightMode) {
        if (mode == LightMode.DARK) {
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

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager!!.removeView(floatingView)
            floatingView = null
        }
        lightService.destroy()
        stopSelf()
    }

    private fun storeReopenFlag(value: Boolean) {
        settingsStore.set(SpoverSettings.REOPEN_FLAG, false)
    }


    private fun storePrefPosition(x: Int, y: Int) {
        settingsStore.set(SpoverSettings.OVERLAY_X, x)
        settingsStore.set(SpoverSettings.OVERLAY_Y, y)
    }

    private fun isPrefSpeedVisible(): Boolean {
        return settingsStore.get(SpoverSettings.SHOW_CURRENT_SPEED)
    }

    private fun isPrefSpeedLimitVisible(): Boolean {
        return settingsStore.get(SpoverSettings.SHOW_SPEED_LIMIT)
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
                    storeReopenFlag(false)
                } else {
                    storePrefPosition(params!!.x, params!!.y)
                }
            }
        }
        return true
    }
}
