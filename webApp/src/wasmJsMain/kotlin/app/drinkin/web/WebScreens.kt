package app.drinkin.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.*
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.call.body
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import org.jetbrains.skia.Bitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.ExperimentalComposeUiApi

// JS date helpers for wasmJs to get current local date components
private fun getYearFromJs(): Int = js("new Date().getFullYear()")
private fun getMonthFromJs(): Int = js("new Date().getMonth() + 1")
private fun getDayFromJs(): Int = js("new Date().getDate()")

private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

fun isValidEmail(email: String): Boolean = emailRegex.matches(email)

fun isUnderage(dobString: String): Boolean {
    return try {
        val parts = dobString.split("-")
        if (parts.size != 3) return true
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        val currentYear = getYearFromJs()
        val currentMonth = getMonthFromJs()
        val currentDay = getDayFromJs()

        var age = currentYear - year
        if (currentMonth < month || (currentMonth == month && currentDay < day)) {
            age--
        }
        age < 18
    } catch (e: Exception) {
        true
    }
}

private fun setBridgeToken(token: String): Unit =
    js(" { var b = document.getElementById('upload-bridge'); if (!b) { b = document.createElement('div'); b.id = 'upload-bridge'; b.style.display = 'none'; document.body.appendChild(b); } b.setAttribute('data-token', token); } ")

private fun triggerUploadJs(): Unit =
    js("window.triggerImageUpload()")

private fun getBridgeStatus(): String =
    js(" (document.getElementById('upload-bridge') ? document.getElementById('upload-bridge').getAttribute('data-status') || 'idle' : 'idle') ")

private fun getBridgeUrl(): String =
    js(" (document.getElementById('upload-bridge') ? document.getElementById('upload-bridge').getAttribute('data-url') || '' : '') ")

private fun getBridgeError(): String =
    js(" (document.getElementById('upload-bridge') ? document.getElementById('upload-bridge').getAttribute('data-error') || '' : '') ")

