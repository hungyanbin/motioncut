package com.yanbin.motioncut.ui.video

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import java.awt.image.BufferedImage
import org.bytedeco.ffmpeg.global.avutil

actual class VideoPlayer actual constructor(private val videoPath: String) {
    private var grabber: FFmpegFrameGrabber? = null
    private var converter: Java2DFrameConverter? = null
    private var playbackJob: Job? = null
    private var decodingJob: Job? = null
    private var frameCallback: ((ImageBitmap) -> Unit)? = null

    // Frame buffering for smooth playback with timestamp support
    private val frameBuffer = TimestampFrameBuffer(defaultCapacity = 200)
    private var currentFrameTimestamp = 0L

    // Flow-based frame processing
    private var frameFlow: Flow<Frame>? = null
    private var prefetchJob: Job? = null
    
    // State tracking
    private var isEndOfVideo = false

    // Playback timing
    private var frameRate: Double = 30.0
    private var frameDelay: Long = 33L // ~30 FPS
    private var isInitialized = false
    private var shouldPlay = false
    
    // Real-time playback control
    private var videoStartTime: Long = 0L // System time when video playback started

    // Video rotation handling
    private var videoRotation: Int = 0 // Rotation angle in degrees

    // Performance monitoring
    private var droppedFrames = 0
    
    // Video duration and position tracking
    private var videoDurationMs: Long = 0L

    actual suspend fun initialize(onFrameUpdate: (ImageBitmap) -> Unit) {
        frameCallback = onFrameUpdate

        withContext(Dispatchers.IO) {
            try {
                // Suppress FFmpeg log messages to reduce noise
                avutil.av_log_set_level(avutil.AV_LOG_ERROR)
                
                grabber = FFmpegFrameGrabber(videoPath).apply {
                    // Use RGB24 pixel format to avoid deprecated yuv420p warnings
                    start()
                }

                converter = Java2DFrameConverter()
                frameRate = grabber?.frameRate?.takeIf { it > 0 } ?: 30.0
                frameDelay = (1000 / frameRate).toLong()

                // Get video duration
                videoDurationMs = (grabber?.lengthInTime ?: 0L) / 1000L // Convert from microseconds to milliseconds

                // Detect video rotation from metadata
                videoRotation = detectVideoRotation(grabber)

                // Reset to beginning
                currentFrameTimestamp = 0L
                isInitialized = true

                // Create frame flow and start prefetching
                startFrameFlow()
                startPrefetching()

            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Create a Flow that produces video frames on-demand
     */
    private fun startFrameFlow() {
        frameFlow = flow {
            while (isInitialized && !isEndOfVideo) {
                try {
                    val frame = grabber?.grab()
                    if (frame != null && frame.image != null) {
                        val bufferedImage = converter?.convert(frame)
                        if (bufferedImage != null) {
                            val imageBitmap = bufferedImage.toOptimizedImageBitmap(videoRotation)
                            val timestamp = (grabber?.timestamp ?: 0L) / 1000L // Convert to milliseconds
                            val videoFrame = Frame(imageBitmap, timestamp)
                            emit(videoFrame)
                        }
                    } else if (frame == null) {
                        // End of video reached
                        isEndOfVideo = true
                        break
                    }
                } catch (e: Exception) {
                    // Handle grabbing errors gracefully
                    delay(frameDelay)
                }
            }
        }.flowOn(Dispatchers.IO)
    }
    
    /**
     * Start prefetching frames until buffer is full
     */
    private fun startPrefetching() {
        prefetchJob = CoroutineScope(Dispatchers.IO).launch {
            frameFlow?.collect { frame ->
                while (frameBuffer.wouldRemoveFrameAtOrAfter(currentFrameTimestamp)) {
                    // Delay and try again
                    delay(30)
                }
                frameBuffer.appendFrame(frame)
            }
        }
    }

    actual fun play() {
        if (!isInitialized) return

        shouldPlay = true
        playbackJob?.cancel()
        
        // Set video start time, accounting for current position if resuming
        videoStartTime = System.currentTimeMillis() - currentFrameTimestamp

        startPlaybackJob()
    }

    private fun startPlaybackJob() {
        playbackJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && shouldPlay) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - videoStartTime // Time elapsed since video started

                // Calculate which frame should be displayed based on elapsed time
                val targetTimestamp = elapsedTime

                // Get the closest frame to the target timestamp
                val frame = frameBuffer.getClosestFrame(targetTimestamp)

                if (frame != null) {
                    // Only update if this is a different frame than currently displayed
                    if (frame.timestamp != currentFrameTimestamp) {
                        frameCallback?.invoke(frame.imageBitmap)
                        currentFrameTimestamp = frame.timestamp
                    }
                } else {
                    // Check if we've reached end of video
                    if (isEndOfVideo && frameBuffer.isEmpty) {
                        // No more frames and end of video reached, stop playing
                        shouldPlay = false
                        break
                    } else {
                        // No frame available for current time, increment dropped frames
                        droppedFrames++
                    }
                }

                // Small delay to prevent excessive CPU usage
                delay(16) // ~60 FPS check rate
            }
        }
    }

    actual fun pause() {
        shouldPlay = false
        playbackJob?.cancel()
        playbackJob = null
        
        // Calculate the current video position when pausing
        if (videoStartTime > 0) {
            val pausedAt = System.currentTimeMillis() - videoStartTime
            currentFrameTimestamp = pausedAt
        }
    }
    
    /**
     * Seek to a specific timestamp in milliseconds
     * This will attempt to find and display the closest frame to the given timestamp
     */
    actual suspend fun seekTo(timestampMs: Long) {
        if (!isInitialized) return
        
        // Update current frame timestamp
        currentFrameTimestamp = timestampMs
        
        // Always update video start time to maintain sync, regardless of play state
        // This ensures that when seeking during playback, the timing remains correct
        videoStartTime = System.currentTimeMillis() - timestampMs
        
        // Try to get the closest frame from buffer
        val frame = frameBuffer.getNextFrame(timestampMs)
        if (frame != null) {
            println("[seekTo] have frame ${timestampMs} ${frame} ")
            withContext(Dispatchers.Main) {
                frameCallback?.invoke(frame.imageBitmap)
            }
        } else {
            // Frame not in buffer, seek in the grabber
            if (playbackJob?.isActive == true) {
                playbackJob?.cancel()
                frameBuffer.resetBuffer()
                grabber?.timestamp = timestampMs * 1000L // Convert to microseconds
                println("[seekTo] set grabber timestamp ${timestampMs * 1000L}")
                startPlaybackJob()
            }
        }
    }
    
    /**
     * Get the time range of currently buffered frames
     */
    fun getBufferedTimeRange(): Pair<Long, Long> {
        return Pair(frameBuffer.startTimeOfAvailableFrames, frameBuffer.endTimeOfAvailableFrames)
    }

    actual fun stop() {
        shouldPlay = false
        playbackJob?.cancel()
        decodingJob?.cancel()
        
        // Cancel all flow-based jobs
        prefetchJob?.cancel()

        try {
            grabber?.stop()
            grabber?.release()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }

        runBlocking { frameBuffer.clear() }
        
        // Reset state
        prefetchJob = null
        frameFlow = null
        isEndOfVideo = false
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
    
    actual fun getCurrentPosition(): Long {
        return currentFrameTimestamp
    }
    
    actual fun getDuration(): Long {
        return videoDurationMs
    }
}

/**
 * Optimized BufferedImage to ImageBitmap conversion
 * Performance improvements:
 * 1. Direct pixel data access - no PNG encoding/decoding
 * 2. Efficient color space conversion
 * 3. Minimal memory allocation
 */
private fun BufferedImage.toOptimizedImageBitmap(rotation: Int): ImageBitmap {
    return when (type) {
        BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB -> {
            // Fast path: Use built-in extension function which handles color mapping correctly
            this.toComposeImageBitmap(rotation)
        }
        else -> {
            // Fallback: Convert to compatible format first, then use fast path
            toCompatibleFormat().toComposeImageBitmap(rotation)
        }
    }
}

// The orientation could be 0, 90, 180, 270
private fun BufferedImage.toComposeImageBitmap(rotation: Int): ImageBitmap {
    // Normalize rotation to 0, 90, 180, 270
    val normalizedRotation = ((rotation % 360) + 360) % 360
    
    // Determine final dimensions after rotation
    val (finalWidth, finalHeight) = when (normalizedRotation) {
        90, 270 -> height to width  // Swap dimensions for 90° and 270° rotations
        else -> width to height     // Keep original dimensions for 0° and 180°
    }
    
    val bytesPerPixel = 4
    val pixels = ByteArray(finalWidth * finalHeight * bytesPerPixel)

    var k = 0
    for (y in 0 until finalHeight) {
        for (x in 0 until finalWidth) {
            // Calculate source coordinates based on rotation
            // For each destination pixel (x,y), find where it should come from in the source
            val (srcX, srcY) = when (normalizedRotation) {
                0 -> x to y                                    // No rotation
                90 -> y to (height - 1 - x)                   // 90° clockwise: dest(x,y) <- src(y, height-1-x)
                180 -> (width - 1 - x) to (height - 1 - y)    // 180°: dest(x,y) <- src(width-1-x, height-1-y)
                270 -> (width - 1 - y) to x                   // 270° clockwise: dest(x,y) <- src(width-1-y, x)
                else -> x to y                                 // Fallback to no rotation
            }
            
            // Ensure source coordinates are within bounds
            if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
                val argb = getRGB(srcX, srcY)
                val a = (argb shr 24) and 0xff
                val r = (argb shr 16) and 0xff
                val g = (argb shr 8) and 0xff
                val b = (argb shr 0) and 0xff
                pixels[k++] = b.toByte()
                pixels[k++] = g.toByte()
                pixels[k++] = r.toByte()
                pixels[k++] = a.toByte()
            } else {
                // Fill with transparent pixels if out of bounds
                pixels[k++] = 0
                pixels[k++] = 0
                pixels[k++] = 0
                pixels[k++] = 0
            }
        }
    }

    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeS32(finalWidth, finalHeight, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(pixels)
    return bitmap.asComposeImageBitmap()
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
