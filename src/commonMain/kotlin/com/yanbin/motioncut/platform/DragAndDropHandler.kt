package com.yanbin.motioncut.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanbin.motioncut.domain.VideoFile

/**
 * Common interface for drag and drop functionality across platforms
 */
@Composable
expect fun DragAndDropContainer(
    onFileDropped: (VideoFile) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
)