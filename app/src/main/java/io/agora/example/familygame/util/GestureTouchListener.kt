package io.agora.example.familygame.util

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

class GestureTouchListener(val callback: (Float, Float) -> Unit) : View.OnTouchListener {
    private val mHandler = Handler(Looper.getMainLooper())
    private var enableTouch = false

    // 一次调用，走多少像素
    private val controlDistance = 50
    // 多少ms回调一次
    private val controlSpeedInMs = 100L
    private var startTouchPoint: PointF = PointF()
    private var endTouchPoint: PointF = PointF()
    private var lastCallbackTime = 0L

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?.let {
            when (it.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    enableTouch = true
                    startTouchPoint.x = it.rawX
                    startTouchPoint.y = it.rawY
                    updatePos()
                }
                MotionEvent.ACTION_MOVE -> {
                    endTouchPoint.x = it.rawX
                    endTouchPoint.y = it.rawY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    enableTouch = false
                }
                else -> {}
            }
        }
        return true
    }

    private fun updatePos() {
        if(!enableTouch) return
        else if(SystemClock.uptimeMillis() - lastCallbackTime > 0L){
            lastCallbackTime = SystemClock.uptimeMillis()
            doUpdatePos()
            mHandler.postDelayed({ updatePos() }, controlSpeedInMs)
        }

    }

    private fun doUpdatePos(){
        val diffX = endTouchPoint.x - startTouchPoint.x
        val diffY = endTouchPoint.y - startTouchPoint.y

        if (diffX == 0f && diffY == 0f)
            return
        else {
            val distance = sqrt(diffX * diffX + diffY * diffY)
            val fraction = (distance / controlDistance).let {
                if (it >= 1f) it
                else 1f
            }
            callback(diffX / fraction, diffY / fraction)
        }
    }


}