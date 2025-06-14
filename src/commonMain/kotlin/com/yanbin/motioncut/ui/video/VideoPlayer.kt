package com.yanbin.motioncut.ui.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanbin.motioncut.domain.VideoFile
import com.yanbin.motioncut.ui.widget.Pause
import java.io.File
import kotlin.math.min

/**
 * Video player component with optimized frame-by-frame playback
 */
@Composable
fun VideoPlayer(
    videoFile: VideoFile,
    modifier: Modifier = Modifier,
    onPlayPause: (Boolean) -> Unit = {},
    isPlaying: Boolean = false
) {
    var currentIsPlaying by remember { mutableStateOf(isPlaying) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            // Optimized video display area with frame buffering
            VideoDisplayArea(
                videoFile = videoFile,
                isPlaying = currentIsPlaying,
                modifier = Modifier.fillMaxSize()
            )
            
            // Play/Pause button overlay
            PlayButton(
                isPlaying = currentIsPlaying,
                onClick = {
                    currentIsPlaying = !currentIsPlaying
                    onPlayPause(currentIsPlaying)
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Circular play/pause button with smooth animations
 */
@Composable
private fun PlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
                Color.White.copy(alpha = 0.9f),
                CircleShape
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colors.primary,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colors.primary
        )
    }
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
    var videoPlayer by remember { mutableStateOf<VideoSurface?>(null) }
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

            val player = VideoSurface(videoFile.path)
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
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
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
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
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
 * Optimized video frame drawing with aspect ratio preservation and fit-to-view scaling
 */
fun DrawScope.drawOptimizedVideoFrame(frame: ImageBitmap) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val frameWidth = frame.width.toFloat()
    val frameHeight = frame.height.toFloat()

    // Calculate aspect ratios
    val canvasAspectRatio = canvasWidth / canvasHeight
    val frameAspectRatio = frameWidth / frameHeight

    // Calculate scaling to fit the entire view while maintaining aspect ratio
    val scale = if (frameAspectRatio > canvasAspectRatio) {
        // Frame is wider than canvas - fit to width
        canvasWidth / frameWidth
    } else {
        // Frame is taller than canvas - fit to height
        canvasHeight / frameHeight
    }

    val scaledWidth = frameWidth * scale
    val scaledHeight = frameHeight * scale

    // Center the frame in the available space
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
