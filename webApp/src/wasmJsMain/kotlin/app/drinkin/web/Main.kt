package app.drinkin.web

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.CanvasBasedWindow
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.Post

// Point to backend URL
private val apiClient = DrinkinApiClient(baseUrl = "http://localhost:8080/api")

enum class WebScreen { LOGIN, REGISTER, DASHBOARD }
enum class DashboardTab { HOME, MY_GROUP, CHAT, SAVED, PROFILE }

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "drinkinCanvas") {
        DrinkinWebApp()
    }
}

@Composable
fun DrinkinWebApp() {
    var token by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf(WebScreen.LOGIN) }
    var currentTab by remember { mutableStateOf(DashboardTab.HOME) }

    // Dynamic states for interactive features
    val savedPosts = remember { mutableStateListOf<Post>() }
    var userAboutText by remember { mutableStateOf("Experienced beverage enthusiast, cocktail reviewer, and barista. Love exploring specialty coffee, craft IPAs, and organic wines.") }

    // Refresh key to force feed list reload when needed
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
                            currentScreen = WebScreen.DASHBOARD
                            currentTab = DashboardTab.HOME
                        }
                    )
                }
                WebScreen.REGISTER -> {
                    WebRegisterScreen(
                        apiClient = apiClient,
                        onNavigateToLogin = { currentScreen = WebScreen.LOGIN },
                        onAuthSuccess = { authToken ->
                            token = authToken
                            currentScreen = WebScreen.DASHBOARD
                            currentTab = DashboardTab.HOME
                        }
                    )
                }
                WebScreen.DASHBOARD -> {
                    WebDashboardScreen(
                        apiClient = apiClient,
                        currentTab = currentTab,
                        onTabChange = { currentTab = it },
                        savedPosts = savedPosts,
                        userAboutText = userAboutText,
                        onUserAboutChange = { userAboutText = it },
                        feedRefreshKey = feedRefreshKey,
                        onForceFeedRefresh = { feedRefreshKey++ },
                        onLogout = {
                            token = null
                            apiClient.setAuthToken(null)
                            currentScreen = WebScreen.LOGIN
                        }
                    )
                }
            }
        }
    }
}
