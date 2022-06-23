package dev.leonardini.rehcorder.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.util.TypedValue
import dev.leonardini.rehcorder.R

// Extending TextView to exploit its onMeasure
class WaveformView : androidx.appcompat.widget.AppCompatTextView, Visualizer.OnDataCaptureListener {

    private val paint: Paint = Paint()
    private var visualizer: Visualizer? = null
    private var rawAudioBytes: ByteArray? = ByteArray(0)

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
        rawAudioBytes?.let { rawAudioBytes ->
            val samples = (rawAudioBytes.size / (width / 20f)).toInt()
            for (i in 0 until (width / 20)) {
                var value = 0f
                for (j in 0 until samples) {
                    if (i * samples + j < rawAudioBytes.size)
                        value += rawAudioBytes[i * samples + j].toFloat() + 128
                }
                value /= samples
                value = (value / 256) * height
                value = value.coerceAtLeast(16f)
                canvas.drawRoundRect(
                    i * 20f,
                    (height - value) / 2f,
                    (i * 20f) + 16f,
                    (height - value) / 2f + value,
                    8f,
                    8f,
                    paint
                )
            }
        }

        super.onDraw(canvas)
    }

    fun setAudioSession(audioSessionId: Int) {
        visualizer?.apply {
            enabled = false
            release()
        }
        try {
            visualizer = Visualizer(audioSessionId)
            visualizer!!.apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    this@WaveformView,
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    false
                )
                enabled = true
            }
        } catch (_: Exception) {
            visualizer = null
        }
    }

    fun cleanAudioVisualizer() {
        visualizer?.apply {
            enabled = false
            release()
        }
    }

    override fun onWaveFormDataCapture(
        visualizer: Visualizer?,
        waveform: ByteArray?,
        samplingRate: Int
    ) {
        rawAudioBytes = waveform
        invalidate()
    }

    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
    }
}