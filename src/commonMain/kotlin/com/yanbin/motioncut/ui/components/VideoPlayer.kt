package com.yanbin.motioncut.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanbin.motioncut.domain.VideoFile

/**
 * Expected function for video display - will be implemented platform-specifically
 */
@Composable
expect fun VideoDisplayArea(
    videoFile: VideoFile,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
)