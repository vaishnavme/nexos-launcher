package com.vaishnavs.nexoslauncher.data.files

import java.io.File

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
    val kind: FileKind
) {
    val file: File get() = File(path)
}

enum class FileKind {
    FOLDER,
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    OTHER
}

enum class SortMode {
    NAME,
    DATE,
    SIZE
}

data class FileClipboard(
    val paths: List<String>,
    val cut: Boolean
)
