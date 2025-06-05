package com.yanbin.motioncut.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanbin.motioncut.platform.createOSInfo

@Composable
fun SystemInfoScreen(onNavigateBack: () -> Unit) {
    val osInfo = remember { createOSInfo() }
    val memoryInfo = remember { osInfo.getMemoryInfo() }
    val cpuInfo = remember { osInfo.getCPUInfo() }
    val gpuInfo = remember { osInfo.getGPUInfo() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Title Bar with Back Button
        TopAppBar(
            title = {
                Text(
                    text = "System Information",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary,
            elevation = 4.dp
        )
        
        // System Information Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // OS Information
            Text(
                text = "Operating System: ${osInfo.getOSDisplayString()}",
                style = MaterialTheme.typography.body1
            )
            
            // Memory Information
            Text(
                text = "Memory:",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "  Total: ${memoryInfo.totalMemory}",
                style = MaterialTheme.typography.body2
            )
            Text(
                text = "  Used: ${memoryInfo.usedMemory} (${String.format("%.1f", memoryInfo.usagePercentage)}%)",
                style = MaterialTheme.typography.body2
            )
            Text(
                text = "  Available: ${memoryInfo.availableMemory}",
                style = MaterialTheme.typography.body2
            )
            
            // CPU Information
            Text(
                text = "CPU:",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "  Name: ${cpuInfo.name}",
                style = MaterialTheme.typography.body2
            )
            Text(
                text = "  Architecture: ${cpuInfo.architecture}",
                style = MaterialTheme.typography.body2
            )
            Text(
                text = "  Cores: ${cpuInfo.cores}, Threads: ${cpuInfo.threads}",
                style = MaterialTheme.typography.body2
            )
            
            // GPU Information
            Text(
                text = "GPU:",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (gpuInfo.isNotEmpty()) {
                gpuInfo.forEachIndexed { index, gpu ->
                    Text(
                        text = "  GPU ${index + 1}: ${gpu.name}",
                        style = MaterialTheme.typography.body2
                    )
                    Text(
                        text = "    Vendor: ${gpu.vendor}",
                        style = MaterialTheme.typography.body2
                    )
                    gpu.memory?.let { memory ->
                        Text(
                            text = "    Memory: $memory",
                            style = MaterialTheme.typography.body2
                        )
                    }
                    gpu.driver?.let { driver ->
                        Text(
                            text = "    Driver: $driver",
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            } else {
                Text(
                    text = "  No GPU information available",
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}