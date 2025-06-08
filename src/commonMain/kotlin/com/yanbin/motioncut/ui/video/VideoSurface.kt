package com.yanbin.motioncut.ui.video

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap.Companion.makeFromImage
import org.jetbrains.skia.Image.Companion.makeFromBitmap
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Expected class for optimized video surface - will be implemented platform-specifically
 */
expect class VideoSurface(videoPath: String) {
    suspend fun initialize(onFrameUpdate: (ImageBitmap?) -> Unit)
    fun play()
    fun pause()
    fun stop()
}

/**
 * Optimized BufferedImage to ImageBitmap conversion
 */
internal fun BufferedImage.toOptimizedImageBitmap(): ImageBitmap {
    // Convert BufferedImage to ImageBitmap using toComposeImageBitmap
    return makeFromBitmap(
        makeFromImage(
            makeFromEncoded(
                ByteArrayOutputStream().apply {
                    ImageIO.write(this@toOptimizedImageBitmap, "png", this)
                }.toByteArray()
            )
        )
    ).toComposeImageBitmap()
}