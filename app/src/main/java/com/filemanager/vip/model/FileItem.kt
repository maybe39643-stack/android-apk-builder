package com.filemanager.vip.model

import com.filemanager.vip.R
import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified(),
    val isFavorite: Boolean = false
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "").lowercase()

    val category: FileCategory
        get() = FileCategory.fromExtension(extension, isDirectory)
}

enum class FileCategory(val labelRes: Int) {
    FOLDER(R.string.folder),
    IMAGE(R.string.images),
    VIDEO(R.string.videos),
    AUDIO(R.string.audio),
    DOC(R.string.documents),
    APK(R.string.apk),
    DOWNLOAD(R.string.downloads),
    FILE(R.string.file);

    companion object {
        val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic")
        val VIDEO_EXT = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v")
        val AUDIO_EXT = setOf("mp3", "wav", "aac", "flac", "ogg", "m4a", "wma", "amr")
        val DOC_EXT = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv")
        val DOWNLOAD_EXT = setOf("zip", "rar", "7z", "tar", "gz", "iso", "torrent")

        fun fromExtension(ext: String, isDir: Boolean): FileCategory {
            if (isDir) return FOLDER
            return when (ext) {
                in IMAGE_EXT -> IMAGE
                in VIDEO_EXT -> VIDEO
                in AUDIO_EXT -> AUDIO
                in DOC_EXT -> DOC
                "apk" -> APK
                in DOWNLOAD_EXT -> DOWNLOAD
                else -> FILE
            }
        }
    }
}
