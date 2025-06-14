package com.yanbin.motioncut.ui.video

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Desktop implementation of optimized video surface with hardware acceleration and frame buffering
 * Performance optimizations:
 * - Circular frame buffer (10 frames ahead)
 * - Background decoding thread
 * - Hardware-accelerated decoding when available
 * - Adaptive frame skipping for real-time playback
 */
actual class VideoSurface actual constructor(private val videoPath: String) {
    private var grabber: FFmpegFrameGrabber? = null
    private var converter: Java2DFrameConverter? = null
    private var playbackJob: Job? = null
    private var decodingJob: Job? = null
    private var frameCallback: ((ImageBitmap, Int) -> Unit)? = null

    // Frame buffering for smooth playback
    private val frameBuffer = ConcurrentLinkedQueue<ImageBitmap>()
    private val maxBufferSize = 10
    private val bufferMutex = Mutex()

    // Playback timing
    private var frameRate: Double = 30.0
    private var frameDelay: Long = 33L // ~30 FPS
    private var isInitialized = false
    private var shouldPlay = false

    // Video rotation handling
    private var videoRotation: Int = 0 // Rotation angle in degrees

    // Performance monitoring
    private var lastFrameTime = 0L
    private var droppedFrames = 0

    actual suspend fun initialize(onFrameUpdate: (ImageBitmap, Int) -> Unit) {
        frameCallback = onFrameUpdate

        withContext(Dispatchers.IO) {
            try {
                grabber = FFmpegFrameGrabber(videoPath).apply {
                    // Don't force specific codec - let FFmpeg auto-detect
                    // setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264)
                    // Use default pixel format for better compatibility
                    // pixelFormat = org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGB24
                    start()
                }

                converter = Java2DFrameConverter()
                frameRate = grabber?.frameRate?.takeIf { it > 0 } ?: 30.0
                frameDelay = (1000 / frameRate).toLong()

                // Detect video rotation from metadata
                videoRotation = detectVideoRotation(grabber)

                // Load first frame as thumbnail - try multiple frames if needed
                var firstFrame = grabber?.grab()
                var attempts = 0
                while (firstFrame != null && firstFrame.image == null && attempts < 10) {
                    firstFrame = grabber?.grab()
                    attempts++
                }

                if (firstFrame != null && firstFrame.image != null) {
                    val bufferedImage = converter?.convert(firstFrame)
                    val imageBitmap = bufferedImage?.toOptimizedImageBitmap()

                    if (imageBitmap != null) {
                        withContext(Dispatchers.Main) {
                            frameCallback?.invoke(imageBitmap, videoRotation)
                        }
                    }
                }

                // Reset to beginning
                grabber?.restart()
                isInitialized = true

                // Start background frame decoding
                startFrameDecoding()

            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Background frame decoding to keep buffer filled
     */
    private fun startFrameDecoding() {
        decodingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isInitialized) {
                try {
                    // Only decode if buffer has space
                    if (frameBuffer.size < maxBufferSize) {
                        val frame = grabber?.grab()
                        if (frame != null) {
                            // Check if frame has image data
                            if (frame.image != null) {
                                val bufferedImage = converter?.convert(frame)
//                                val rotatedImage = bufferedImage?.let { applyRotation(it, videoRotation) }
                                val imageBitmap = bufferedImage?.toOptimizedImageBitmap()

                                if (imageBitmap != null) {
                                    bufferMutex.withLock {
                                        frameBuffer.offer(imageBitmap)
                                    }
                                }
                            }
                            // If frame.image is null, it might be an audio frame or metadata
                            // Continue to next frame without restarting
                        } else {
                            // End of video, restart
                            grabber?.restart()
                        }
                    } else {
                        // Buffer full, wait a bit
                        delay(frameDelay / 2)
                    }
                } catch (e: Exception) {
                    // Handle decoding errors gracefully
                    delay(frameDelay)
                }
            }
        }
    }

    actual fun play() {
        if (!isInitialized) return

        shouldPlay = true
        playbackJob?.cancel()
        playbackJob = CoroutineScope(Dispatchers.Main).launch {
            lastFrameTime = System.currentTimeMillis()

            while (isActive && shouldPlay) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastFrame = currentTime - lastFrameTime

                // Check if it's time for next frame
                if (timeSinceLastFrame >= frameDelay) {
                    val frame = bufferMutex.withLock {
                        frameBuffer.poll()
                    }

                    if (frame != null) {
                        frameCallback?.invoke(frame, videoRotation)
                        lastFrameTime = currentTime
                    } else {
                        // Buffer empty, skip frame to maintain timing
                        droppedFrames++
                        lastFrameTime = currentTime
                    }
                }

                // Adaptive delay to maintain frame rate
                val nextFrameDelay = maxOf(1L, frameDelay - (System.currentTimeMillis() - currentTime))
                delay(nextFrameDelay)
            }
        }
    }

    actual fun pause() {
        shouldPlay = false
        playbackJob?.cancel()
        playbackJob = null
    }

    actual fun stop() {
        shouldPlay = false
        playbackJob?.cancel()
        decodingJob?.cancel()

        try {
            grabber?.stop()
            grabber?.release()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }

        frameBuffer.clear()
    }

    /**
     * Detect video rotation from metadata using FFmpegFrameGrabber's built-in method
     */
    private fun detectVideoRotation(grabber: FFmpegFrameGrabber?): Int {
        return try {
            // Use FFmpegFrameGrabber's built-in getDisplayRotation method
            val rotation = grabber?.displayRotation ?: 0.0
            
            // Convert double to int and normalize to standard rotation values
            val rotationInt = rotation.toInt()
            -rotationInt
        } catch (e: Exception) {
            0 // Default to no rotation on error
        }
    }

    /**
     * Apply rotation to BufferedImage
     */
    private fun applyRotation(image: BufferedImage, rotation: Int): BufferedImage {
        if (rotation == 0) return image

        val radians = Math.toRadians(rotation.toDouble())
        val sin = Math.abs(Math.sin(radians))
        val cos = Math.abs(Math.cos(radians))

        val originalWidth = image.width
        val originalHeight = image.height

        // Calculate new dimensions after rotation
        val newWidth = (originalWidth * cos + originalHeight * sin).toInt()
        val newHeight = (originalWidth * sin + originalHeight * cos).toInt()

        // Create new image with rotated dimensions
        val rotatedImage = BufferedImage(newWidth, newHeight, image.type)
        val g2d: Graphics2D = rotatedImage.createGraphics()

        try {
            // Set rendering hints for better quality
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Create transformation
            val transform = AffineTransform()
            
            // Translate to center of new image
            transform.translate(newWidth / 2.0, newHeight / 2.0)
            
            // Apply rotation
            transform.rotate(radians)
            
            // Translate back by half of original image size
            transform.translate(-originalWidth / 2.0, -originalHeight / 2.0)

            // Apply transformation and draw image
            g2d.transform = transform
            g2d.drawImage(image, 0, 0, null)

        } finally {
            g2d.dispose()
        }

        return rotatedImage
    }
}