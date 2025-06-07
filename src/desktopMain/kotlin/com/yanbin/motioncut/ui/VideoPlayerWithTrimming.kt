package com.yanbin.motioncut.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanbin.motioncut.domain.VideoFile
import com.yanbin.motioncut.services.VideoTrimmerService
import com.yanbin.motioncut.ui.components.VideoPlayer
import kotlinx.coroutines.launch
import java.io.File

/**
 * Desktop-specific video trimming functionality
 */
@Composable
actual fun VideoPlayerWithTrimming(
    videoFile: VideoFile,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()
    val trimmerService = remember { VideoTrimmerService() }
    
    var isTrimmingInProgress by remember { mutableStateOf(false) }
    var trimmingProgress by remember { mutableStateOf(0f) }
    var trimmingMessage by remember { mutableStateOf("") }
    var showTrimDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var completionMessage by remember { mutableStateOf("") }
    
    Column(modifier = modifier) {
        // Video Player
        Box(modifier = Modifier.fillMaxWidth()) {
            VideoPlayer(
                videoFile = videoFile,
                modifier = Modifier.fillMaxWidth(),
                onPlayPause = { isPlaying ->
                    println("Video ${if (isPlaying) "playing" else "paused"}")
                }
            )
            
            // Trim Button Overlay
            Button(
                onClick = {
                    if (!isTrimmingInProgress) {
                        showTrimDialog = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red,
                    contentColor = Color.White
                ),
                enabled = !isTrimmingInProgress
            ) {
                Text("Trim to Half")
            }
        }
        
        // Trimming Progress Indicator
        if (isTrimmingInProgress) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Trimming Video...",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = trimmingProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colors.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${(trimmingProgress * 100).toInt()}% Complete",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    
                    if (trimmingMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trimmingMessage,
                            style = MaterialTheme.typography.body2,
                            color = if (trimmingMessage.contains("Error")) Color.Red else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    
    // Trim Confirmation Dialog
    if (showTrimDialog) {
        AlertDialog(
            onDismissRequest = { showTrimDialog = false },
            title = {
                Text("Trim Video to Half")
            },
            text = {
                Text("This will create a new video file that is half the length of the original. The original file will not be modified.\n\nDo you want to continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTrimDialog = false
                        scope.launch {
                            isTrimmingInProgress = true
                            trimmingProgress = 0f
                            trimmingMessage = "Initializing..."
                            
                            val inputPath = videoFile.path
                            val outputPath = generateOutputPath(inputPath)
                            
                            trimmerService.trimVideoToHalf(
                                inputPath = inputPath,
                                outputPath = outputPath,
                                onProgress = { progress ->
                                    trimmingProgress = progress
                                    trimmingMessage = "Processing frames..."
                                },
                                onComplete = {
                                    isTrimmingInProgress = false
                                    trimmingMessage = ""
                                    completionMessage = "Video trimmed successfully!\n\nSaved to:\n$outputPath"
                                    showCompletionDialog = true
                                },
                                onError = { error ->
                                    isTrimmingInProgress = false
                                    trimmingMessage = "Error: $error"
                                }
                            )
                        }
                    }
                ) {
                    Text("Trim Video")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTrimDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Completion Success Dialog
    if (showCompletionDialog) {
        TrimmingCompleteDialog(onDismiss = { showCompletionDialog = false}, completionMessage)
    }
}

@Composable
private fun TrimmingCompleteDialog(onDismiss: () -> Unit, completionMessage: String) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text("✅ Trimming Complete!")
        },
        text = {
            Text(
                text = completionMessage,
                style = MaterialTheme.typography.body1
            )
        },
        confirmButton = {
            Button(
                onClick = { onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Text("OK")
            }
        }
    )
}

/**
 * Generate output path for trimmed video
 */
private fun generateOutputPath(inputPath: String): String {
    val file = File(inputPath)
    val nameWithoutExtension = file.nameWithoutExtension
    val extension = file.extension.ifEmpty { "mp4" } // Default to mp4 if no extension
    val parentDir = file.parent ?: file.absoluteFile.parent ?: System.getProperty("user.home")
    
    // Ensure we have a valid filename
    val safeName = if (nameWithoutExtension.isBlank()) "trimmed_video" else nameWithoutExtension
    
    return "$parentDir${File.separator}${safeName}_trimmed_half.$extension"
}