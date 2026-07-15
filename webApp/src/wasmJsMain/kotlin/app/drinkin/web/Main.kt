package app.drinkin.web

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.CanvasBasedWindow
import app.drinkin.shared.api.DrinkinApiClient

// TODO: point to your deployed backend URL per environment
private val apiClient = DrinkinApiClient(baseUrl = "http://localhost:8080/api")

fun main() {
    CanvasBasedWindow(canvasElementId = "drinkinCanvas") {
        DrinkinWebApp()
    }
}

@Composable
fun DrinkinWebApp() {
    MaterialTheme {
        Surface {
            // TODO: wire up routing -> AuthScreen -> FeedScreen -> CreatePostScreen
            // See tickets DRK-111, DRK-112, DRK-113
            Text("Drinkin' — Web app scaffold")
        }
    }
}
