package de.spover.spover

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.RelativeLayout

class OverlayService : Service(), View.OnTouchListener {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private lateinit var rlSpeed : RelativeLayout;
    private lateinit var rlSpeedLimit : RelativeLayout;

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"created")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addOverlayView()
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

        rlSpeed = floatingView!!.findViewById(R.id.rl_speed)
        rlSpeed.visibility = if (getPrefSpeedVisible()) View.VISIBLE else View.GONE

        rlSpeedLimit = floatingView!!.findViewById(R.id.rl_speed_limit)
        rlSpeedLimit.visibility = if (getPrefSpeedLimitVisible()) View.VISIBLE else View.GONE

        floatingView!!.setOnTouchListener(this)
        windowManager!!.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager!!.removeView(floatingView)
            floatingView = null
        }
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
    private fun shouldClose(y : Int): Boolean {
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

        when {
            motionEvent.action == MotionEvent.ACTION_DOWN -> {
                touchStartX = motionEvent.rawX
                touchStartY = motionEvent.rawY

                viewStartX = params!!.x
                viewStartY = params!!.y
            }
            motionEvent.action == MotionEvent.ACTION_MOVE -> {
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
            motionEvent.action == MotionEvent.ACTION_UP -> {
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
