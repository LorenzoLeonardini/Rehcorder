package dev.leonardini.rehcorder.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatSeekBar
import dev.leonardini.rehcorder.R

/**
 * Seek bar extension providing support for regions highlights
 */
class HighlightedSeekBar : AppCompatSeekBar {

    private val paint: Paint = Paint()
    private val regions = ArrayList<Pair<Float, Float>>()

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.HighlightedSeekBar, defStyle, 0
        )

        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            com.google.android.material.R.attr.colorError,
            typedValue,
            true
        )
        paint.color = a.getColor(R.styleable.HighlightedSeekBar_selectionColor, typedValue.data)

        a.recycle()

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val width = measuredWidth - paddingLeft - paddingRight
        val height = measuredHeight - paddingTop - paddingBottom

        // This will look horrible
        val barHeight = progressDrawable.intrinsicHeight
        val top = (height - barHeight) / 2f

        for (region in regions) {
            canvas?.drawRect(
                paddingLeft + region.first * width,
                top,
                paddingLeft + region.second * width,
                top + barHeight.toFloat(),
                paint
            )
        }

        drawThumb(canvas!!)
    }

    // Copied from AbsSeekBar implementation (up two superclasses)
    private fun drawThumb(canvas: Canvas) {
        if (thumb != null) {
            val saveCount = canvas.save()
            // Translate the padding. For the x, we need to allow the thumb to
            // draw in its extra space
            canvas.translate((paddingLeft - thumbOffset).toFloat(), paddingTop.toFloat())
            thumb.draw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }

    fun highlightRegion(start: Int, end: Int) {
        regions.add(Pair(start.toFloat() / max, end.toFloat() / max))
        invalidate()
    }

    fun clearRegions() {
        regions.clear()
        invalidate()
    }

}