package com.yanbin.motioncut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanbin.motioncut.domain.VideoFile

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
            
            // Video information overlay
            VideoInfoOverlay(
                videoFile = videoFile,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
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
 * Video information overlay showing file details
 */
@Composable
private fun VideoInfoOverlay(
    videoFile: VideoFile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = videoFile.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = videoFile.getTypeDescription(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Text(
                    text = videoFile.getFormattedSize(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Compact video player for smaller spaces
 */
@Composable
fun CompactVideoPlayer(
    videoFile: VideoFile,
    modifier: Modifier = Modifier,
    onPlayPause: (Boolean) -> Unit = {},
    isPlaying: Boolean = false
) {
    var currentIsPlaying by remember { mutableStateOf(isPlaying) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact play button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary)
                    .clickable {
                        currentIsPlaying = !currentIsPlaying
                        onPlayPause(currentIsPlaying)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentIsPlaying) Icons.Filled.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (currentIsPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Video information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = videoFile.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${videoFile.getTypeDescription()} • ${videoFile.getFormattedSize()}",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Extension property to create a custom Pause icon using vector graphics
 */
val Icons.Filled.Pause: androidx.compose.ui.graphics.vector.ImageVector
    get() {
        if (_pause != null) {
            return _pause!!
        }
        _pause = androidx.compose.ui.graphics.vector.ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                strokeLineMiter = 4.0f,
                pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero
            ) {
                moveTo(6.0f, 4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(16.0f)
                horizontalLineToRelative(-4.0f)
                close()
                moveTo(14.0f, 4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(16.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }.build()
        return _pause!!
    }

private var _pause: androidx.compose.ui.graphics.vector.ImageVector? = null