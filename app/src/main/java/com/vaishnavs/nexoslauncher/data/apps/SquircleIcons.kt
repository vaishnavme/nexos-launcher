package com.vaishnavs.nexoslauncher.data.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object SquircleIcons {
    fun flatten(drawable: Drawable, sizePx: Int): Drawable {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val source = drawable.mutate()
        if (source is AdaptiveIconDrawable) {
            // Adaptive icons are 108dp with a 72dp viewport. Draw layers unmasked
            // so the square fills, then SquircleImageView clips to the iOS shape.
            val extra = sizePx / 4
            val origin = -extra
            val layer = sizePx + extra * 2
            source.background?.mutate()?.let { layerDrawable ->
                layerDrawable.setBounds(origin, origin, origin + layer, origin + layer)
                layerDrawable.draw(canvas)
            }
            source.foreground?.mutate()?.let { layerDrawable ->
                layerDrawable.setBounds(origin, origin, origin + layer, origin + layer)
                layerDrawable.draw(canvas)
            }
        } else {
            source.setBounds(0, 0, sizePx, sizePx)
            source.draw(canvas)
        }
        return BitmapDrawable(null, bitmap)
    }

    private const val DEFAULT_SIZE_PX = 192

    fun flatten(drawable: Drawable): Drawable = flatten(drawable, DEFAULT_SIZE_PX)
}
