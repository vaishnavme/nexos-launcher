package com.vaishnavs.nexoslauncher.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.vaishnavs.nexoslauncher.R
import kotlin.math.min

class SpeedArcView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var speed: Int = PLACEHOLDER_SPEED
        set(value) {
            field = value
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.charcoal_elevated)
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.white)
    }
    private val arcBounds = RectF()

    override fun onDraw(canvas: Canvas) {
        val stroke = min(width, height) * 0.09f
        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke
        val inset = stroke / 2f + 4f
        arcBounds.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, trackPaint)
        val fraction = (speed / MAX_SPEED.toFloat()).coerceIn(0f, 1f)
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE * fraction, false, progressPaint)
    }

    companion object {
        const val PLACEHOLDER_SPEED = 62
        private const val MAX_SPEED = 180
        private const val START_ANGLE = 135f
        private const val SWEEP_ANGLE = 270f
    }
}
