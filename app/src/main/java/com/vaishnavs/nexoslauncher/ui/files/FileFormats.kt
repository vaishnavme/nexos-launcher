package com.vaishnavs.nexoslauncher.ui.files

import com.vaishnavs.nexoslauncher.R
import com.vaishnavs.nexoslauncher.data.files.FileEntry
import com.vaishnavs.nexoslauncher.data.files.FileKind
import java.text.DateFormat
import java.util.Date

object FileFormats {
    private val dateFormat: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    fun size(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${kb.toInt()} KB"
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.1f GB", mb / 1024.0)
    }

    fun date(millis: Long): String = dateFormat.format(Date(millis))

    fun meta(entry: FileEntry): String {
        val whenText = date(entry.lastModified)
        return if (entry.isDirectory) {
            whenText
        } else {
            "${size(entry.sizeBytes)}  •  $whenText"
        }
    }

    fun iconRes(entry: FileEntry): Int {
        return when (entry.kind) {
            FileKind.FOLDER -> R.drawable.ic_folder
            FileKind.IMAGE -> R.drawable.ic_image
            FileKind.AUDIO -> R.drawable.ic_music
            FileKind.VIDEO -> R.drawable.ic_video
            FileKind.DOCUMENT -> R.drawable.ic_document
            FileKind.OTHER -> R.drawable.ic_file
        }
    }
}
