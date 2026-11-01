package com.vaishnavs.nexoslauncher.data.files

import android.webkit.MimeTypeMap
import java.io.File

object FileTypes {
    fun mimeOf(file: File): String? {
        val extension = file.extension.lowercase()
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun kindOf(isDirectory: Boolean, mimeType: String?): FileKind {
        if (isDirectory) return FileKind.FOLDER
        val mime = mimeType.orEmpty()
        return when {
            mime.startsWith("image/") -> FileKind.IMAGE
            mime.startsWith("audio/") -> FileKind.AUDIO
            mime.startsWith("video/") -> FileKind.VIDEO
            mime.startsWith("text/") || mime.startsWith("application/") -> FileKind.DOCUMENT
            else -> FileKind.OTHER
        }
    }

    fun matchesFilter(mimeType: String?, isDirectory: Boolean, filters: List<String>): Boolean {
        if (isDirectory) return true
        if (filters.isEmpty() || filters.any { it == "*/*" }) return true
        val mime = mimeType ?: return false
        return filters.any { filter ->
            when {
                filter == "*/*" -> true
                filter.endsWith("/*") -> mime.startsWith(filter.removeSuffix("*"))
                else -> mime.equals(filter, ignoreCase = true)
            }
        }
    }
}
