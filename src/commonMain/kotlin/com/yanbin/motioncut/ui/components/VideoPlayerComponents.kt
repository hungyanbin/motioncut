package com.yanbin.motioncut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yanbin.motioncut.domain.VideoFile
import com.yanbin.motioncut.ui.widget.Pause

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
                .aspectRatio(16f / 9f) // Standard video aspect ratio
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
