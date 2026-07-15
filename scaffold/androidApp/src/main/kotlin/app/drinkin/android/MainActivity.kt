package app.drinkin.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.drinkin.shared.api.DrinkinApiClient

class MainActivity : ComponentActivity() {
    // TODO: inject base URL from BuildConfig per environment (local/staging/prod)
    private val apiClient = DrinkinApiClient(baseUrl = "http://10.0.2.2:8080/api")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrinkinApp()
        }
    }
}

@Composable
fun DrinkinApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // TODO: wire up NavHost -> AuthScreen -> FeedScreen -> CreatePostScreen
            // See tickets DRK-108, DRK-109, DRK-110
            Text("Drinkin' — Android app scaffold")
        }
    }
}
