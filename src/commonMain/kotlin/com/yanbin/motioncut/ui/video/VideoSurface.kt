package com.yanbin.motioncut.ui.video

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Expected class for optimized video surface - will be implemented platform-specifically
 */
expect class VideoSurface(videoPath: String) {
    suspend fun initialize(onFrameUpdate: (ImageBitmap, Int) -> Unit)
    fun play()
    fun pause()
    fun stop()
}
