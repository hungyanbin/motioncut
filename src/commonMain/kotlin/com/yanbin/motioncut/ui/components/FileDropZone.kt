package com.yanbin.motioncut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanbin.motioncut.domain.VideoFile
import java.io.File

@Composable
fun FileDropZone(
    onFileDropped: (VideoFile) -> Unit,
    modifier: Modifier = Modifier,
    isDragOver: Boolean = false
) {
    val backgroundColor = if (isDragOver) {
        MaterialTheme.colors.primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colors.surface
    }
    
    val borderColor = if (isDragOver) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add File",
                modifier = Modifier.size(48.dp),
                tint = if (isDragOver) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isDragOver) {
                    "Drop your video file here"
                } else {
                    "Drag & drop a video file here"
                },
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = if (isDragOver) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Supported formats: MP4, AVI, MOV, MKV, WMV, FLV, WebM, M4V",
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun FileInfoDisplay(
    videoFile: VideoFile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "File Information",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            FileInfoRow(label = "Name", value = videoFile.name)
            FileInfoRow(label = "Type", value = videoFile.getTypeDescription())
            FileInfoRow(label = "Size", value = videoFile.getFormattedSize())
            FileInfoRow(label = "Extension", value = videoFile.extension.uppercase())
            
            if (!videoFile.isSupportedVideoFormat()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ This file format may not be fully supported",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FileInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface
        )
    }
}

/**
 * Helper function to create VideoFile from a dropped file
 */
fun createVideoFileFromPath(filePath: String): VideoFile? {
    return try {
        val file = File(filePath)
        if (!file.exists()) return null
        
        val name = file.name
        val size = file.length()
        val extension = file.extension
        val type = when (extension.lowercase()) {
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "m4v" -> "video/x-m4v"
            else -> "video/*"
        }
        
        VideoFile(
            name = name,
            path = filePath,
            size = size,
            type = type,
            extension = extension
        )
    } catch (e: Exception) {
        null
    }
}