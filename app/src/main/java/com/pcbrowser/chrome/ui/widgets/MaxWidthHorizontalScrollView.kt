package com.pcbrowser.chrome.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.HorizontalScrollView

class MaxWidthHorizontalScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private var maxAllowedWidth: Int = -1

    fun setMaxWidth(width: Int) {
        maxAllowedWidth = width
        requestLayout()
    }

    fun getMaxWidth(): Int = maxAllowedWidth

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var modifiedWidthSpec = widthMeasureSpec
        if (maxAllowedWidth > 0) {
            val mode = MeasureSpec.getMode(widthMeasureSpec)
            val size = MeasureSpec.getSize(widthMeasureSpec)
            if (size > maxAllowedWidth) {
                modifiedWidthSpec = MeasureSpec.makeMeasureSpec(maxAllowedWidth, mode)
            }
        }
        super.onMeasure(modifiedWidthSpec, heightMeasureSpec)
    }
}
