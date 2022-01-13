package io.agora.example.familygame.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.annotation.IntegerRes
import io.agora.example.familygame.util.dp
import io.agora.example.familygame.util.log
import kotlin.math.pow

/**
 * @Author: liuqiang
 * @Date: 2022/1/6 23:35
 * @Description:
 */
class Hanoi(context: Context, attr: AttributeSet?) : FrameLayout(context, attr) {
    companion object {
        // 底座厚度
        const val baseBottomHeight = 20

        // 3D视角下梯形偏移量
        const val baseBottomOffset = 20

        //
        const val stickRadius = 10
        const val stickHeight = 80
    }

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val basePath = Path()
    private val baseStickPath = Path()

    var hanoiValue: Int = 0x1c0
        set(value) {
            field = value
            rearrange()
        }

    private val ringList = listOf(HanoiRing(context), HanoiRing(context), HanoiRing(context))
    private var currentMovingRing: HanoiRing? = null

    private var draggable = false
    private var currentOpStackPos = 0
    private var currentRingPosInList = -1

    private var lastTouchPointF = PointF()

    var finishListener: ((Boolean) -> Unit)? = null

    init {
        setWillNotDraw(false)
        ringList.forEachIndexed { index, hanoiRing ->
            addView(hanoiRing, LayoutParams(((30 + 20 * (index + 1)).dp.toInt()), 20.dp.toInt()))
            hanoiRing.ringColor = when (index) {
                0 -> Color.GREEN
                1 -> Color.BLUE
                else -> Color.RED
            }
        }
        setOnLongClickListener {
            // Vibrate
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            calculateMovingRing()
            // animate
            currentMovingRing?.animate()?.translationY(20.dp)?.setDuration(200)?.withEndAction { draggable = true }?.start()
            false
        }
        rearrange()
    }

    private fun rearrange() {
        // 杆子 x 坐标
        val partWidth = (measuredWidth - paddingStart - paddingRight) / 6f

        val startY =
            measuredHeight - paddingBottom - 1.4f * baseBottomOffset.dp - ringList[0].measuredHeight

        for (i in 0..2) {
            reLayoutRing(i, partWidth + partWidth * (2 * i), startY)
        }
    }

