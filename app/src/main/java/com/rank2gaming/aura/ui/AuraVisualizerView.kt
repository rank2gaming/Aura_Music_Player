package com.rank2gaming.aura.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AuraVisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        strokeWidth = 6f
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var currentTemplateIndex = 0
    private var isMiniMode = false
    private var isAnimating = false

    // Animation State
    private var phase = 0.0
    private val particles = FloatArray(100) { Random.nextFloat() * 360f }

    private val palettes = List(60) { i ->
        when (i % 10) {
            0 -> listOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA)
            1 -> listOf(Color.CYAN, Color.BLUE, Color.parseColor("#00E5FF"), Color.parseColor("#2979FF"), Color.WHITE)
            2 -> listOf(Color.RED, Color.parseColor("#FF5722"), Color.parseColor("#FF9800"), Color.YELLOW, Color.WHITE)
            3 -> listOf(Color.GREEN, Color.parseColor("#CDDC39"), Color.YELLOW, Color.parseColor("#FFEB3B"), Color.WHITE)
            4 -> listOf(Color.parseColor("#6200EE"), Color.MAGENTA, Color.CYAN, Color.parseColor("#03DAC5"), Color.WHITE)
            5 -> listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.CYAN, Color.MAGENTA)
            6 -> listOf(Color.parseColor("#FFD700"), Color.parseColor("#FF6D00"), Color.YELLOW, Color.WHITE, Color.parseColor("#FFEB3B"))
            7 -> listOf(Color.parseColor("#1DE9B6"), Color.CYAN, Color.BLUE, Color.WHITE, Color.parseColor("#00BFA5"))
            8 -> listOf(Color.parseColor("#EA80FC"), Color.parseColor("#AA00FF"), Color.MAGENTA, Color.RED, Color.WHITE)
            else -> listOf(Color.WHITE, Color.LTGRAY, Color.GRAY, Color.DKGRAY, Color.BLACK)
        }
    }

    fun setIsMiniMode(isMini: Boolean) {
        this.isMiniMode = isMini
        paint.strokeWidth = if (isMini) 4f else 6f
    }

    fun setTemplate(index: Int) {
        currentTemplateIndex = index % 60
        invalidate()
    }

    fun nextTemplate() {
        currentTemplateIndex = (currentTemplateIndex + 1) % 60
        invalidate()
    }

    fun getCurrentTemplate() = currentTemplateIndex

    fun setPlaying(playing: Boolean) {
        isAnimating = playing
        if (playing) invalidate()
    }

    // No-op for simulated mode (No Permission Required)
    fun link(audioSessionId: Int) {}

    fun release() {
        isAnimating = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isAnimating) return

        val cx = width / 2f
        val cy = height / 2f

        val baseRadius = if (isMiniMode) {
            width / 3.5f
        } else {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150f, resources.displayMetrics)
        }

        phase += 0.08 // Animation Speed

        val currentPalette = palettes[currentTemplateIndex % 60]
        paint.color = currentPalette[0]

        // --- 60 TEMPLATES RENDER LOGIC ---
        if (currentTemplateIndex < 30) {
            val style = currentTemplateIndex % 5
            when (style) {
                0 -> drawSimulatedBars(canvas, cx, cy, baseRadius, 32, 1.0f, currentPalette)
                1 -> drawSineWave(canvas, cx, cy, baseRadius, 3, 20f, currentPalette)
                2 -> drawPulse(canvas, cx, cy, baseRadius, 2, currentPalette)
                3 -> drawRotatingPoly(canvas, cx, cy, baseRadius, 3, currentPalette)
                4 -> drawParticles(canvas, cx, cy, baseRadius, 20, 5f, currentPalette)
            }
        } else {
            val newStyle = (currentTemplateIndex - 30) % 5
            when (newStyle) {
                0 -> drawSpiralWeb(canvas, cx, cy, baseRadius, currentPalette)
                1 -> drawHexGrid(canvas, cx, cy, baseRadius, currentPalette)
                2 -> drawStarBurst(canvas, cx, cy, baseRadius, currentPalette)
                3 -> drawOrbitingRings(canvas, cx, cy, baseRadius, currentPalette)
                4 -> drawChaosField(canvas, cx, cy, baseRadius, currentPalette)
            }
        }

        if (isAnimating) {
            postInvalidateDelayed(16)
        }
    }

    // --- SIMULATED DRAWING FUNCTIONS ---

    private fun drawSimulatedBars(c: Canvas, cx: Float, cy: Float, r: Float, count: Int, scale: Float, colors: List<Int>, rotate: Boolean = false, mirror: Boolean = false) {
        val rotOffset = if(rotate) phase * 20.0 else 0.0

        for (i in 0 until count) {
            paint.color = colors[i % colors.size]
            val angle = (i.toDouble() / count) * 360.0 + rotOffset

            // Simulated Math Noise
            val input = (phase * 2.0) + i
            val noise = (sin(input) * 0.5) + 0.5
            val height = ((noise * 100.0 * scale) + 10.0).toFloat()

            val rad = Math.toRadians(angle)
            val cosV = cos(rad).toFloat()
            val sinV = sin(rad).toFloat()

            val startX = cx + (r * cosV)
            val startY = cy + (r * sinV)
            val endX = cx + ((r + height) * cosV)
            val endY = cy + ((r + height) * sinV)

            c.drawLine(startX, startY, endX, endY, paint)

            if(mirror) {
                val endX2 = cx + ((r - height/2) * cosV)
                val endY2 = cy + ((r - height/2) * sinV)
                c.drawLine(startX, startY, endX2, endY2, paint)
            }
        }
    }

    private fun drawSineWave(c: Canvas, cx: Float, cy: Float, r: Float, freq: Int, amp: Float, colors: List<Int>) {
        val points = 100
        for (i in 0 until points) {
            paint.color = colors[(i / 20) % colors.size]
            val angle = (i.toDouble() / points) * 360.0 + (phase * 10.0)

            val input = (i * freq * 0.1) + phase
            val offset = (sin(input) * amp).toFloat()

            val rad = Math.toRadians(angle)
            val cosV = cos(rad).toFloat()
            val sinV = sin(rad).toFloat()

            c.drawPoint(cx + ((r + offset) * cosV), cy + ((r + offset) * sinV), paint)
        }
    }

    private fun drawPulse(c: Canvas, cx: Float, cy: Float, r: Float, rings: Int, colors: List<Int>) {
        val input = phase * 5.0
        val pulse = ((sin(input) * 0.1 + 1.0) * r).toFloat()

        paint.style = Paint.Style.STROKE
        for(i in 0 until rings) {
            paint.color = colors[i % colors.size]
            val gap = (i * 20).toFloat()
            c.drawCircle(cx, cy, pulse + gap, paint)
        }
    }

    private fun drawRotatingPoly(c: Canvas, cx: Float, cy: Float, r: Float, sides: Int, colors: List<Int>) {
        val rotation = phase * 30.0
        val polyPath = Path()
        paint.color = colors[0]
        for (i in 0 until sides) {
            val angle = (i.toDouble() / sides) * 360.0 + rotation
            val rad = Math.toRadians(angle)
            val x = cx + r * cos(rad).toFloat()
            val y = cy + r * sin(rad).toFloat()
            if (i == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
        }
        polyPath.close()
        c.drawPath(polyPath, paint)
    }

    private fun drawParticles(c: Canvas, cx: Float, cy: Float, r: Float, count: Int, speed: Float, colors: List<Int>) {
        fillPaint.style = Paint.Style.FILL
        for (i in 0 until count) {
            fillPaint.color = colors[i % colors.size]
            particles[i] = ((particles[i] + speed) % 360f)

            val rad = Math.toRadians(particles[i].toDouble())
            val input = phase + i
            val dist = (r + (sin(input) * 20.0)).toFloat()

            val x = cx + (dist * cos(rad).toFloat())
            val y = cy + (dist * sin(rad).toFloat())

            c.drawCircle(x, y, if (isMiniMode) 3f else 6f, fillPaint)
        }
    }

    // --- NEW STYLES (30-59) ---

    private fun drawSpiralWeb(c: Canvas, cx: Float, cy: Float, r: Float, colors: List<Int>) {
        for (i in 0 until 40) {
            paint.color = colors[i % colors.size]
            val rad = Math.toRadians((i * 20.0) + (phase * 50.0))
            val dist = (r + (i * 2) + (sin(phase + i) * 10.0)).toFloat()
            val x = cx + (dist * cos(rad).toFloat())
            val y = cy + (dist * sin(rad).toFloat())
            c.drawCircle(x, y, 4f, paint)
            if (i % 5 == 0) c.drawLine(cx, cy, x, y, paint)
        }
    }

    private fun drawHexGrid(c: Canvas, cx: Float, cy: Float, r: Float, colors: List<Int>) {
        val sides = 6
        for (k in 0 until 3) {
            paint.color = colors[k % colors.size]
            val currentR = (r + (k * 30) + (sin(phase) * 10.0)).toFloat()
            val polyPath = Path()
            for (i in 0 until sides) {
                val angle = (i.toDouble() / sides) * 360.0 + (phase * (10 * (k + 1)))
                val rad = Math.toRadians(angle)
                val x = cx + (currentR * cos(rad).toFloat())
                val y = cy + (currentR * sin(rad).toFloat())
                if (i == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
            }
            polyPath.close()
            c.drawPath(polyPath, paint)
        }
    }

    private fun drawStarBurst(c: Canvas, cx: Float, cy: Float, r: Float, colors: List<Int>) {
        val points = 16
        for (i in 0 until points) {
            paint.color = colors[i % colors.size]
            val angle = (i.toDouble() / points) * 360.0 + (phase * 20.0)
            val rad = Math.toRadians(angle)

            val input = phase + i
            val len = (r + 20 + (abs(sin(input)) * 80)).toFloat()

            val cosV = cos(rad).toFloat()
            val sinV = sin(rad).toFloat()

            c.drawLine(cx + r * cosV, cy + r * sinV, cx + len * cosV, cy + len * sinV, paint)
        }
    }

    private fun drawOrbitingRings(c: Canvas, cx: Float, cy: Float, r: Float, colors: List<Int>) {
        paint.style = Paint.Style.STROKE
        for (i in 0 until 5) {
            paint.color = colors[i % colors.size]
            val ovalR = (r + (i * 15)).toFloat()
            c.save()
            val rot = (phase * 10 * (i + 1)).toFloat()
            c.rotate(rot, cx, cy)
            c.drawOval(cx - ovalR, cy - (ovalR / 2), cx + ovalR, cy + (ovalR / 2), paint)
            c.restore()
        }
    }

    private fun drawChaosField(c: Canvas, cx: Float, cy: Float, r: Float, colors: List<Int>) {
        for (i in 0 until 50) {
            paint.color = colors[i % colors.size]
            val angle = (i * 137.5) + (phase * 20.0)
            val rad = Math.toRadians(angle)

            val dist = (r + (sin(phase + i) * 50.0) + (i * 2)).toFloat()

            val x = cx + (dist * cos(rad).toFloat())
            val y = cy + (dist * sin(rad).toFloat())

            c.drawCircle(x, y, 3f, paint)
        }
    }
}