package io.agora.example.familygame.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import io.agora.example.familygame.util.dp

class HanoiRing(context: Context, attr: AttributeSet? = null) : View(context, attr) {

    private val mPath = Path()
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var ringColor: Int = Color.RED
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mPaint.color = ringColor
        val w = measuredWidth - paddingLeft - paddingRight
        val h = measuredHeight - paddingBottom - paddingTop

        mPath.apply {
            reset()
            addRoundRect(
                paddingLeft.toFloat(), paddingTop.toFloat(),
                paddingLeft.toFloat() + w, h + paddingTop.toFloat(),
                h / 2f, h / 2f, Path.Direction.CW
            )
        }

        canvas?.let {
            it.drawPath(mPath, mPaint)
//            it.drawBitmap()
        }
    }
}