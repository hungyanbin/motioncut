package com.yanbin.motioncut.ui.video

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage

actual class VideoSurface actual constructor(private val videoPath: String) {
    private var grabber: FFmpegFrameGrabber? = null
    private var converter: Java2DFrameConverter? = null
    private var playbackJob: Job? = null
    private var decodingJob: Job? = null
    private var frameCallback: ((ImageBitmap, Int) -> Unit)? = null

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

                // Reset to beginning
                currentFrameTimestamp = 0L
                isInitialized = true

                // Create frame flow and start prefetching
                createFrameFlow()
                startPrefetching()

            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Create a Flow that produces video frames on-demand
     */
    private fun createFrameFlow() {
        frameFlow = flow {
            while (isInitialized && !isEndOfVideo) {
                try {
                    val frame = grabber?.grab()
                    if (frame != null && frame.image != null) {
                        val bufferedImage = converter?.convert(frame)
                        if (bufferedImage != null) {
                            val imageBitmap = bufferedImage.toOptimizedImageBitmap()
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
                        frameCallback?.invoke(frame.imageBitmap, videoRotation)
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
    suspend fun seekTo(timestampMs: Long) {
        if (!isInitialized) return
        
        // Update current frame timestamp
        currentFrameTimestamp = timestampMs
        
        // If playing, adjust video start time to maintain sync
        if (shouldPlay) {
            videoStartTime = System.currentTimeMillis() - timestampMs
        }
        
        // Try to get the closest frame from buffer
        val frame = frameBuffer.getClosestFrame(timestampMs)
        if (frame != null) {
            withContext(Dispatchers.Main) {
                frameCallback?.invoke(frame.imageBitmap, videoRotation)
            }
        } else {
            // Frame not in buffer, seek in the grabber
            try {
                grabber?.timestamp = timestampMs * 1000L // Convert to microseconds
            } catch (e: Exception) {
                // Handle seek errors gracefully
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
