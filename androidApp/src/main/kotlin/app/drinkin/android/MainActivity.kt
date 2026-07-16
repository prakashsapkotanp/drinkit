package app.drinkin.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.drinkin.shared.api.DrinkinApiClient

enum class AppScreen { LOGIN, REGISTER, MAIN_FEED }

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
                mutableStateOf(if (initialToken != null) AppScreen.MAIN_FEED else AppScreen.LOGIN)
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                apiClient = apiClient,
                                tokenManager = tokenManager,
                                onNavigateToRegister = { currentScreen = AppScreen.REGISTER },
                                onAuthSuccess = {
                                    currentScreen = AppScreen.MAIN_FEED
                                }
                            )
                        }
                        AppScreen.REGISTER -> {
                            RegisterScreen(
                                apiClient = apiClient,
                                tokenManager = tokenManager,
                                onNavigateToLogin = { currentScreen = AppScreen.LOGIN },
                                onAuthSuccess = {
                                    currentScreen = AppScreen.MAIN_FEED
                                }
                            )
                        }
                        AppScreen.MAIN_FEED -> {
                            MainFeedPlaceholder(
                                onLogout = {
                                    tokenManager.clearToken()
                                    apiClient.setAuthToken(null)
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainFeedPlaceholder(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drinkin' Feed") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colors.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Drinkin' — Android app scaffold",
                style = MaterialTheme.typography.h5
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("You are logged in securely!")
        }
    }
}
