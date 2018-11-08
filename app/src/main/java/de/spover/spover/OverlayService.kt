package de.spover.spover

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.support.v4.view.GestureDetectorCompat
import android.util.Log
import android.view.*
import android.widget.FrameLayout

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
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT)

        params!!.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT// or Gravity.START
        params!!.x = 300
        params!!.y = 0

        val interceptorLayout = object : FrameLayout(this) {

            override fun dispatchKeyEvent(event: KeyEvent): Boolean {

                // Only fire on the ACTION_DOWN event, or you'll get two events (one for _DOWN, one for _UP)
                if (event.action == KeyEvent.ACTION_DOWN) {

                    // Check if the HOME button is pressed
                    if (event.keyCode == KeyEvent.KEYCODE_BACK) {

                        Log.v(TAG, "BACK Button Pressed")

                        // As we've taken action, we'll return true to prevent other apps from consuming the event as well
                        return true
                    }
                }

                // Otherwise don't intercept the event
                return super.dispatchKeyEvent(event)
            }
        }

        floatingView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.floating_view, interceptorLayout)
        floatingView!!.setOnTouchListener(this)
        windowManager!!.addView(floatingView, params)

        mDetector = GestureDetectorCompat(this, MyGestureListener(this))
    }

    override fun onDestroy() {

        super.onDestroy()

        if (floatingView != null) {
            windowManager!!.removeView(floatingView)
            floatingView = null
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        return mDetector.onTouchEvent(motionEvent)
    }

    private class MyGestureListener(var overlayService: OverlayService) : GestureDetector.OnGestureListener {

        override fun onSingleTapUp(p0: MotionEvent?): Boolean {
            // not needed
            return false
        }

        override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, distX: Float, distY: Float): Boolean {
            val params = overlayService.params
            params!!.x += distX.toInt()
            params!!.y -= distY.toInt()
            overlayService.windowManager!!.updateViewLayout(overlayService.floatingView, params)
            return true
        }

        override fun onLongPress(p0: MotionEvent?) {
            // not needed
        }

        override fun onShowPress(p0: MotionEvent?) {
            // not needed
        }

        override fun onDown(event: MotionEvent): Boolean {
            // not needed
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velX: Float, velY: Float): Boolean {
            // not needed
            return false
        }
    }

    companion object {
        private var TAG = OverlayService::class.java.simpleName
    }
}
