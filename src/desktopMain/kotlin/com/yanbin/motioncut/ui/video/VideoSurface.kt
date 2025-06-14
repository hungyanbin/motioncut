package com.yanbin.motioncut.ui.video

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Desktop implementation of optimized video surface with parallel pipeline processing
 * Performance optimizations:
 * - Parallel 3-stage pipeline: Frame Grabbing -> Frame Conversion -> Image Bitmap Optimization
 * - Circular frame buffer (10 frames ahead)
 * - Background processing with separate threads for each pipeline stage
 * - Hardware-accelerated decoding when available
 * - Adaptive frame skipping for real-time playback
 *
 * Pipeline Architecture:
 * Thread 1: grabber?.grab() -> Frame N+2
 * Thread 2: toOptimizedImageBitmap() -> Frame N
 */
actual class VideoSurface actual constructor(private val videoPath: String) {
    private var grabber: FFmpegFrameGrabber? = null
    private var converter: Java2DFrameConverter? = null
    private var playbackJob: Job? = null
    private var decodingJob: Job? = null
    private var frameCallback: ((ImageBitmap, Int) -> Unit)? = null

    // Frame buffering for smooth playback
    private val frameBuffer = ConcurrentLinkedQueue<ImageBitmap>()
    private val maxBufferSize = 200
    private val bufferMutex = Mutex()

    // Pipeline channels for parallel processing
    private var bufferedImageChannel: Channel<BufferedImage>? = null
    
    // Pipeline jobs
    private var frameGrabJob: Job? = null
    private var imageBitmapJob: Job? = null

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
     * Background frame decoding with parallel pipeline processing
     * Pipeline: Frame Grabbing -> Frame Conversion -> Image Bitmap Optimization
     */
    private fun startFrameDecoding() {
        // Initialize channels for pipeline communication
        bufferedImageChannel = Channel<BufferedImage>(capacity = 3)
        
        // Stage 1: Frame Grabbing Thread
        frameGrabJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isInitialized) {
                try {
                    val frame = grabber?.grab()
                    if (frame != null && frame.image != null) {
                        val bufferedImage = converter?.convert(frame)
                        if (bufferedImage != null) {
                            bufferedImageChannel?.send(bufferedImage)
                        }
                    } else if (frame == null) {
                        // End of video, restart
                        grabber?.restart()
                    }
                    // If frame.image is null, it might be an audio frame - continue
                } catch (e: Exception) {
                    // Handle grabbing errors gracefully
                    delay(frameDelay)
                }
            }
        }
        
        // Stage 2: Multiple Image Bitmap Optimization Workers
        imageBitmapJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive && isInitialized) {
                    try {
                        // Only process if buffer has space
                        if (frameBuffer.size < maxBufferSize) {
                            val bufferedImage = bufferedImageChannel?.receive()
                            if (bufferedImage != null) {
                                val imageBitmap = bufferedImage.toOptimizedImageBitmap()
                                bufferMutex.withLock {
                                    frameBuffer.offer(imageBitmap)
                                }
                            }
                        } else {
                            // Buffer full, wait a bit before processing next frame
                            delay(frameDelay / 2)
                        }
                    } catch (e: Exception) {
                        // Handle bitmap optimization errors gracefully
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
        
        // Pause pipeline jobs but don't cancel them completely
        // They will continue buffering frames in the background
    }

    actual fun stop() {
        shouldPlay = false
        playbackJob?.cancel()
        decodingJob?.cancel()
        
        // Cancel all pipeline jobs
        frameGrabJob?.cancel()
        imageBitmapJob?.cancel()
        
        // Close channels
        bufferedImageChannel?.close()

        try {
            grabber?.stop()
            grabber?.release()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }

        frameBuffer.clear()
        
        // Reset pipeline components
        bufferedImageChannel = null
        frameGrabJob = null
        imageBitmapJob = null
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
}

/**
 * Optimized BufferedImage to ImageBitmap conversion
 * Performance improvements:
 * 1. Direct pixel data access - no PNG encoding/decoding
 * 2. Efficient color space conversion
 * 3. Minimal memory allocation
 */
internal fun BufferedImage.toOptimizedImageBitmap(): ImageBitmap {
    return when (type) {
        BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB -> {
            // Fast path: Use built-in extension function which handles color mapping correctly
            this.toComposeImageBitmap()
        }
        else -> {
            // Fallback: Convert to compatible format first, then use fast path
            toCompatibleFormat().toComposeImageBitmap()
        }
    }
}

/**
 * Alternative direct conversion - use this if you want to experiment with manual pixel manipulation
 */
internal fun BufferedImage.toOptimizedImageBitmapDirect(): ImageBitmap {
    return when (type) {
        BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB -> {
            toImageBitmapDirect()
        }
        else -> {
            toCompatibleFormat().toImageBitmapDirect()
        }
    }
}

/**
 * Direct conversion using pixel data - fastest method
 */
private fun BufferedImage.toImageBitmapDirect(): ImageBitmap {
    val width = width
    val height = height
    val hasAlpha = colorModel.hasAlpha()
    
    // Get pixel data directly from BufferedImage
    val pixels = IntArray(width * height)
    getRGB(0, 0, width, height, pixels, 0, width)
    
    // Convert IntArray to ByteArray for Skia (RGBA format)
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val argb = pixels[i]
        val baseIndex = i * 4
        bytes[baseIndex] = ((argb shr 16) and 0xFF).toByte()     // R
        bytes[baseIndex + 1] = ((argb shr 8) and 0xFF).toByte()  // G
        bytes[baseIndex + 2] = (argb and 0xFF).toByte()          // B
        bytes[baseIndex + 3] = ((argb shr 24) and 0xFF).toByte() // A
    }
    
    // Create Skia Image directly from bytes
    val imageInfo = org.jetbrains.skia.ImageInfo.makeN32(
        width,
        height,
        if (hasAlpha) org.jetbrains.skia.ColorAlphaType.UNPREMUL
        else org.jetbrains.skia.ColorAlphaType.OPAQUE
    )
    
    val skImage = org.jetbrains.skia.Image.makeRaster(imageInfo, bytes, width * 4)
    return skImage.toComposeImageBitmap()
}

/**
 * Convert BufferedImage to a compatible format for fast processing
 */
private fun BufferedImage.toCompatibleFormat(): BufferedImage {
    val compatibleImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2d = compatibleImage.createGraphics()
    try {
        g2d.drawImage(this, 0, 0, null)
    } finally {
        g2d.dispose()
    }
    return compatibleImage
}