@Composable
fun NetworkImage(url: String, modifier: Modifier = Modifier) {
    var imageBitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(url) {
        coroutineScope.launch {
            try {
                val fullUrl = if (url.startsWith("/")) {
                    "http://localhost:8080$url"
                } else {
                    url
                }
                val client = io.ktor.client.HttpClient()
                val response: io.ktor.client.statement.HttpResponse = client.get(fullUrl)
                val bytes: ByteArray = response.body()
                val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                val bitmap = org.jetbrains.skia.Bitmap.makeFromImage(skiaImage)
                imageBitmap = bitmap.asComposeImageBitmap()
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    if (imageBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = imageBitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}

// Drinkin Color Theme (No LinkedIn references)
val DrinkinAccentBlue = Color(0xFF0D5FA8)
val DrinkinLightGray = Color(0xFFF3F2EF)
val DrinkinTextBlack = Color(0xFF191919)
val DrinkinMutedGray = Color(0xFF666666)
val DrinkinBorderColor = Color(0xFFE0E0E0)
val DrinkinCardBackground = Color(0xFFFFFFFF)

@Composable
fun WebLoginScreen(
    apiClient: DrinkinApiClient,
    onNavigateToRegister: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val performLogin = {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (!isValidEmail(trimmedEmail)) {
            errorMsg = "Please enter a valid email address."
        } else if (trimmedPassword.length < 8) {
            errorMsg = "Password must be at least 8 characters long."
        } else {
            isLoading = true
            coroutineScope.launch {
                try {
                    val response = apiClient.login(LoginRequest(email = trimmedEmail, password = trimmedPassword))
                    apiClient.setAuthToken(response.token)
                    onAuthSuccess(response.token)
                } catch (e: ResponseException) {
                    errorMsg = when (e.response.status.value) {
                        401 -> "Invalid credentials. Email or password incorrect."
                        409 -> "Email already registered."
                        else -> "Error: ${e.response.status.value}"
                    }
                } catch (e: Exception) {
                    errorMsg = "Network error. Please try again."
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DrinkinLightGray),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 6.dp,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Drinkin", style = MaterialTheme.typography.h5, color = DrinkinAccentBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(DrinkinAccentBlue, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Text("Welcome to your professional drink network", style = MaterialTheme.typography.body2, color = DrinkinMutedGray)

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMsg = null
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMsg = null
                    },
                    label = { Text("Password (8+ characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
                                performLogin()
                            }
                        }
                    )
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                }

                if (isLoading) {
                    CircularProgressIndicator(color = DrinkinAccentBlue)
                } else {
                    Button(
                        onClick = { performLogin() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DrinkinAccentBlue),
                        shape = RoundedCornerShape(24.dp),
                        enabled = email.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text("Join now", color = DrinkinAccentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WebRegisterScreen(
    apiClient: DrinkinApiClient,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val performRegister = {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedDob = dob.trim()

        if (!isValidEmail(trimmedEmail)) {
            errorMsg = "Please enter a valid email address."
        } else if (trimmedUsername.length !in 3..50) {
            errorMsg = "Username must be between 3 and 50 characters."
        } else if (trimmedPassword.length < 8) {
            errorMsg = "Password must be at least 8 characters long."
        } else {
            val dobRegex = "^\\d{4}-\\d{2}-\\d{2}\$".toRegex()
            if (!dobRegex.matches(trimmedDob)) {
                errorMsg = "Date of Birth must be in YYYY-MM-DD format."
            } else if (isUnderage(trimmedDob)) {
                errorMsg = "Registration failed: Must be at least 18 years old."
            } else {
                isLoading = true
                coroutineScope.launch {
                    try {
                        val response = apiClient.register(
                            RegisterRequest(
                                email = trimmedEmail,
                                username = trimmedUsername,
                                password = trimmedPassword,
                                dateOfBirth = trimmedDob
                            )
                        )
                        apiClient.setAuthToken(response.token)
                        onAuthSuccess(response.token)
                    } catch (e: ResponseException) {
                        errorMsg = when (e.response.status.value) {
                            401 -> "Invalid credentials."
                            409 -> "Email or username already in use."
                            400 -> "Registration failed: Must be at least 18 years old."
                            else -> "Registration failed: ${e.response.status.value}"
                        }
                    } catch (e: Exception) {
                        errorMsg = "Network error. Please try again."
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DrinkinLightGray),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 6.dp,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Drinkin", style = MaterialTheme.typography.h5, color = DrinkinAccentBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(DrinkinAccentBlue, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Text("Join the professional drink network", style = MaterialTheme.typography.body2, color = DrinkinMutedGray)

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMsg = null
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMsg = null
                    },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMsg = null
                    },
                    label = { Text("Password (8+ chars)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = dob,
                    onValueChange = {
                        dob = it
                        errorMsg = null
                    },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (email.isNotBlank() && username.isNotBlank() && password.isNotBlank() && dob.isNotBlank() && !isLoading) {
                                performRegister()
                            }
                        }
                    )
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                }

                if (isLoading) {
                    CircularProgressIndicator(color = DrinkinAccentBlue)
                } else {
                    Button(
                        onClick = { performRegister() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DrinkinAccentBlue),
                        shape = RoundedCornerShape(24.dp),
                        enabled = email.isNotBlank() && username.isNotBlank() && password.isNotBlank() && dob.isNotBlank()
                    ) {
                        Text("Agree & Join", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text("Already on Drinkin? Sign in", color = DrinkinAccentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WebDashboardScreen(
    apiClient: DrinkinApiClient,
    currentTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    savedPosts: MutableList<Post>,
    userAboutText: String,
    onUserAboutChange: (String) -> Unit,
    feedRefreshKey: Int,
    onForceFeedRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var isPaginationLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var likedPostIds by remember { mutableStateOf(emptySet<String>()) }
    var likeCountOffsets by remember { mutableStateOf(emptyMap<String, Int>()) }

    var showStartPostModal by remember { mutableStateOf(false) }

    // Viewing other user profile capability
    var viewingOtherUserId by remember { mutableStateOf<String?>(null) }
    var otherUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isOtherProfileLoading by remember { mutableStateOf(false) }
    var otherUserPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isOtherUserPostsLoading by remember { mutableStateOf(false) }

    // Logged-in user ID
    var myUserId by remember { mutableStateOf<String?>(null) }
    var myProfileState by remember { mutableStateOf<UserProfile?>(null) }

    // Dynamic Connections and Pending requests list
    var pendingConnections by remember { mutableStateOf<List<PendingConnectionRequest>>(emptyList()) }
    var activeConnections by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    // Real Conversations list
    var realConversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var realMessages by remember { mutableStateOf<List<Message>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    fun loadInitialFeed() {
        isLoading = true
        errorMsg = null
        posts = emptyList()
        nextCursor = null
        hasMore = true
        coroutineScope.launch {
            try {
                val myProfile = apiClient.getUserProfile("me")
                myUserId = myProfile.id
                myProfileState = myProfile
                onUserAboutChange(myProfile.bio ?: "")

                val page = apiClient.getFeed(cursor = null)
                posts = page.items
                nextCursor = page.nextCursor
                hasMore = page.nextCursor != null
            } catch (e: Exception) {
                errorMsg = "Failed to load feed. Please check your connection."
            } finally {
                isLoading = false
            }
        }
    }

    fun loadNextPage() {
        if (isPaginationLoading || !hasMore) return
        isPaginationLoading = true
        coroutineScope.launch {
            try {
                val page = apiClient.getFeed(cursor = nextCursor)
                posts = posts + page.items
                nextCursor = page.nextCursor
                hasMore = page.nextCursor != null
            } catch (e: Exception) {
                // Ignore
            } finally {
                isPaginationLoading = false
            }
        }
    }

    fun loadNetworkData() {
        coroutineScope.launch {
            try {
                pendingConnections = apiClient.getPendingRequests().items
                activeConnections = apiClient.getConnections().items
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadConversationsList() {
        coroutineScope.launch {
            try {
                realConversations = apiClient.getConversations().items
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadMessagesForActiveConv(convId: String) {
        coroutineScope.launch {
            try {
                realMessages = apiClient.getMessages(convId).items
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadOtherUserProfileAndPosts(userId: String) {
        isOtherProfileLoading = true
        coroutineScope.launch {
            try {
                val profile = apiClient.getUserProfile(userId)
                otherUserProfile = profile

                isOtherUserPostsLoading = true
                val postsPage = apiClient.getUserPosts(profile.id)
                otherUserPosts = postsPage.items
            } catch (e: Exception) {
                // Ignore
            } finally {
                isOtherProfileLoading = false
                isOtherUserPostsLoading = false
            }
        }
    }

    LaunchedEffect(feedRefreshKey) {
        loadInitialFeed()
    }

    LaunchedEffect(currentTab) {
        if (currentTab == DashboardTab.MY_GROUP) {
            loadNetworkData()
        } else if (currentTab == DashboardTab.CHAT) {
            loadConversationsList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = DrinkinCardBackground,
                elevation = 1.dp,
                modifier = Modifier.border(0.5.dp, DrinkinBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(DrinkinAccentBlue, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable {
                                viewingOtherUserId = null
                                onTabChange(DashboardTab.HOME)
                            }
                    ) {
                        Text("D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // User search auto-complete dropdown inside top bar
                    var searchInput by remember { mutableStateOf("") }
                    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
                    var isSearching by remember { mutableStateOf(false) }

                    LaunchedEffect(searchInput) {
                        if (searchInput.trim().length >= 2) {
                            delay(300)
                            try {
                                isSearching = true
                                searchResults = apiClient.searchUsers(searchInput.trim()).items
                            } catch (e: Exception) {
                                searchResults = emptyList()
                            } finally {
                                isSearching = false
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    }

                    Box {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("Search users...", fontSize = 12.sp) },
                            modifier = Modifier.width(220.dp).height(44.dp),
                            shape = RoundedCornerShape(24.dp)
                        )

                        if (searchResults.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .padding(top = 48.dp),
                                elevation = 8.dp,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    searchResults.take(5).forEach { result ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewingOtherUserId = result.id
                                                    loadOtherUserProfileAndPosts(result.id)
                                                    searchInput = ""
                                                    searchResults = emptyList()
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(result.displayName ?: result.username, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("@${result.username}", color = DrinkinMutedGray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    @Composable
                    fun TabItem(tab: DashboardTab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
                        val isSelected = currentTab == tab && viewingOtherUserId == null
                        val color = if (isSelected) DrinkinAccentBlue else DrinkinMutedGray
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable {
                                    viewingOtherUserId = null
                                    onTabChange(tab)
                                }
                                .padding(horizontal = 16.dp)
                                .fillMaxHeight()
                        ) {
                            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(label, color = color, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }

                    TabItem(DashboardTab.HOME, Icons.Default.Home, "Home")
                    TabItem(DashboardTab.MY_GROUP, Icons.Default.Person, "My Group")
                    TabItem(DashboardTab.CHAT, Icons.Default.Send, "Chat")
                    TabItem(DashboardTab.SAVED, Icons.Default.Star, "Saved")
                    TabItem(DashboardTab.PROFILE, Icons.Default.Person, "Profile")

                    Spacer(modifier = Modifier.width(16.dp))

                    TextButton(onClick = onLogout) {
                        Text("Sign Out", color = DrinkinAccentBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DrinkinLightGray)
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT COLUMN: Profile summary card
                Column(
                    modifier = Modifier.width(250.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(DrinkinAccentBlue.copy(alpha = 0.1f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Avatar", tint = DrinkinAccentBlue, modifier = Modifier.size(36.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(myProfileState?.displayName ?: myProfileState?.username ?: "Your Professional Profile", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = DrinkinTextBlack)
                            Text("@${myProfileState?.username ?: ""}", style = MaterialTheme.typography.caption, color = DrinkinMutedGray, textAlign = TextAlign.Center)

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Connections", color = DrinkinMutedGray, fontSize = 12.sp)
                                Text("${activeConnections.size}", color = DrinkinAccentBlue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            Text("About Me", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold, color = DrinkinTextBlack, modifier = Modifier.align(Alignment.Start))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (userAboutText.isBlank()) "No bio provided." else userAboutText, fontSize = 12.sp, color = DrinkinMutedGray, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.Start))
                        }
                    }
                }

                // MIDDLE COLUMN: Main Content
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewingOtherUserId != null) {
                        // Render User's Profile view (Can be me or other)
                        if (isOtherProfileLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = DrinkinAccentBlue)
                        } else otherUserProfile?.let { profile ->
                            val isMe = profile.id == myUserId

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier.size(72.dp).background(DrinkinAccentBlue.copy(alpha = 0.1f), shape = CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = DrinkinAccentBlue, modifier = Modifier.size(44.dp))
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(profile.displayName ?: profile.username, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                                                    Text("@${profile.username}", color = DrinkinMutedGray, fontSize = 13.sp)
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                IconButton(onClick = { viewingOtherUserId = null }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Close Profile")
                                                }
                                            }

                                            Divider()

                                            Text(profile.bio ?: "No bio provided.", style = MaterialTheme.typography.body2)

                                            Divider()

                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Text("${profile.followerCount} Followers")
                                                Text("${profile.followingCount} Following")
                                            }

                                            Divider()

                                            // Hide connection status actions entirely if viewing our own profile!
                                            if (!isMe) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                    when (profile.connectionStatus) {
                                                        "NONE", null -> {
                                                            Button(
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        try {
                                                                            apiClient.sendConnectionRequest(ConnectionRequest(addresseeId = profile.id))
                                                                            loadOtherUserProfileAndPosts(profile.id)
                                                                        } catch (e: Exception) {}
                                                                    }
                                                                },
                                                                colors = ButtonDefaults.buttonColors(backgroundColor = DrinkinAccentBlue)
                                                            ) {
                                                                Text("Connect", color = Color.White)
                                                            }
                                                        }
                                                        "PENDING_SENT" -> {
                                                            Button(onClick = {}, enabled = false) {
                                                                Text("Request Pending")
                                                            }
                                                        }
                                                        "PENDING_RECEIVED" -> {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                                Button(
                                                                    onClick = {
                                                                        coroutineScope.launch {
                                                                            try {
                                                                                val list = apiClient.getPendingRequests().items
                                                                                val match = list.firstOrNull { it.requester.id == profile.id }
                                                                                if (match != null) {
                                                                                    apiClient.acceptConnection(match.id)
                                                                                    loadOtherUserProfileAndPosts(profile.id)
                                                                                }
                                                                            } catch (e: Exception) {}
                                                                        }
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                                                                ) {
                                                                    Text("Accept", color = Color.White)
                                                                }

                                                                Button(
                                                                    onClick = {
                                                                        coroutineScope.launch {
                                                                            try {
                                                                                val list = apiClient.getPendingRequests().items
                                                                                val match = list.firstOrNull { it.requester.id == profile.id }
                                                                                if (match != null) {
                                                                                    apiClient.rejectConnection(match.id)
                                                                                    loadOtherUserProfileAndPosts(profile.id)
                                                                                }
                                                                            } catch (e: Exception) {}
                                                                        }
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
                                                                ) {
                                                                    Text("Reject", color = Color.White)
                                                                }
                                                            }
                                                        }
                                                        "CONNECTED" -> {
                                                            Button(
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        try {
                                                                            val conv = apiClient.getOrCreateConversation(ConversationRequest(otherUserId = profile.id))
                                                                            selectedConversationId = conv.id
                                                                            viewingOtherUserId = null
                                                                            onTabChange(DashboardTab.CHAT)
                                                                        } catch (e: Exception) {}
                                                                    }
                                                                },
                                                                colors = ButtonDefaults.buttonColors(backgroundColor = DrinkinAccentBlue)
                                                            ) {
                                                                Text("Message", color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        text = "Recent Activity",
                                        style = MaterialTheme.typography.subtitle1,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }

                                if (isOtherUserPostsLoading) {
                                    item {
                                        CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally), color = DrinkinAccentBlue)
                                    }
                                } else if (otherUserPosts.isEmpty()) {
                                    item {
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                                Text("No drink reviews posted yet.", style = MaterialTheme.typography.caption, color = DrinkinMutedGray)
                                            }
                                        }
                                    }
                                } else {
                                    items(otherUserPosts) { post ->
                                        WebPostCard(
                                            post = post,
                                            isLiked = false,
                                            likeCount = post.likeCount,
                                            onAuthorClick = {},
                                            onLikeToggle = { _ -> },
                                            isSaved = false,
                                            onSaveToggle = {}
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        when (currentTab) {
                            DashboardTab.HOME -> {
                                // Home feed view
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(DrinkinLightGray, shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = "Avatar", tint = DrinkinMutedGray, modifier = Modifier.size(20.dp))
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .border(1.dp, DrinkinBorderColor, shape = RoundedCornerShape(24.dp))
                                                    .clickable { showStartPostModal = true }
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text("Start a post about a drink experience...", color = DrinkinMutedGray, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }

                                if (isLoading) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = DrinkinAccentBlue)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(posts) { index, post ->
                                            if (index >= posts.size - 2 && hasMore && !isPaginationLoading) {
                                                loadNextPage()
                                            }

                                            val isLiked = likedPostIds.contains(post.id)
                                            val offset = likeCountOffsets[post.id] ?: 0
                                            val displayedLikeCount = post.likeCount + offset

                                            val isSaved = savedPosts.any { it.id == post.id }

                                            WebPostCard(
                                                post = post,
                                                isLiked = isLiked,
                                                likeCount = displayedLikeCount,
                                                onAuthorClick = {
                                                    viewingOtherUserId = post.author.id
                                                    loadOtherUserProfileAndPosts(post.author.id)
                                                },
                                                onLikeToggle = { reactionType ->
                                                    coroutineScope.launch {
                                                        val isRemove = isLiked && reactionType == "LIKE"
                                                        val newIsLiked = !isRemove
                                                        likedPostIds = if (newIsLiked) likedPostIds + post.id else likedPostIds - post.id
                                                        likeCountOffsets = likeCountOffsets + (post.id to (if (newIsLiked) (if (isLiked) offset else offset + 1) else offset - 1))

                                                        try {
                                                            if (newIsLiked) {
                                                                apiClient.likePost(post.id, reactionType)
                                                            } else {
                                                                apiClient.unlikePost(post.id)
                                                            }
                                                        } catch (e: Exception) {
                                                            likedPostIds = if (newIsLiked) likedPostIds - post.id else likedPostIds + post.id
                                                            likeCountOffsets = likeCountOffsets + (post.id to offset)
                                                        }
                                                    }
                                                },
                                                isSaved = isSaved,
                                                onSaveToggle = {
                                                    if (isSaved) {
                                                        savedPosts.removeAll { it.id == post.id }
                                                    } else {
                                                        savedPosts.add(post)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            DashboardTab.MY_GROUP -> {
                                // Connection Inbox UI
                                Text("My Network Connections", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = DrinkinTextBlack)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Pending Requests (${pendingConnections.size})", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        pendingConnections.forEach { req ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(req.requester.displayName ?: req.requester.username, fontWeight = FontWeight.Bold)
                                                    Text("@${req.requester.username}", color = DrinkinMutedGray, fontSize = 12.sp)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                try {
                                                                    apiClient.acceptConnection(req.id)
                                                                    loadNetworkData()
                                                                } catch (e: Exception) {}
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                                                    ) {
                                                        Text("Accept", color = Color.White)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                try {
                                                                    apiClient.rejectConnection(req.id)
                                                                    loadNetworkData()
                                                                } catch (e: Exception) {}
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
                                                    ) {
                                                        Text("Reject", color = Color.White)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))
                                        Divider()
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text("My Connections (${activeConnections.size})", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        activeConnections.forEach { conn ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewingOtherUserId = conn.id
                                                        loadOtherUserProfileAndPosts(conn.id)
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(36.dp).background(DrinkinAccentBlue.copy(alpha = 0.1f), shape = CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = DrinkinAccentBlue)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(conn.displayName ?: conn.username, fontWeight = FontWeight.Bold)
                                                    Text("@${conn.username}", color = DrinkinMutedGray, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            DashboardTab.CHAT -> {
                                // Real Direct Messages UI
                                Row(
                                    modifier = Modifier.fillMaxSize().border(0.5.dp, DrinkinBorderColor, shape = RoundedCornerShape(8.dp)),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    // Conversations pane
                                    Card(
                                        modifier = Modifier.weight(0.35f).fillMaxHeight(),
                                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                                        elevation = 1.dp
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().background(DrinkinAccentBlue).padding(16.dp)
                                            ) {
                                                Text("Messaging", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }

                                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                items(realConversations) { conv ->
                                                    val isSelected = selectedConversationId == conv.id
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(if (isSelected) DrinkinLightGray else DrinkinCardBackground)
                                                            .clickable {
                                                                selectedConversationId = conv.id
                                                                loadMessagesForActiveConv(conv.id)
                                                            }
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.size(36.dp).background(DrinkinAccentBlue.copy(alpha = 0.1f), shape = CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.Person, contentDescription = null, tint = DrinkinAccentBlue)
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(conv.otherUser.displayName ?: conv.otherUser.username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text(conv.lastMessagePreview ?: "No messages.", color = DrinkinMutedGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                    Divider()
                                                }
                                            }
                                        }
                                    }

                                    // Chat Thread Pane
                                    val activeConv = realConversations.firstOrNull { it.id == selectedConversationId }
                                    Card(
                                        modifier = Modifier.weight(0.65f).fillMaxHeight(),
                                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                        elevation = 1.dp
                                    ) {
                                        if (activeConv != null) {
                                            var chatInput by remember(selectedConversationId) { mutableStateOf("") }

                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, DrinkinBorderColor).padding(16.dp)
                                                ) {
                                                    Text(activeConv.otherUser.displayName ?: activeConv.otherUser.username, fontWeight = FontWeight.Bold)
                                                }

                                                LazyColumn(
                                                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    items(realMessages.reversed()) { msg ->
                                                        val isMe = msg.senderId != activeConv.otherUser.id
                                                        val align = if (isMe) Alignment.End else Alignment.Start
                                                        val bgColor = if (isMe) DrinkinAccentBlue else DrinkinLightGray
                                                        val textCol = if (isMe) Color.White else DrinkinTextBlack

                                                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(bgColor, shape = RoundedCornerShape(12.dp))
                                                                    .padding(12.dp)
                                                            ) {
                                                                Text(msg.text, color = textCol)
                                                            }
                                                        }
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, DrinkinBorderColor).padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = chatInput,
                                                        onValueChange = { chatInput = it },
                                                        placeholder = { Text("Write a message...") },
                                                        modifier = Modifier.weight(1f).height(44.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            if (chatInput.isNotBlank()) {
                                                                val typed = chatInput
                                                                chatInput = ""
                                                                coroutineScope.launch {
                                                                    try {
                                                                        val sent = apiClient.sendMessage(activeConv.id, MessageRequest(text = typed))
                                                                        realMessages = realMessages + sent
                                                                    } catch (e: Exception) {}
                                                                }
                                                            }
                                                        },
                                                        enabled = chatInput.isNotBlank()
                                                    ) {
                                                        Icon(Icons.Default.Send, contentDescription = "Send", tint = DrinkinAccentBlue)
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("Select a conversation to start chatting")
                                            }
                                        }
                                    }
                                }
                            }

                            DashboardTab.SAVED -> {
                                // Bookmarks Tab
                                Text("Saved reviews", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)

                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(savedPosts) { post ->
                                        WebPostCard(
                                            post = post,
                                            isLiked = false,
                                            likeCount = post.likeCount,
                                            onAuthorClick = {
                                                viewingOtherUserId = post.author.id
                                                loadOtherUserProfileAndPosts(post.author.id)
                                            },
                                            onLikeToggle = { _ -> },
                                            isSaved = true,
                                            onSaveToggle = {
                                                savedPosts.removeAll { it.id == post.id }
                                            }
                                        )
                                    }
                                }
                            }

                            DashboardTab.PROFILE -> {
                                // Profile edit section fully integrated with PUT /users/me
                                var editDisplayName by remember { mutableStateOf("") }
                                var editBio by remember { mutableStateOf("") }
                                var editAvatarUrl by remember { mutableStateOf("") }
                                var editPrefs by remember { mutableStateOf("") }
                                var isEditing by remember { mutableStateOf(false) }

                                val updateLocalFormStates = {
                                    coroutineScope.launch {
                                        try {
                                            val profile = apiClient.getUserProfile("me")
                                            editDisplayName = profile.displayName ?: ""
                                            editBio = profile.bio ?: ""
                                            editAvatarUrl = profile.avatarUrl ?: ""
                                            editPrefs = profile.drinkPreferences.joinToString(", ")
                                        } catch (e: Exception) {}
                                    }
                                }

                                LaunchedEffect(Unit) {
                                    updateLocalFormStates()
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(72.dp).background(DrinkinAccentBlue.copy(alpha = 0.1f), shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = DrinkinAccentBlue, modifier = Modifier.size(44.dp))
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(if (editDisplayName.isNotBlank()) editDisplayName else (myProfileState?.username ?: "Professional User"), style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                                                Text("@${myProfileState?.username ?: ""}", color = DrinkinMutedGray, fontSize = 13.sp)
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            OutlinedButton(
                                                onClick = {
                                                    if (isEditing) {
                                                        // Call PUT /users/me
                                                        coroutineScope.launch {
                                                            try {
                                                                val updated = apiClient.updateProfile(
                                                                    UpdateProfileRequest(
                                                                        displayName = editDisplayName.takeIf { it.isNotBlank() },
                                                                        bio = editBio.takeIf { it.isNotBlank() },
                                                                        avatarUrl = editAvatarUrl.takeIf { it.isNotBlank() },
                                                                        drinkPreferences = editPrefs.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                                                    )
                                                                )
                                                                myProfileState = updated
                                                                onUserAboutChange(updated.bio ?: "")
                                                            } catch (e: Exception) {}
                                                        }
                                                    }
                                                    isEditing = !isEditing
                                                },
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(if (isEditing) "Save Profile" else "Edit Info")
                                            }
                                        }

                                        Divider()

                                        if (isEditing) {
                                            OutlinedTextField(
                                                value = editDisplayName,
                                                onValueChange = { editDisplayName = it },
                                                label = { Text("Display Name") },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            OutlinedTextField(
                                                value = editBio,
                                                onValueChange = { editBio = it },
                                                label = { Text("Headline / Bio") },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            OutlinedTextField(
                                                value = editPrefs,
                                                onValueChange = { editPrefs = it },
                                                label = { Text("Drink Preferences (comma-separated)") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Column {
                                                Text("Headline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(editBio, color = DrinkinTextBlack, fontSize = 14.sp)
                                            }

                                            Column {
                                                Text("Drink Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(editPrefs, color = DrinkinTextBlack, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT COLUMN: Recommendations / Saved Posts quick panel
                Column(
                    modifier = Modifier.width(250.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Saved Experience Cards", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold, color = DrinkinTextBlack)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (savedPosts.isEmpty()) {
                                Text("No saved reviews. Bookmark posts to quickly access them here.", fontSize = 12.sp, color = DrinkinMutedGray)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    savedPosts.take(4).forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onTabChange(DashboardTab.SAVED) }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = DrinkinAccentBlue, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                item.text,
                                                fontSize = 12.sp,
                                                color = DrinkinTextBlack,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // START A POST MODAL DIALOG
        if (showStartPostModal) {
            AlertDialog(
                onDismissRequest = { showStartPostModal = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Share Drink Review update", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DrinkinTextBlack)
                    }
                },
                text = {
                    var postText by remember { mutableStateOf("") }
                    var category by remember { mutableStateOf(DrinkCategory.ALCOHOLIC) }
                    var type by remember { mutableStateOf("") }
                    var starsRating by remember { mutableStateOf<Int?>(null) }
                    var notes by remember { mutableStateOf("") }
                    var scenarioTag by remember { mutableStateOf("") }
                    var inlineError by remember { mutableStateOf<String?>(null) }

                    val modalScrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(modalScrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = postText,
                            onValueChange = {
                                postText = it
                                inlineError = null
                            },
                            placeholder = { Text("What drink are you tasting right now? Share details...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            maxLines = 4
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DrinkinMutedGray)
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(
                                selected = category == DrinkCategory.ALCOHOLIC,
                                onClick = { category = DrinkCategory.ALCOHOLIC }
                            )
                            Text("Alcoholic", fontSize = 12.sp, modifier = Modifier.clickable { category = DrinkCategory.ALCOHOLIC })
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(
                                selected = category == DrinkCategory.NON_ALCOHOLIC,
                                onClick = { category = DrinkCategory.NON_ALCOHOLIC }
                            )
                            Text("Non-Alcoholic", fontSize = 12.sp, modifier = Modifier.clickable { category = DrinkCategory.NON_ALCOHOLIC })
                        }

                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            placeholder = { Text("Drink type (e.g. Porter, Espresso) - Optional", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column {
                            Text("Your Rating (Optional)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DrinkinMutedGray)
                            Row {
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = if (starsRating != null && starsRating!! >= i) Icons.Default.Star else Icons.Outlined.Star,
                                        contentDescription = "Star $i",
                                        tint = if (starsRating != null && starsRating!! >= i) Color(0xFFF1C40F) else LocalContentColor.current.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { starsRating = if (starsRating == i) null else i }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Tasting Notes (e.g. Oaky, Sweet) - Optional", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = scenarioTag,
                            onValueChange = { scenarioTag = it },
                            placeholder = { Text("Scenario (e.g. Celebration) - Optional", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        inlineError?.let {
                            Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showStartPostModal = false }) {
                                Text("Cancel", color = DrinkinMutedGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (postText.trim().isEmpty()) {
                                        inlineError = "Post content cannot be empty."
                                        return@Button
                                    }
                                    if (postText.length > 1000) {
                                        inlineError = "Post must be under 1000 characters."
                                        return@Button
                                    }

                                    coroutineScope.launch {
                                        try {
                                            apiClient.createPost(
                                                CreatePostRequest(
                                                    text = postText,
                                                    drinkCategory = category,
                                                    drinkType = type.takeIf { it.isNotBlank() },
                                                    rating = starsRating,
                                                    tastingNotes = notes.takeIf { it.isNotBlank() },
                                                    scenario = scenarioTag.takeIf { it.isNotBlank() }
                                                )
                                            )
                                            showStartPostModal = false
                                            onForceFeedRefresh()
                                        } catch (e: Exception) {
                                            inlineError = "Failed to post. Please try again."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = DrinkinAccentBlue),
                                enabled = postText.isNotBlank()
                            ) {
                                Text("Post update", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                buttons = {}
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WebPostCard(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    onAuthorClick: () -> Unit,
    onLikeToggle: (String) -> Unit,
    isSaved: Boolean,
    onSaveToggle: () -> Unit
) {
    var showReactions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = DrinkinCardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onAuthorClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(DrinkinAccentBlue.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = DrinkinAccentBlue)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.Bold, color = DrinkinTextBlack, fontSize = 14.sp)
                    Text("Beverage Aficionado • @${post.author.username}", color = DrinkinMutedGray, fontSize = 11.sp)
                    Text(post.createdAt.take(10), color = DrinkinMutedGray, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(post.text, color = DrinkinTextBlack, style = MaterialTheme.typography.body2, lineHeight = 20.sp)

            // Display Post Image if present
            if (post.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                NetworkImage(
                    url = post.mediaUrls.first(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(DrinkinAccentBlue.copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(post.drinkCategory.name, style = MaterialTheme.typography.caption, color = DrinkinAccentBlue, fontWeight = FontWeight.SemiBold)
                }

                post.drinkType?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF8B5A2B).copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(it, style = MaterialTheme.typography.caption, color = Color(0xFF8B5A2B), fontWeight = FontWeight.SemiBold)
                    }
                }

                post.rating?.let {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("⭐ $it/5", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomStart) {
                    Row(
                        modifier = Modifier
                            .onPointerEvent(PointerEventType.Enter) { showReactions = true }
                            .onPointerEvent(PointerEventType.Exit) { showReactions = false }
                            .clickable { onLikeToggle("LIKE") }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Like",
                            tint = if (isLiked) DrinkinAccentBlue else DrinkinMutedGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Like ($likeCount)", color = if (isLiked) DrinkinAccentBlue else DrinkinMutedGray, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    if (showReactions) {
                        Card(
                            modifier = Modifier
                                .padding(bottom = 32.dp)
                                .onPointerEvent(PointerEventType.Enter) { showReactions = true }
                                .onPointerEvent(PointerEventType.Exit) { showReactions = false }
                                .border(0.5.dp, DrinkinBorderColor, shape = RoundedCornerShape(24.dp)),
                            elevation = 6.dp,
                            shape = RoundedCornerShape(24.dp),
                            backgroundColor = DrinkinCardBackground
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val reactions = listOf(
                                    "LIKE" to "👍",
                                    "LOVE" to "❤️",
                                    "CHEERS" to "🍻",
                                    "WOW" to "😮",
                                    "SAD" to "😢"
                                )
                                reactions.forEach { (type, emoji) ->
                                    Text(
                                        text = emoji,
                                        fontSize = 18.sp,
                                        modifier = Modifier
                                            .clickable {
                                                onLikeToggle(type)
                                                showReactions = false
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.clickable { onSaveToggle() }.padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Save",
                        tint = if (isSaved) DrinkinAccentBlue else DrinkinMutedGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSaved) "Saved" else "Save", color = if (isSaved) DrinkinAccentBlue else DrinkinMutedGray, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}
