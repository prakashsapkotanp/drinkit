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

enum class AppScreen { LOGIN, REGISTER, FEED, CREATE_POST, PROFILE, OTHER_PROFILE, CONNECTIONS_INBOX, CHAT_LIST, CHAT_THREAD, USER_SEARCH }

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

            var selectedOtherUserId by remember { mutableStateOf<String?>(null) }
            var selectedConversationId by remember { mutableStateOf<String?>(null) }

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
                            key(feedRefreshTrigger) {
                                FeedScreen(
                                    apiClient = apiClient,
                                    onNavigateToCreatePost = { currentScreen = AppScreen.CREATE_POST },
                                    onNavigateToProfile = { currentScreen = AppScreen.PROFILE },
                                    onNavigateToSearch = { currentScreen = AppScreen.USER_SEARCH },
                                    onNavigateToConnections = { currentScreen = AppScreen.CONNECTIONS_INBOX },
                                    onNavigateToChat = { currentScreen = AppScreen.CHAT_LIST },
                                    onNavigateToOtherProfile = { userId ->
                                        selectedOtherUserId = userId
                                        currentScreen = AppScreen.OTHER_PROFILE
                                    },
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
                        AppScreen.PROFILE -> {
                            ProfileScreen(
                                apiClient = apiClient,
                                onBack = { currentScreen = AppScreen.FEED }
                            )
                        }
                        AppScreen.OTHER_PROFILE -> {
                            selectedOtherUserId?.let { userId ->
                                OtherProfileScreen(
                                    apiClient = apiClient,
                                    userId = userId,
                                    onBack = { currentScreen = AppScreen.FEED },
                                    onNavigateToChat = { convId ->
                                        selectedConversationId = convId
                                        currentScreen = AppScreen.CHAT_THREAD
                                    }
                                )
                            }
                        }
                        AppScreen.CONNECTIONS_INBOX -> {
                            ConnectionsInboxScreen(
                                apiClient = apiClient,
                                onBack = { currentScreen = AppScreen.FEED },
                                onNavigateToOtherProfile = { userId ->
                                    selectedOtherUserId = userId
                                    currentScreen = AppScreen.OTHER_PROFILE
                                }
                            )
                        }
                        AppScreen.CHAT_LIST -> {
                            ChatListScreen(
                                apiClient = apiClient,
                                onBack = { currentScreen = AppScreen.FEED },
                                onNavigateToThread = { convId ->
                                    selectedConversationId = convId
                                    currentScreen = AppScreen.CHAT_THREAD
                                }
                            )
                        }
                        AppScreen.CHAT_THREAD -> {
                            selectedConversationId?.let { convId ->
                                ChatThreadScreen(
                                    apiClient = apiClient,
                                    conversationId = convId,
                                    onBack = { currentScreen = AppScreen.CHAT_LIST }
                                )
                            }
                        }
                        AppScreen.USER_SEARCH -> {
                            SimpleUserSearchScreen(
                                apiClient = apiClient,
                                onBack = { currentScreen = AppScreen.FEED },
                                onNavigateToOtherProfile = { userId ->
                                    selectedOtherUserId = userId
                                    currentScreen = AppScreen.OTHER_PROFILE
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
