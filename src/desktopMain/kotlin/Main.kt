import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yanbin.motioncut.platform.createOSInfo

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }
    val osInfo = remember { createOSInfo() }
    val memoryInfo = remember { osInfo.getMemoryInfo() }
    val cpuInfo = remember { osInfo.getCPUInfo() }
    val gpuInfo = remember { osInfo.getGPUInfo() }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "MotionCut - System Information",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
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
            
            Button(
                onClick = { text = "Hello, Desktop!" },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(text)
            }
        }
    }
}

fun main() = application {
    val osInfo = createOSInfo()
    val windowTitle = "MotionCut - ${osInfo.getOSDisplayString()}"
    
    Window(
        onCloseRequest = ::exitApplication,
        title = windowTitle
    ) {
        App()
    }
}