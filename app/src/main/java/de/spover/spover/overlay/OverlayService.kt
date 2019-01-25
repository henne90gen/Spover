package de.spover.spover.overlay

import android.annotation.SuppressLint
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
import de.spover.spover.*
import de.spover.spover.settings.SettingsStore
import de.spover.spover.settings.SpoverSettings
import de.spover.spover.speedlimit.SpeedLimitService
import de.spover.spover.speedlimit.SpeedMode
import kotlin.math.roundToInt

class OverlayService : Service(), View.OnTouchListener {

    companion object {
        private var TAG = OverlayService::class.java.simpleName
    }

    private lateinit var settingsStore: SettingsStore
    private lateinit var locationService: LocationService
    private lateinit var speedLimitService: SpeedLimitService
    private lateinit var lightService: LightService

    private lateinit var windowManager: WindowManager

    private var floatingView: View? = null
    private lateinit var floatingViewParams: WindowManager.LayoutParams
    private lateinit var rlSpeed: RelativeLayout
    private lateinit var rlSpeedLimit: RelativeLayout
    private lateinit var ivSpeed: ImageView
    private lateinit var ivSpeedLimit: ImageView
    private lateinit var tvSpeed: TextView

    private var destroyOverlayView: View? = null
    private lateinit var destroyViewParams: WindowManager.LayoutParams
    private lateinit var destroyIcon: ImageView

    private var soundManager: SoundManager = SoundManager.getInstance()

    private lateinit var tvSpeedLimit: TextView

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var viewStartX = 0
    private var viewStartY = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        settingsStore = SettingsStore(this)

        speedLimitService = SpeedLimitService(this, this::setSpeedLimit, this::adaptUIToChangedEnvironment)
        locationService = LocationService(this, this::updateSpeed, speedLimitService::updateCurrentLocation)

        lightService = LightService(this, this::adaptUIToChangedEnvironment)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        soundManager.loadSound(this)

