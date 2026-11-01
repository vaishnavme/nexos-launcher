package com.vaishnavs.nexoslauncher.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

class SquircleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val clipPath = Path()
    private val clipRect = RectF()

    init {
        scaleType = ScaleType.CENTER_CROP
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (view.width == 0 || view.height == 0) return
                val radius = min(view.width, view.height) * CORNER_FRACTION
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipRect.set(0f, 0f, w.toFloat(), h.toFloat())
        val radius = min(w, h) * CORNER_FRACTION
        clipPath.reset()
        clipPath.addRoundRect(clipRect, radius, radius, Path.Direction.CW)
        invalidateOutline()
    }

    override fun onDraw(canvas: Canvas) {
        if (clipPath.isEmpty && width > 0 && height > 0) {
            onSizeChanged(width, height, 0, 0)
        }
        val save = canvas.save()
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
        canvas.restoreToCount(save)
    }

    companion object {
        private const val CORNER_FRACTION = 0.22f
    }
}
