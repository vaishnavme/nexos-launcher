package com.vaishnavs.nexoslauncher.data.files

import java.io.File
import java.io.IOException

object FileOperations {
    fun copy(sources: List<File>, destDir: File): Int {
        requireDirectory(destDir)
        var count = 0
        sources.forEach { source ->
            count += copyOne(source, destDir)
        }
        return count
    }

    fun move(sources: List<File>, destDir: File): Int {
        requireDirectory(destDir)
        var count = 0
        sources.forEach { source ->
            count += moveOne(source, destDir)
        }
        return count
    }

    fun delete(files: List<File>): Int {
        var count = 0
        files.forEach { file ->
            count += deleteOne(file)
        }
        return count
    }

    fun rename(file: File, newName: String): File {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed.contains('/') || trimmed.contains(File.separatorChar)) {
            throw IOException("invalid name")
        }
        val dest = File(file.parentFile, trimmed)
        if (dest.absolutePath == file.absolutePath) return file
        if (dest.exists()) throw IOException("exists")
        if (!file.renameTo(dest)) throw IOException("rename failed")
        return dest
    }

    fun mkdir(parent: File, name: String): File {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.contains('/') || trimmed.contains(File.separatorChar)) {
            throw IOException("invalid name")
        }
        val dir = File(parent, trimmed)
        if (dir.exists()) throw IOException("exists")
        if (!dir.mkdirs()) throw IOException("mkdir failed")
        return dir
    }

    private fun copyOne(source: File, destDir: File): Int {
        preventNesting(source, destDir)
        val dest = uniqueName(destDir, source.name, source.isDirectory)
        return if (source.isDirectory) {
            if (!dest.mkdirs() && !dest.isDirectory) throw IOException("mkdir failed")
            var count = 1
            source.listFiles()?.forEach { child ->
                count += copyOne(child, dest)
            }
            count
        } else {
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            1
        }
    }

    private fun moveOne(source: File, destDir: File): Int {
        preventNesting(source, destDir)
        if (source.parentFile?.canonicalPath == destDir.canonicalPath) return 0
        val dest = uniqueName(destDir, source.name, source.isDirectory)
        if (source.renameTo(dest)) {
            return if (dest.isDirectory) countTree(dest) else 1
        }
        val copied = copyOne(source, destDir)
        deleteOne(source)
        return copied
    }

    private fun deleteOne(file: File): Int {
        var count = 0
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                count += deleteOne(child)
            }
        }
        if (file.delete() || !file.exists()) count++
        return count
    }

    private fun uniqueName(dir: File, name: String, isDirectory: Boolean): File {
        var dest = File(dir, name)
        if (!dest.exists()) return dest
        val dot = name.lastIndexOf('.')
        val hasExt = !isDirectory && dot > 0
        val base = if (hasExt) name.substring(0, dot) else name
        val ext = if (hasExt) name.substring(dot) else ""
        var index = 1
        while (dest.exists()) {
            dest = File(dir, "$base ($index)$ext")
            index++
        }
        return dest
    }

    private fun preventNesting(source: File, destDir: File) {
        if (!source.isDirectory) return
        val sourcePath = source.canonicalPath
        val destPath = destDir.canonicalPath
        if (destPath == sourcePath || destPath.startsWith(sourcePath + File.separator)) {
            throw IOException("nested")
        }
    }

    private fun requireDirectory(dir: File) {
        if (!dir.isDirectory && !dir.mkdirs()) {
            throw IOException("destination")
        }
    }

    private fun countTree(file: File): Int {
        if (!file.isDirectory) return 1
        var count = 1
        file.listFiles()?.forEach { child ->
            count += countTree(child)
        }
        return count
    }
}
