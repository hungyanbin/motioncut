package com.yanbin.motioncut.ui.video

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Expected class for optimized video surface - will be implemented platform-specifically
 */
expect class VideoPlayer(videoPath: String) {
    suspend fun initialize(onFrameUpdate: (ImageBitmap) -> Unit)
    fun play()
    fun pause()
    fun stop()
    suspend fun seekTo(timestampMs: Long)
    fun getCurrentPosition(): Long
    fun getDuration(): Long
}
