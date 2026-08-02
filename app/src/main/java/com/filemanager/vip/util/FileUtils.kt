package com.filemanager.vip.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    /** Get all storage roots (internal + SD cards) */
    fun getStorageRoots(): List<File> {
        val roots = mutableListOf<File>()
        val internal = Environment.getExternalStorageDirectory()
        if (internal.exists()) roots.add(internal)

        // Check for secondary storage (SD card)
        val secondary = System.getenv("SECONDARY_STORAGE")
        secondary?.split(":")?.forEach { path ->
            val f = File(path)
            if (f.exists() && f.canRead()) roots.add(f)
        }
        return roots.distinctBy { it.absolutePath }
    }

    /** Get primary external storage root */
    fun getPrimaryRoot(): File = Environment.getExternalStorageDirectory()

    fun getHomeDirectory(): File = Environment.getExternalStorageDirectory()

    /** List files in directory, sorted: folders first then files */
    fun listFiles(dir: File): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val files = dir.listFiles() ?: return emptyList()
        return files
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    /** Recursively count files in a directory (for progress stats) */
    fun countFiles(dir: File): Pair<Long, Long> {
        var fileCount = 0L
        var totalSize = 0L
        dir.walkTopDown().forEach { f ->
            if (f.isFile) {
                fileCount++
                totalSize += f.length()
            }
            if (fileCount > 5000) return fileCount to totalSize // safety limit
        }
        return fileCount to totalSize
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.size - 1) {
            value /= 1024
            unit++
        }
        return if (unit == 0) "${bytes} ${units[unit]}" else String.format(Locale.US, "%.2f %s", value, units[unit])
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    /** Get storage usage info for a path */
    fun getStorageInfo(path: File): Triple<Long, Long, Long> {
        return try {
            val stat = StatFs(path.absolutePath)
            val total = stat.totalBytes
            val available = stat.availableBytes
            val used = total - available
            Triple(total, used, available)
        } catch (e: Exception) {
            Triple(0L, 0L, 0L)
        }
    }

    /** Copy file or directory */
    fun copy(src: File, destDir: File): Boolean {
        return try {
            val dest = File(destDir, src.name)
            if (src.isDirectory) {
                if (!dest.exists()) dest.mkdirs()
                src.listFiles()?.forEach { child ->
                    copy(child, dest)
                }
            } else {
                dest.parentFile?.mkdirs()
                src.copyTo(dest, overwrite = true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Move file or directory */
    fun move(src: File, destDir: File): Boolean {
        return try {
            if (src.parentFile?.absolutePath == destDir.absolutePath) return true
            val dest = File(destDir, src.name)
            if (src.renameTo(dest)) {
                true
            } else {
                // Fallback: copy + delete
                if (copy(src, destDir)) {
                    src.deleteRecursively()
                    true
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Delete file or directory */
    fun delete(file: File): Boolean {
        return try {
            file.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    /** Search files by name in a directory (recursive with depth limit) */
    fun search(dir: File, query: String, maxResults: Int = 200): List<File> {
        val results = mutableListOf<File>()
        val q = query.lowercase()
        fun walk(f: File, depth: Int) {
            if (results.size >= maxResults) return
            if (depth > 4) return
            val children = f.listFiles() ?: return
            for (child in children) {
                if (child.name.lowercase().contains(q)) {
                    results.add(child)
                    if (results.size >= maxResults) return
                }
                if (child.isDirectory) walk(child, depth + 1)
            }
        }
        walk(dir, 0)
        return results
    }
}
