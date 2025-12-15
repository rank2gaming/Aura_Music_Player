package com.rank2gaming.aura.audio.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class RotaryKnobView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var value = 0f
    private var minValue = -10f
    private var maxValue = 10f
    private var isHapticEnabled = true
    private var lastHapticValue = -100

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 15f
        color = Color.parseColor("#E0E0E0")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 15f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#FF4081")
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        setShadowLayer(12f, 0f, 6f, Color.parseColor("#40000000"))
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4081")
        style = Paint.Style.FILL
    }

    private var knobRadius = 0f
    private var cx = 0f
    private var cy = 0f
    private val startAngle = 135f
    private val sweepAngle = 270f

    var onValueChanged: ((Float) -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, knobPaint)
    }

    // --- Added Getter ---
    fun getValue(): Float {
        return value
    }

    fun setValue(newValue: Float) {
        value = newValue.coerceIn(minValue, maxValue)
        invalidate()
    }

    fun setRange(min: Float, max: Float) {
        minValue = min
        maxValue = max
    }

    fun setTrackColor(color: Int) {
        progressPaint.color = color
        dotPaint.color = color
        invalidate()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        isHapticEnabled = enabled
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        knobRadius = min(w, h) / 2f * 0.75f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(cx, cy, knobRadius, knobPaint)

        val trackR = knobRadius * 1.15f
        val rect = RectF(cx - trackR, cy - trackR, cx + trackR, cy + trackR)
        canvas.drawArc(rect, startAngle, sweepAngle, false, trackPaint)

        val pct = (value - minValue) / (maxValue - minValue)
        val currentSweep = sweepAngle * pct
        canvas.drawArc(rect, startAngle, currentSweep, false, progressPaint)

        val angleRad = Math.toRadians((startAngle + currentSweep).toDouble())
        val dotR = knobRadius * 0.7f
        val dotX = cx + cos(angleRad).toFloat() * dotR
        val dotY = cy + sin(angleRad).toFloat() * dotR
        canvas.drawCircle(dotX, dotY, 12f, dotPaint)

        val displayValue = if (abs(value) < 0.1f) "0dB" else String.format("%+.1f", value)
        val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(displayValue, cx, textY, textPaint)
    }

    private var lastTouchY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = lastTouchY - event.y
                val change = deltaY * 0.03f

                if (abs(change) > 0.01f) {
                    val newValue = (value + change).coerceIn(minValue, maxValue)
                    if (newValue != value) {
                        value = newValue
                        onValueChanged?.invoke(value)

                        if (isHapticEnabled) {
                            val intVal = value.toInt()
                            if (intVal != lastHapticValue) {
                                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                lastHapticValue = intVal
                            }
                        }
                        invalidate()
                    }
                    lastTouchY = event.y
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}