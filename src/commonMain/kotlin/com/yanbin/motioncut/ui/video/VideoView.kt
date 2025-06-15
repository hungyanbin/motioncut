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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanbin.motioncut.domain.VideoFile
import com.yanbin.motioncut.ui.widget.Pause
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VideoView(
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
    val alphaInPausing = 0.5f
    val alphaInPlaying = 0.2f
    val alphaValue = if (isPlaying) alphaInPlaying else alphaInPausing
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
                Color.White.copy(alpha = alphaValue),
                CircleShape
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colors.primary.copy(alpha = alphaValue),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colors.primary.copy(alpha = alphaValue)
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
    var currentOrientation by remember { mutableIntStateOf(0)}
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
            player.initialize { frame, videoOrientation ->
                currentFrame = frame
                currentOrientation = videoOrientation
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
                    drawOptimizedVideoFrame(currentFrame, currentOrientation)
                }
            }
            else -> {
                PlaceholderDisplay(videoFile)
            }
        }
    }
}

/**
 * Optimized video frame drawing with aspect ratio preservation, fit-to-view scaling, and rotation support
 */
fun DrawScope.drawOptimizedVideoFrame(bitmap: ImageBitmap?, orientation: Int) {
    val frame = bitmap ?: return
    val canvasWidth = size.width
    val canvasHeight = size.height
    
    // If no rotation needed, use simple drawing
    if (orientation == 0) {
        drawSimpleFrame(frame, canvasWidth, canvasHeight)
        return
    }
    
    // Apply rotation transformation similar to desktop implementation
    val radians = Math.toRadians(orientation.toDouble())
    val sinAbs = abs(sin(radians))
    val cosAbs = abs(cos(radians))
    
    val originalWidth = frame.width.toFloat()
    val originalHeight = frame.height.toFloat()
    
    // Calculate rotated dimensions
    val rotatedWidth = (originalWidth * cosAbs + originalHeight * sinAbs).toFloat()
    val rotatedHeight = (originalWidth * sinAbs + originalHeight * cosAbs).toFloat()
    
    // Calculate aspect ratios using rotated dimensions
    val canvasAspectRatio = canvasWidth / canvasHeight
    val rotatedAspectRatio = rotatedWidth / rotatedHeight
    
    // Calculate scaling to fit the entire view while maintaining aspect ratio
    val scale = if (rotatedAspectRatio > canvasAspectRatio) {
        // Rotated frame is wider than canvas - fit to width
        canvasWidth / rotatedWidth
    } else {
        // Rotated frame is taller than canvas - fit to height
        canvasHeight / rotatedHeight
    }
    
    val scaledWidth = originalWidth * scale
    val scaledHeight = originalHeight * scale
    
    // Center the rotation around the canvas center
    val centerX = canvasWidth / 2
    val centerY = canvasHeight / 2
    
    // Apply rotation and draw the image
    rotate(
        degrees = orientation.toFloat(),
        pivot = Offset(centerX, centerY)
    ) {
        // Center the original (unrotated) frame
        val offsetX = centerX - scaledWidth / 2
        val offsetY = centerY - scaledHeight / 2
        
        drawImage(
            image = frame,
            dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
            dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt())
        )
    }
}

/**
 * Helper function for drawing frames without rotation
 */
private fun DrawScope.drawSimpleFrame(frame: ImageBitmap, canvasWidth: Float, canvasHeight: Float) {
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
            text = "Video Ready",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = videoFile.name,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}
