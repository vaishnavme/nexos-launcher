package com.vaishnavs.nexoslauncher.ui.files

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.Size
import android.widget.ImageView
import android.media.ThumbnailUtils
import com.vaishnavs.nexoslauncher.data.files.FileEntry
import com.vaishnavs.nexoslauncher.data.files.FileKind
import java.util.concurrent.Executors

class ThumbnailLoader {
    private val cache = object : LruCache<String, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val executor = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    fun load(entry: FileEntry, target: ImageView) {
        if (entry.kind != FileKind.IMAGE && entry.kind != FileKind.VIDEO) return
        val key = "${entry.path}:${entry.lastModified}"
        target.tag = key
        cache.get(key)?.let { bitmap ->
            target.setPadding(0, 0, 0, 0)
            target.setImageBitmap(bitmap)
            return
        }
        executor.execute {
            val bitmap = runCatching {
                val size = Size(THUMB_SIZE, THUMB_SIZE)
                if (entry.kind == FileKind.VIDEO) {
                    ThumbnailUtils.createVideoThumbnail(entry.file, size, null)
                } else {
                    ThumbnailUtils.createImageThumbnail(entry.file, size, null)
                }
            }.getOrNull() ?: return@execute
            cache.put(key, bitmap)
            main.post {
                if (target.tag == key) {
                    target.setPadding(0, 0, 0, 0)
                    target.setImageBitmap(bitmap)
                }
            }
        }
    }

    companion object {
        private const val MAX_BYTES = 2 * 1024 * 1024
        private const val THUMB_SIZE = 96
    }
}
