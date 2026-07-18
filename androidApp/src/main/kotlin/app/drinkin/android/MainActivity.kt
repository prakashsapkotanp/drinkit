package app.drinkin.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.android.theme.DrinkinTheme

enum class AppScreen { LOGIN, REGISTER, FEED, CREATE_POST }

class MainActivity : ComponentActivity() {
    private val apiClient = DrinkinApiClient(baseUrl = "http://10.0.2.2:8080/api")
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)

        val initialToken = tokenManager.getToken()
        if (initialToken != null) {
            apiClient.setAuthToken(initialToken)
        }

        setContent {
            var currentScreen by remember {
                mutableStateOf(if (initialToken != null) AppScreen.FEED else AppScreen.LOGIN)
            }

            // Simple key to force refresh feed when navigating back
            var feedRefreshTrigger by remember { mutableStateOf(0) }

            DrinkinTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                apiClient = apiClient,
                                tokenManager = tokenManager,
                                onNavigateToRegister = { currentScreen = AppScreen.REGISTER },
                                onAuthSuccess = {
                                    currentScreen = AppScreen.FEED
                                }
                            )
                        }
                        AppScreen.REGISTER -> {
                            RegisterScreen(
                                apiClient = apiClient,
                                tokenManager = tokenManager,
                                onNavigateToLogin = { currentScreen = AppScreen.LOGIN },
                                onAuthSuccess = {
                                    currentScreen = AppScreen.FEED
                                }
                            )
                        }
                        AppScreen.FEED -> {
                            // Key parameter forces compose to recreate FeedScreen, refreshing feed state automatically
                            key(feedRefreshTrigger) {
                                FeedScreen(
                                    apiClient = apiClient,
                                    onNavigateToCreatePost = { currentScreen = AppScreen.CREATE_POST },
                                    onLogout = {
                                        tokenManager.clearToken()
                                        apiClient.setAuthToken(null)
                                        currentScreen = AppScreen.LOGIN
                                    }
                                )
                            }
                        }
                        AppScreen.CREATE_POST -> {
                            CreatePostScreen(
                                apiClient = apiClient,
                                onBackToFeed = { currentScreen = AppScreen.FEED },
                                onPostCreated = {
                                    feedRefreshTrigger++
                                    currentScreen = AppScreen.FEED
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
