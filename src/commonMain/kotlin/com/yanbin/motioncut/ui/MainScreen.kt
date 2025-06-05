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
            verticalArrangement = Arrangement.Center
        ) {
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
            
            Text(
                text = "Click the info icon in the title bar to view system information",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}