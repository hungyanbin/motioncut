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

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Running on: ${osInfo.getOSDisplayString()}",
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(onClick = {
                text = "Hello, Desktop!"
            }) {
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