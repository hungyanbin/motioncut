package com.yanbin.motioncut.domain

/**
 * Represents a video file with its metadata
 */
data class VideoFile(
    val name: String,
    val path: String,
    val size: Long,
    val type: String,
    val extension: String
) {
    /**
     * Returns a human-readable file size string
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Returns the file type description based on extension
     */
    fun getTypeDescription(): String {
        return when (extension.lowercase()) {
            "mp4" -> "MP4 Video"
            "avi" -> "AVI Video"
            "mov" -> "QuickTime Movie"
            "mkv" -> "Matroska Video"
            "wmv" -> "Windows Media Video"
            "flv" -> "Flash Video"
            "webm" -> "WebM Video"
            "m4v" -> "iTunes Video"
            else -> "Video File"
        }
    }
    
    /**
     * Checks if the file is a supported video format
     */
    fun isSupportedVideoFormat(): Boolean {
        val supportedExtensions = setOf("mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v")
        return extension.lowercase() in supportedExtensions
    }
}