        addOverlayView()
    }

    private fun updateSpeed(speedInMetersPerSecond: Double) {
        val speedInKilometersPerHour: Int = (speedInMetersPerSecond * 3.6).roundToInt()
        speedLimitService.updateSpeedMode(speedInKilometersPerHour)
        adaptUIToChangedEnvironment()
        tvSpeed.text = speedInKilometersPerHour.toString()
    }

    // OSM provides speed limits in km/h
    private fun setSpeedLimit(speedLimitText: String) {
        tvSpeedLimit.text = speedLimitText
    }

    @Suppress("DEPRECATION")
    @SuppressLint("InflateParams")
    private fun addOverlayView() {
        floatingViewParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // FLAG_SHOW_WHEN_LOCKED is deprecated, but the new method is only available for activities and the overlay is a service
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT)

        floatingViewParams.apply {
            gravity = Gravity.TOP or Gravity.START

            if (settingsStore.get(SpoverSettings.FIRST_LAUNCH)) {
                setFirstLaunchPos()
            }

            x = settingsStore.get(SpoverSettings.OVERLAY_X)
            y = settingsStore.get(SpoverSettings.OVERLAY_Y)
        }

        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = layoutInflater.inflate(R.layout.floating_view, null)

        val overlayService = this
        floatingView?.apply {
            rlSpeed = findViewById(R.id.rl_speed)
            rlSpeed.visibility = if (isPrefSpeedVisible()) View.VISIBLE else View.GONE

            rlSpeedLimit = findViewById(R.id.rl_speed_limit)
            rlSpeedLimit.visibility = if (isPrefSpeedLimitVisible()) View.VISIBLE else View.GONE

            ivSpeed = findViewById(R.id.iv_speed)
            ivSpeedLimit = findViewById(R.id.iv_speed_limit)

            tvSpeed = findViewById(R.id.tv_speed)
            tvSpeedLimit = findViewById(R.id.tv_speed_limit)

            setOnTouchListener(overlayService)
            windowManager.addView(this, floatingViewParams)
        }

        // FLAG_SHOW_WHEN_LOCKED is deprecated, but the new method is only available for activities and the overlay is a service
        val flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        destroyViewParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
        )


        destroyOverlayView = layoutInflater.inflate(R.layout.destroy_overlay_view, null)
        destroyOverlayView?.apply {
            destroyIcon = findViewById(R.id.destroy_icon)
            destroyIcon.visibility = View.INVISIBLE
            destroyIcon.isClickable = false

            windowManager.addView(this, destroyViewParams)
        }
    }

    /**
     * set overlay position on first launch to somewhere right around vertical center
     */
    private fun setFirstLaunchPos() {
        val displaySize = Point()
        windowManager.defaultDisplay.getSize(displaySize)
        val y = (displaySize.y) / 2
        val x = (displaySize.x)
        storePrefPosition(x, y)
        settingsStore.set(SpoverSettings.FIRST_LAUNCH, false)
    }


    /**
     * change the overlay colors depending on:
     *     1) the current brightness mode
     *     2) the current speed mode (computed through speed limit, speed and warning threshold
     */
    private fun adaptUIToChangedEnvironment() {
        val lightMode = lightService.lightMode
        val speedMode = speedLimitService.speedMode

        if (lightMode == LightMode.DARK) {
            when (speedMode) {
                SpeedMode.GREEN -> ivSpeed.setImageResource(R.drawable.ic_green_dark_icon)
                SpeedMode.YELLOW -> ivSpeed.setImageResource(R.drawable.ic_yellow_dark_icon)
                SpeedMode.RED -> {
                    ivSpeed.setImageResource(R.drawable.ic_red_dark_icon)
                    if (settingsStore.get(SpoverSettings.SOUND_ALERT)) soundManager.play()
                }
            }
            ivSpeedLimit.setImageResource(R.drawable.ic_red_dark_icon)
            tvSpeed.setTextColor(getColor(R.color.colorTextLight))
            tvSpeedLimit.setTextColor(getColor(R.color.colorTextLight))
        } else {
            when (speedMode) {
                SpeedMode.GREEN -> ivSpeed.setImageResource(R.drawable.ic_green_icon)
                SpeedMode.YELLOW -> ivSpeed.setImageResource(R.drawable.ic_yellow_icon)
                SpeedMode.RED -> {
                    ivSpeed.setImageResource(R.drawable.ic_red_icon)
                    if (settingsStore.get(SpoverSettings.SOUND_ALERT)) soundManager.play()
                }
            }
            ivSpeedLimit.setImageResource(R.drawable.ic_red_icon)
            tvSpeed.setTextColor(getColor(R.color.colorText))
            tvSpeedLimit.setTextColor(getColor(R.color.colorText))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying overlay")

        if (floatingView != null) {
            windowManager.removeView(floatingView)
            floatingView = null
        }

        if (destroyOverlayView != null) {
            windowManager.removeView(destroyOverlayView)
            destroyOverlayView = null
        }

        locationService.unregisterLocationUpdates()
        lightService.destroy()
        stopSelf()
    }

    private fun storeReopenFlag(value: Boolean) {
        settingsStore.set(SpoverSettings.REOPEN_FLAG, value)
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
     * @param y - y value of touch up position
     */
    private fun shouldClose(x: Float, y: Float): Boolean {
        val iconPadding = 25
        val iconX = destroyIcon.x - iconPadding
        val iconY = destroyIcon.y - iconPadding
        val iconWidth = destroyIcon.width + 2 * iconPadding
        val iconHeight = destroyIcon.height + 2 * iconPadding

        Log.d(TAG, "X: $x, Y: $y, IconX: $iconX, IconY: $iconY, IconWidth: $iconWidth, IconHeight: $iconHeight")

        if (x > iconX && x < iconX + iconWidth) {
            if (y > iconY && y < iconY + iconHeight) {
                return true
            }
        }
        return false
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        floatingView?.performClick()

        val isEvent = { action: Int ->
            action == motionEvent.action
        }

        when {
            isEvent(MotionEvent.ACTION_DOWN) -> {
                touchStartX = motionEvent.rawX
                touchStartY = motionEvent.rawY

                viewStartX = floatingViewParams.x
                viewStartY = floatingViewParams.y

                destroyIcon.visibility = View.VISIBLE
            }
            isEvent(MotionEvent.ACTION_MOVE) -> {
                val dX = motionEvent.rawX - touchStartX
                val dY = motionEvent.rawY - touchStartY

                floatingViewParams.x = (dX + viewStartX).toInt()
                floatingViewParams.y = (dY + viewStartY).toInt()

                try {
                    windowManager.updateViewLayout(floatingView, floatingViewParams)
                } catch (ignore: IllegalArgumentException) {
                    // FIXME add logging and user feedback
                }
            }
            isEvent(MotionEvent.ACTION_UP) -> {
                destroyIcon.visibility = View.INVISIBLE

                if (shouldClose(motionEvent.rawX, motionEvent.rawY)) {
                    onDestroy()
                    storeReopenFlag(false)
                } else {
                    storePrefPosition(floatingViewParams.x, floatingViewParams.y)
                }
            }
        }
        return true
    }
}
