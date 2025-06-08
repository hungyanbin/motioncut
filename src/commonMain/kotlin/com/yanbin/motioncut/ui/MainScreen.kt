package com.yanbin.motioncut.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanbin.motioncut.domain.VideoFile
import com.yanbin.motioncut.ui.components.FileDropZone
import com.yanbin.motioncut.ui.components.FileInfoDisplay
import com.yanbin.motioncut.platform.DragAndDropContainer
import com.yanbin.motioncut.ui.video.VideoPlayerWithTrimming

enum class Screen {
    HOME,
    SYSTEM_INFO
}

@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    
    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            onNavigateToSystemInfo = { currentScreen = Screen.SYSTEM_INFO }
        )
        Screen.SYSTEM_INFO -> SystemInfoScreen(
            onNavigateBack = { currentScreen = Screen.HOME }
        )
    }
}

@Composable
fun HomeScreen(onNavigateToSystemInfo: () -> Unit) {
    var selectedVideoFile by remember { mutableStateOf<VideoFile?>(null) }
    var isDragOver by remember { mutableStateOf(false) }
    
    DragAndDropContainer(
        onFileDropped = { videoFile ->
            selectedVideoFile = videoFile
        },
        onDragStateChanged = { dragState ->
            isDragOver = dragState
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Title Bar with Navigation Button
        TopAppBar(
            title = {
                Text(
                    text = "MotionCut",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onNavigateToSystemInfo) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "System Information",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary,
            elevation = 4.dp
        )
        
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (selectedVideoFile == null) Arrangement.Center else Arrangement.Top
        ) {
            if (selectedVideoFile == null) {
                // Welcome message and drag & drop zone
                Text(
                    text = "Welcome to MotionCut",
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Video Trimming Made Simple",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 48.dp)
                )
                
                FileDropZone(
                    onFileDropped = { videoFile ->
                        selectedVideoFile = videoFile
                    },
                    isDragOver = isDragOver,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Text(
                    text = "Click the info icon in the title bar to view system information",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            } else {
                // File information display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Video File",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { selectedVideoFile = null },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary
                        )
                    ) {
                        Text("Select Another File")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FileInfoDisplay(
                    videoFile = selectedVideoFile!!,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Video Player Component with Trimming (Desktop-specific)
                VideoPlayerWithTrimming(
                    videoFile = selectedVideoFile!!,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
        }
    }
}