    private fun reLayoutRing(group: Int, startX: Float, startY: Float) {
        var currentY = startY
        for (i in 3 downTo 1) {
            if (checkPosExist(group, i)) {
                ringList[i - 1].let {
                    it.translationX = startX - it.measuredWidth / 2f
                    it.translationY = currentY
                    currentY -= it.measuredHeight - it.paddingTop - it.paddingBottom
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isEnabled && hanoiValue > 0)
            event?.let {
                when (it.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        currentOpStackPos = when {
                            it.x < (measuredWidth - paddingLeft - paddingRight) / 3f -> 0
                            it.x < 2 * (measuredWidth - paddingLeft - paddingRight) / 3f -> 1
                            else -> 2
                        }
                        lastTouchPointF.x = it.x
                        lastTouchPointF.y = it.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (draggable) {
                            currentMovingRing?.let { ring ->
                                ring.translationX =
                                    (ring.translationX + (it.x - lastTouchPointF.x)).coerceIn(
                                        0f,
                                        0f + measuredWidth - paddingLeft - paddingRight - ring.measuredWidth
                                    )
                            }
                            lastTouchPointF.x = it.x
                            lastTouchPointF.y = it.y
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        回落
                        onUserOPDown()
                        currentRingPosInList = -1
                        currentMovingRing = null
                    }
                }
            }
        return super.onTouchEvent(event)
    }

    private fun onUserOPDown() {
        currentMovingRing?.let {
            val targetStackPos = when {
                it.translationX < (measuredWidth - paddingRight - paddingLeft) / 3f -> 0
                it.translationX < 2 * (measuredWidth - paddingRight - paddingLeft) / 3f -> 1
                else -> 2
            }
            // 最小，随便
            if (currentRingPosInList == 0) {
                updateValue(targetStackPos, 1)
                finishOP(true)
            } else if (currentRingPosInList == 1) {
                if (checkPosExist(targetStackPos, 1)) {
                    rearrange()
                    finishOP(false)
                } else {
                    updateValue(targetStackPos, 2)
                    finishOP(true)
                }
            } else {
                if (checkPosExist(targetStackPos, 1) || checkPosExist(currentOpStackPos, 2)) {
                    rearrange()
                    finishOP(false)
                } else {
                    updateValue(targetStackPos, 3)
                    finishOP(true)
                }
            }
        }

    }

    private fun finishOP(res: Boolean) {
        finishListener?.invoke(res)
    }

    private fun calculateMovingRing() {
        currentRingPosInList = when {
            checkPosExist(currentOpStackPos, 1) -> 0
            checkPosExist(currentOpStackPos, 2) -> 1
            checkPosExist(currentOpStackPos, 3) -> 2
            else -> -1
        }

        currentMovingRing = if (currentRingPosInList == -1) null else ringList[currentRingPosInList]
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        reConstructBasePath()
        reConstructBaseStickPath()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        rearrange()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return
        drawBase(canvas)
        drawBaseLine(canvas)
        drawBaseStick(canvas)
        drawBaseStickLine(canvas)
    }

    private fun reConstructBaseStickPath() {
        baseStickPath.apply {
            reset()
            val partWidth = (measuredWidth - paddingStart - paddingRight) / 6f
            val startX = paddingStart + partWidth - stickRadius.dp
            val startY = measuredHeight - paddingBottom - 1.5f * baseBottomOffset.dp
            constructStick(startX, startY)
            constructStick(startX + partWidth * 2, startY)
            constructStick(startX + partWidth * 4, startY)
        }
    }

    private fun drawBase(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.parseColor("#DBBD9B")
        canvas.drawPath(basePath, mPaint)
    }

    private fun drawBaseStick(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.parseColor("#DBBD9B")
//        val woodBgd =  BitmapFactory.decodeResource(resources, R.drawable.ic_baseline_add_24)
        canvas.drawPath(baseStickPath, mPaint)
    }

    private fun drawBaseLine(canvas: Canvas) {
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = 1.dp
        mPaint.color = Color.parseColor("#A99A89")

        canvas.drawPath(basePath, mPaint)
    }

    private fun drawBaseStickLine(canvas: Canvas) {
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = 1.dp
        mPaint.color = Color.parseColor("#A99A89")

        canvas.drawPath(baseStickPath, mPaint)
    }

    private fun reConstructBasePath() {
        basePath.apply {
            reset()
            moveTo(paddingLeft.toFloat(), measuredHeight - paddingBottom - baseBottomHeight.dp)
            lineTo(paddingLeft.toFloat(), (measuredHeight - paddingBottom).toFloat())
            lineTo(
                (measuredWidth - paddingRight).toFloat(),
                (measuredHeight - paddingBottom).toFloat()
            )
            lineTo(
                (measuredWidth - paddingRight).toFloat(),
                (measuredHeight - paddingBottom).toFloat() - baseBottomHeight.dp
            )
            lineTo(
                paddingLeft.toFloat(), measuredHeight - paddingBottom - baseBottomHeight.dp
            )

            lineTo(
                paddingLeft + baseBottomOffset.dp,
                measuredHeight - paddingBottom - baseBottomOffset.dp - baseBottomHeight.dp
            )

            lineTo(
                measuredWidth - paddingRight - baseBottomOffset.dp,
                measuredHeight - paddingBottom - baseBottomOffset.dp - baseBottomHeight.dp
            )
            lineTo(
                (measuredWidth - paddingRight).toFloat(),
                measuredHeight - paddingBottom - baseBottomHeight.dp
            )
        }

    }

    private fun constructStick(x: Float, y: Float) {
        baseStickPath.apply {
            moveTo(x, y)
            cubicTo(
                x + stickRadius.dp * 2 * 1 / 3f,
                y + 5,
                x + stickRadius.dp * 2 * 2 / 3f,
                y + 5,
                x + (stickRadius * 2).dp,
                y
            )
            lineTo(x + (stickRadius * 2).dp, y - stickHeight.dp)
            cubicTo(
                x + stickRadius.dp * 2 * 2 / 3f,
                y - stickHeight.dp - stickRadius.dp,
                x + stickRadius.dp * 2 * 1 / 3f,
                y - stickHeight.dp - stickRadius.dp,
                x,
                y - stickHeight.dp
            )
            lineTo(x, y)
        }
    }

    /**
     *
     * @param stack 0 1 2
     * xxx
     * 321
     */
    private fun checkPosExist(stack: Int, @IntRange(from = 1, to = 3) pos: Int): Boolean {
        // 000 000 111
        val stackValue = hanoiValue.and((0x07).shl((2 - stack) * 3)).shr((2 - stack) * 3)

        val targetValue = 2f.pow((pos - 1)).toInt()

        return stackValue.and(targetValue) == targetValue
    }

    /**
     *
     * @param stack 0 1 2
     * xxx
     * 321
     */
    private fun updateValue(
        @IntRange(from = 0, to = 2) stack: Int,
        @IntRange(from = 1, to = 3) pos: Int
    ) {
        // 设置 目标位
        var temp = hanoiValue.or(1.shl((pos - 1) + 3 * (2 - stack)))
        when (stack) {
            0 -> {
                temp = temp.and(1.shl((pos - 1) + 3 * (2 - 1)).inv())
                temp = temp.and(1.shl((pos - 1) + 3 * (2 - 2)).inv())
            }
            1 -> {
                temp = temp.and(1.shl((pos - 1) + 3 * (2 - 0)).inv())
                temp = temp.and(1.shl((pos - 1) + 3 * (2 - 2)).inv())
            }
            else -> {
                temp = temp.and(1.shl((pos - 1) + 3 * (2 - 0)).inv())
                temp = temp.and(1.shl((pos - 1) + 3 * (2 - 1)).inv())
            }
        }
        hanoiValue = temp
    }
}


















