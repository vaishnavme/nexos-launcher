package com.vaishnavs.nexoslauncher.data.files

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.vaishnavs.nexoslauncher.R
import java.io.File

class StorageRepository(private val context: Context) {
    fun hasFullAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasMediaAccess(): Boolean {
        if (hasFullAccess()) return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MEDIA_PERMISSIONS.any { permission ->
                ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
            }
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun listSources(): List<FileSource> {
        val volumes = listVolumes()
        val media = listOf(
            FileSource.Media(ID_IMAGES, context.getString(R.string.files_images), MediaCategory.IMAGES),
            FileSource.Media(ID_AUDIO, context.getString(R.string.files_music), MediaCategory.AUDIO),
            FileSource.Media(ID_VIDEO, context.getString(R.string.files_videos), MediaCategory.VIDEO),
            FileSource.Media(ID_DOCS, context.getString(R.string.files_documents), MediaCategory.DOCUMENTS)
        )
        return volumes + media
    }

    fun listDirectory(path: String): List<FileEntry> {
        val dir = File(path)
        val children = dir.listFiles() ?: return emptyList()
        return children.map { toEntry(it) }
    }

    fun listMedia(category: MediaCategory): List<FileEntry> {
        val (uri, extraSelection, extraArgs) = when (category) {
            MediaCategory.IMAGES -> Triple(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null,
                emptyArray<String>()
            )
            MediaCategory.AUDIO -> Triple(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                emptyArray()
            )
            MediaCategory.VIDEO -> Triple(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null,
                emptyArray()
            )
            MediaCategory.DOCUMENTS -> Triple(
                MediaStore.Files.getContentUri("external"),
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?",
                arrayOf("application/%", "text/%")
            )
        }
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val entries = mutableListOf<FileEntry>()
        context.contentResolver.query(
            uri,
            projection,
            extraSelection,
            extraArgs.ifEmpty { null },
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                if (dataIndex < 0) continue
                val path = cursor.getString(dataIndex) ?: continue
                val file = File(path)
                if (!file.exists() || file.isDirectory) continue
                val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) else FileTypes.mimeOf(file)
                val name = if (nameIndex >= 0) {
                    cursor.getString(nameIndex) ?: file.name
                } else {
                    file.name
                }
                entries += FileEntry(
                    name = name,
                    path = path,
                    isDirectory = false,
                    sizeBytes = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else file.length(),
                    lastModified = if (dateIndex >= 0) {
                        cursor.getLong(dateIndex) * 1000L
                    } else {
                        file.lastModified()
                    },
                    mimeType = mime,
                    kind = FileTypes.kindOf(false, mime)
                )
            }
        }
        return entries
    }

    fun defaultSourceId(sources: List<FileSource>, mimeFilters: List<String>): String? {
        val filter = mimeFilters.firstOrNull().orEmpty()
        val mediaId = when {
            filter.startsWith("image/") -> ID_IMAGES
            filter.startsWith("audio/") -> ID_AUDIO
            filter.startsWith("video/") -> ID_VIDEO
            else -> null
        }
        if (mediaId != null && sources.any { it.id == mediaId }) return mediaId
        return sources.firstOrNull { it is FileSource.Volume }?.id
            ?: sources.firstOrNull()?.id
    }

    private fun listVolumes(): List<FileSource.Volume> {
        val manager = context.getSystemService(StorageManager::class.java) ?: return fallbackVolumes()
        val volumes = manager.storageVolumes.mapNotNull { volume ->
            if (volume.state != Environment.MEDIA_MOUNTED) return@mapNotNull null
            val directory = volumeDirectory(volume) ?: return@mapNotNull null
            if (!directory.exists()) return@mapNotNull null
            val kind = volumeKind(volume, directory)
            val label = volume.getDescription(context)?.takeIf { it.isNotBlank() }
                ?: defaultLabel(kind)
            FileSource.Volume(
                id = "volume:${directory.absolutePath}",
                label = label,
                path = directory.absolutePath,
                kind = kind
            )
        }
        return volumes.ifEmpty { fallbackVolumes() }
    }

    private fun fallbackVolumes(): List<FileSource.Volume> {
        val primary = Environment.getExternalStorageDirectory()
        if (!primary.exists()) return emptyList()
        return listOf(
            FileSource.Volume(
                id = "volume:${primary.absolutePath}",
                label = defaultLabel(VolumeKind.INTERNAL),
                path = primary.absolutePath,
                kind = VolumeKind.INTERNAL
            )
        )
    }

    private fun volumeDirectory(volume: StorageVolume): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return volume.directory
        }
        return runCatching {
            StorageVolume::class.java.getMethod("getPathFile").invoke(volume) as? File
        }.getOrNull() ?: runCatching {
            File(StorageVolume::class.java.getMethod("getPath").invoke(volume) as String)
        }.getOrNull()
    }

    private fun volumeKind(volume: StorageVolume, directory: File): VolumeKind {
        if (volume.isPrimary) return VolumeKind.INTERNAL
        val description = volume.getDescription(context).orEmpty().lowercase()
        val path = directory.absolutePath.lowercase()
        return when {
            "usb" in description || "usb" in path || path.contains("media_rw") -> VolumeKind.USB
            volume.isRemovable -> VolumeKind.SD
            else -> VolumeKind.INTERNAL
        }
    }

    private fun defaultLabel(kind: VolumeKind): String {
        val res = when (kind) {
            VolumeKind.INTERNAL -> R.string.files_internal
            VolumeKind.SD -> R.string.files_sd
            VolumeKind.USB -> R.string.files_usb
        }
        return context.getString(res)
    }

    private fun toEntry(file: File): FileEntry {
        val mime = if (file.isDirectory) null else FileTypes.mimeOf(file)
        return FileEntry(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            sizeBytes = if (file.isDirectory) 0L else file.length(),
            lastModified = file.lastModified(),
            mimeType = mime,
            kind = FileTypes.kindOf(file.isDirectory, mime)
        )
    }

    companion object {
        const val ID_IMAGES = "media:images"
        const val ID_AUDIO = "media:audio"
        const val ID_VIDEO = "media:video"
        const val ID_DOCS = "media:documents"

        val MEDIA_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val LEGACY_STORAGE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
