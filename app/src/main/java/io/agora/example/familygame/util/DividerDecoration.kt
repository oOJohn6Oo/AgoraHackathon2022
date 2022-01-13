package io.agora.example.familygame.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import kotlin.math.roundToInt

/**
 * 分割线
 *
 * @author chenhengfei@agora.io
 */
class DividerDecoration : ItemDecoration {
    private val gapHorizontal: Int
    private val gapVertical: Int
    private val spanCount: Int

    constructor(spanCount: Int) {
        gapHorizontal = 16.dp.toInt()
        gapVertical = gapHorizontal
        this.spanCount = spanCount
    }

    constructor(spanCount: Int, gapHorizontal: Int, gapHeight: Int) {
        this.gapHorizontal = gapHorizontal.dp.roundToInt()
        gapVertical = gapHeight.dp.roundToInt()
        this.spanCount = spanCount
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val index = parent.getChildAdapterPosition(view)
        if (spanCount == 1) {
            outRect.left = gapHorizontal
            outRect.right = gapHorizontal
        } else {
            outRect.left = gapHorizontal * (spanCount - index % spanCount) / spanCount
            outRect.right = gapHorizontal * (1 + index % spanCount) / spanCount
        }
        outRect.top = gapVertical / 2
        outRect.bottom = gapVertical / 2
    }
}