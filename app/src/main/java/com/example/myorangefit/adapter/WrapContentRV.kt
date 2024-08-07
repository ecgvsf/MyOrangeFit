package com.example.myorangefit.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView


class WrapContentRV : RecyclerView {
    constructor(context: Context?) : super(context!!)

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    )

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val newHeightSpec = MeasureSpec.makeMeasureSpec(
            Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST
        )
        super.onMeasure(widthSpec, newHeightSpec)
        val params = layoutParams
        params.height = measuredHeight
    }

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        // Never allow scrolling to happen
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        // Never allow scrolling to happen
        return false
    }
}