package app.drinkin.web

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.CanvasBasedWindow
import app.drinkin.shared.api.DrinkinApiClient

// TODO: point to your deployed backend URL per environment
private val apiClient = DrinkinApiClient(baseUrl = "http://localhost:8080/api")

enum class WebScreen { LOGIN, REGISTER, FEED, CREATE_POST }

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "drinkinCanvas") {
        DrinkinWebApp()
    }
}

@Composable
fun DrinkinWebApp() {
    // SECURITY STRATEGY FOR WEB JWT PERSISTENCE (DRK-111):
    // We explicitly store the authentication token strictly in-memory using Compose mutableState.
    // TRADEOFF: Storing tokens in localStorage or sessionStorage exposes the app to severe XSS attacks,
    // where malicious scripts could silently extract the session token. Keeping it strictly in-memory
    // completely mitigates this vulnerability, though it requires the user to re-authenticate (login again)
    // upon browser page refreshes. This is the recommended secure default for Phase 1.
    var token by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf(WebScreen.LOGIN) }

    // Refresh key to force feed list reload upon returning from Create Post screen
    var feedRefreshKey by remember { mutableStateOf(0) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                WebScreen.LOGIN -> {
                    WebLoginScreen(
                        apiClient = apiClient,
                        onNavigateToRegister = { currentScreen = WebScreen.REGISTER },
                        onAuthSuccess = { authToken ->
                            token = authToken
                            currentScreen = WebScreen.FEED
                        }
                    )
                }
                WebScreen.REGISTER -> {
                    WebRegisterScreen(
                        apiClient = apiClient,
                        onNavigateToLogin = { currentScreen = WebScreen.LOGIN },
                        onAuthSuccess = { authToken ->
                            token = authToken
                            currentScreen = WebScreen.FEED
                        }
                    )
                }
                WebScreen.FEED -> {
                    key(feedRefreshKey) {
                        WebFeedScreen(
                            apiClient = apiClient,
                            onNavigateToCreatePost = { currentScreen = WebScreen.CREATE_POST },
                            onLogout = {
                                token = null
                                apiClient.setAuthToken(null)
                                currentScreen = WebScreen.LOGIN
                            }
                        )
                    }
                }
                WebScreen.CREATE_POST -> {
                    WebCreatePostScreen(
                        apiClient = apiClient,
                        onBackToFeed = { currentScreen = WebScreen.FEED },
                        onPostCreated = {
                            feedRefreshKey++
                            currentScreen = WebScreen.FEED
                        }
                    )
                }
            }
        }
    }
}
