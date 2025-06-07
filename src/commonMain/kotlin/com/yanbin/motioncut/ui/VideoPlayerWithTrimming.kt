package com.yanbin.motioncut.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanbin.motioncut.domain.VideoFile

/**
 * Platform-specific video player with trimming functionality
 */
@Composable
expect fun VideoPlayerWithTrimming(
    videoFile: VideoFile,
    modifier: Modifier = Modifier
)