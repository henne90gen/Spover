package de.spover.spover

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.view.View
import android.widget.ImageView

class OverlayWithHole(context: Context) : ImageView(context, null) {

    private val paint = Paint(ANTI_ALIAS_FLAG)
    private val clearMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    val cutout = RectF()

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        paint.color = resources.getColor(android.R.color.black, null)
        paint.alpha = 60
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawPaint(paint)

        cutout.left = width.toFloat() / 4.0f
        cutout.right = width - cutout.left
        cutout.top = height / 4.0f
        cutout.bottom = height - cutout.top

        paint.xfermode = clearMode
        canvas.drawRect(cutout, paint)
    }
}
