package com.vaishnavs.nexoslauncher.data.apps

import android.graphics.drawable.Drawable

class IconCache(
    private val maxSize: Int = MAX_ENTRIES
) {
    private val cache = object : LinkedHashMap<String, Drawable>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Drawable>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: String, loader: () -> Drawable): Drawable {
        cache[key]?.let { return it }
        val drawable = loader()
        cache[key] = drawable
        return drawable
    }

    companion object {
        private const val MAX_ENTRIES = 64
    }
}
