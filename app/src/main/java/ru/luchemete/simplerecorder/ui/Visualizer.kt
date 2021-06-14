package ru.luchemete.simplerecorder.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import ru.luchemete.simplerecorder.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

open class Visualizer : View {

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
    ) : super(context, attrs) {
        init()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    private var ampNormalizer: (Float) -> Float = { sqrt(it) }
    private var calculateAmplitudeMax: (FloatArray) -> Float = { it.maxOrNull() ?: 0f }

    private var amps = mutableListOf<Float>()

    private var cursorPosition = 0

    private var maxVisibleBars = 0

    private lateinit var ampPaint: Paint
    private lateinit var cursorPaint: Paint
    private lateinit var baselinePaint: Paint

    private fun init() {
        ampPaint = Paint().apply { color = context.getColor(R.color.gray_amp) }
        cursorPaint = Paint().apply { color = context.getColor(R.color.white) }
        baselinePaint = Paint().apply { color = context.getColor(R.color.teal_200) }
    }

    private fun getBaseLine() = height / 2
    private fun getStartBar() = max(0, cursorPosition - maxVisibleBars)
    private fun getEndBar() = min(amps.size, getStartBar() + maxVisibleBars)
    private fun getBarHeightAt(i: Int) = height * amps[i] / 1.5

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        maxVisibleBars = width
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxVisibleBars = w
    }

    override fun onDetachedFromWindow() {
        amps.clear()
        ampNormalizer = { 0f }
        calculateAmplitudeMax = { 0f }
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        drawBaseLine(canvas)
        drawAmps(canvas)
        drawCursor(canvas)
        super.onDraw(canvas)
    }

    private fun drawBaseLine(canvas: Canvas) {
        canvas.drawLine(
            0f,
            getBaseLine().toFloat(),
            width.toFloat(),
            getBaseLine().toFloat(),
            baselinePaint
        )
    }

    private fun drawCursor(canvas: Canvas) {
        val startX = min(width.toFloat() - 1f, cursorPosition.toFloat())
        canvas.drawLine(
            startX,
            height / 3f,
            startX,
            height - height / 3f,
            cursorPaint
        )
    }

    private fun drawAmps(canvas: Canvas) {
        if (amps.isNotEmpty()) {
            for (i in getStartBar() until getEndBar()) {
                val startX = i - getStartBar()
                drawAmp(canvas, startX.toFloat(), getBarHeightAt(i).toInt(), getBaseLine())
            }
        }
    }

    private fun drawAmp(canvas: Canvas, startX: Float, height: Int, baseLine: Int) {
        val startY = baseLine + (height / 2).toFloat()
        val stopY = startY - height
        canvas.drawLine(startX, startY, startX, stopY, ampPaint)
    }

    private var initialTouchX = 0f
    private var firstTouchX = 0f

    var onCursorPositionChanged: ((Int) -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.parent.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.x
                firstTouchX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                isPressed = true
                updateView(event)
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                detectSingleTap(event)
                this.parent.requestDisallowInterceptTouchEvent(false)
                isPressed = false
            }
        }
        return true
    }

    private fun updateView(event: MotionEvent) {
        val distance = event.x - firstTouchX

        if (abs(distance) > 0) {
            firstTouchX = event.x
            cursorPositionChanged(min(amps.size, max(0, cursorPosition + distance.toInt())))
        }
    }

    private fun detectSingleTap(event: MotionEvent) {
        val secondTouch = event.x
        val distance = secondTouch - initialTouchX

        if (distance.toInt() == 0) {
            cursorPositionChanged(min(amps.size, secondTouch.toInt() + getStartBar()))
        }
    }

    fun addAmp(data: FloatArray, position: Int, isRecording: Boolean) {
        val amp = calculateAmplitudeMax(data)

        try {
            this.amps.removeAt(position)
        } catch (e: Exception) {
        }
        this.amps.add(position, ampNormalizer.invoke(amp))

        if (isRecording) setCursorPosition(position)
    }

    fun setCursorPosition(position: Int) {
        cursorPosition = position
        invalidate()
    }

    private fun cursorPositionChanged(position: Int) {
        setCursorPosition(position)
        onCursorPositionChanged?.invoke(position)
    }

}