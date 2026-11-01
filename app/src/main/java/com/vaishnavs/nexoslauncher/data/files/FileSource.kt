package com.vaishnavs.nexoslauncher.data.files

sealed class FileSource {
    abstract val id: String
    abstract val label: String

    data class Volume(
        override val id: String,
        override val label: String,
        val path: String,
        val kind: VolumeKind
    ) : FileSource()

    data class Media(
        override val id: String,
        override val label: String,
        val category: MediaCategory
    ) : FileSource()
}

enum class VolumeKind {
    INTERNAL,
    SD,
    USB
}

enum class MediaCategory {
    IMAGES,
    AUDIO,
    VIDEO,
    DOCUMENTS
}
