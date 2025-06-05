import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yanbin.motioncut.platform.createOSInfo
import com.yanbin.motioncut.ui.MainScreen

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
        App()
    }
}