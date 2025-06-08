package com.yanbin.motioncut.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanbin.motioncut.domain.VideoFile
import org.jetbrains.skia.Bitmap.Companion.makeFromImage
import org.jetbrains.skia.Image.Companion.makeFromBitmap
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min

/**
 * Expected class for optimized video player - will be implemented platform-specifically
 */
expect class VideoPlayer(videoPath: String) {
    suspend fun initialize(onFrameUpdate: (ImageBitmap?) -> Unit)
    fun play()
    fun pause()
    fun stop()
}

/**
 * Expected function for video display - will be implemented platform-specifically
 */
@Composable
fun VideoDisplayArea(
    videoFile: VideoFile,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var videoPlayer by remember { mutableStateOf<VideoPlayer?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialize optimized video player
    LaunchedEffect(videoFile.path) {
        isLoading = true
        errorMessage = null

        try {
            val file = File(videoFile.path)
            if (!file.exists()) {
                errorMessage = "Video file not found"
                isLoading = false
                return@LaunchedEffect
            }

            val player = VideoPlayer(videoFile.path)
            player.initialize { frame ->
                currentFrame = frame
            }

            videoPlayer = player
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Failed to load video: ${e.message}"
            isLoading = false
        }
    }

    // Handle play/pause state changes
    LaunchedEffect(isPlaying) {
        videoPlayer?.let { player ->
            if (isPlaying) {
                player.play()
            } else {
                player.pause()
            }
        }
    }

    // Cleanup when component is disposed
    DisposableEffect(videoFile.path) {
        onDispose {
            videoPlayer?.stop()
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                LoadingIndicator()
            }
            errorMessage != null -> {
                ErrorDisplay(errorMessage!!)
            }
            currentFrame != null -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawOptimizedVideoFrame(currentFrame!!)
                }
            }
            else -> {
                PlaceholderDisplay(videoFile)
            }
        }
    }
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

/**
 * Optimized video frame drawing with aspect ratio preservation and scaling
 */
fun DrawScope.drawOptimizedVideoFrame(frame: ImageBitmap) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val frameWidth = frame.width.toFloat()
    val frameHeight = frame.height.toFloat()
    
    // Calculate scaling to fit while maintaining aspect ratio
    val scaleX = canvasWidth / frameWidth
    val scaleY = canvasHeight / frameHeight
    val scale = min(scaleX, scaleY)
    
    val scaledWidth = frameWidth * scale
    val scaledHeight = frameHeight * scale
    
    // Center the frame
    val offsetX = (canvasWidth - scaledWidth) / 2
    val offsetY = (canvasHeight - scaledHeight) / 2
    
    drawImage(
        image = frame,
        dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
        dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt())
    )
}

/**
 * Loading indicator for video initialization
 */
@Composable
fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading optimized video player...",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

/**
 * Error display for video loading failures
 */
@Composable
fun ErrorDisplay(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Video Player Error",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Supported formats: MP4, AVI, MOV, MKV, WebM",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

/**
 * Placeholder display when video is ready but not yet playing
 */
@Composable
fun PlaceholderDisplay(videoFile: VideoFile) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎬",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Optimized Video Ready",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hardware-accelerated playback",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = videoFile.name,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}