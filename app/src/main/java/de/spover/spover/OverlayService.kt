package de.spover.spover

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.support.v4.view.GestureDetectorCompat
import android.view.*

class OverlayService: Service(), View.OnTouchListener {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private lateinit var mDetector: GestureDetectorCompat


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
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
        params!!.x = 300
        params!!.y = 0


        floatingView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.floating_view, null)
        floatingView!!.setOnTouchListener(this)
        windowManager!!.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager!!.removeView(floatingView)
            floatingView = null
        }
    }

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var viewStartX = 0
    private var viewStartY = 0
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            touchStartX = motionEvent.rawX
            touchStartY = motionEvent.rawY

            viewStartX = params!!.x
            viewStartY = params!!.y
        } else if (motionEvent.action == MotionEvent.ACTION_MOVE) {
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
        return true
    }
}
