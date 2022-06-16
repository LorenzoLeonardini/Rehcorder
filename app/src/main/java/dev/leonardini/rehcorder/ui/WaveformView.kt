package dev.leonardini.rehcorder.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import dev.leonardini.rehcorder.R
import kotlin.math.sin

// Extending TextView to exploit its onMeasure
class WaveformView : androidx.appcompat.widget.AppCompatTextView {

    private val paint :Paint = Paint()

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
            attrs, R.styleable.WaveformView, defStyle, 0
        )

        // Read attributes

        a.recycle()

        val typedValue = TypedValue()
        context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        paint.color = typedValue.data
    }

    override fun onDraw(canvas: Canvas) {
        for (i in 0..width step 20) {
            val value = (sin(i.toDouble() * 3.14 * 3.14 / width.toDouble()) * (height / 2)) + 30
            canvas.drawRoundRect(i.toFloat(), (height - value).toFloat() / 2f, i.toFloat() + 16f, (height - value).toFloat() / 2f + value.toFloat(), 8f, 8f, paint)
        }

        super.onDraw(canvas)
    }
}