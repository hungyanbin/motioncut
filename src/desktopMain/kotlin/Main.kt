import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yanbin.motioncut.platform.createOSInfo
import com.yanbin.motioncut.platform.setupWindowDragAndDrop
import com.yanbin.motioncut.ui.MainScreen
import com.yanbin.motioncut.domain.VideoFile

@Composable
@Preview
fun App() {
    MaterialTheme {
        MainScreen()
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MotionCut"
    ) {
        // Set up drag and drop functionality for the window
        setupWindowDragAndDrop()
        
        App()
    }
}