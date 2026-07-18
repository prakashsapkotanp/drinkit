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
import app.drinkin.shared.model.CreatePostRequest
import app.drinkin.shared.model.DrinkCategory
import app.drinkin.shared.model.LoginRequest
import app.drinkin.shared.model.Post
import app.drinkin.shared.model.RegisterRequest
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

// LinkedIn Color Theme
val LinkedInBlue = Color(0xFF0A66C2)
val LinkedInLightGray = Color(0xFFF3F2EF)
val LinkedInTextBlack = Color(0xFF191919)
val LinkedInMutedGray = Color(0xFF666666)
val LinkedInBorderColor = Color(0xFFE0E0E0)
val LinkedInCardBackground = Color(0xFFFFFFFF)

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
            .background(LinkedInLightGray),
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
                // Logo row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Drinkin", style = MaterialTheme.typography.h5, color = LinkedInBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(LinkedInBlue, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Text("Welcome to your professional drink network", style = MaterialTheme.typography.body2, color = LinkedInMutedGray)

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
                    CircularProgressIndicator(color = LinkedInBlue)
                } else {
                    Button(
                        onClick = { performLogin() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = LinkedInBlue),
                        shape = RoundedCornerShape(24.dp),
                        enabled = email.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text("Join now", color = LinkedInBlue, fontWeight = FontWeight.SemiBold)
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
            .background(LinkedInLightGray),
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
                    Text("Drinkin", style = MaterialTheme.typography.h5, color = LinkedInBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(LinkedInBlue, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Text("Join the professional drink network", style = MaterialTheme.typography.body2, color = LinkedInMutedGray)

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
                    CircularProgressIndicator(color = LinkedInBlue)
                } else {
                    Button(
                        onClick = { performRegister() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = LinkedInBlue),
                        shape = RoundedCornerShape(24.dp),
                        enabled = email.isNotBlank() && username.isNotBlank() && password.isNotBlank() && dob.isNotBlank()
                    ) {
                        Text("Agree & Join", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text("Already on DrinkinIn? Sign in", color = LinkedInBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Simulated Chat State Item
data class ChatMessage(val senderName: String, val messageText: String, val isMe: Boolean, val timestamp: String)
data class Contact(val name: String, val headline: String, val unreadCount: Int, val messages: List<ChatMessage>)

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

    // Start Post Modal State
    var showStartPostModal by remember { mutableStateOf(false) }

    // Simulated Chat Messages State
    val contacts = remember {
        mutableStateListOf(
            Contact(
                name = "Sarah Brewer (Sommelier)",
                headline = "Lover of IPAs & Stouts • Brewer at CraftHouse",
                unreadCount = 1,
                messages = listOf(
                    ChatMessage("Sarah Brewer (Sommelier)", "Hey there! Did you get a chance to try that new Stout?", false, "09:30 AM"),
                    ChatMessage("Me", "Yes! It has a wonderful coffee aroma with notes of dark chocolate.", true, "09:32 AM"),
                    ChatMessage("Sarah Brewer (Sommelier)", "Totally agree! It is one of their finest works this season.", false, "09:35 AM")
                )
            ),
            Contact(
                name = "John Coffee Roasters",
                headline = "Specialty Coffee Trader • Sourcing fine Arabica",
                unreadCount = 0,
                messages = listOf(
                    ChatMessage("John Coffee Roasters", "Welcome to DrinkinIn! Best place to log coffee tasting notes.", false, "Yesterday")
                )
            ),
            Contact(
                name = "Alice Wine Cellar",
                headline = "Vintage Collector • Burgundy Wine Advisor",
                unreadCount = 0,
                messages = listOf(
                    ChatMessage("Alice Wine Cellar", "Hey, your tasting review of the Pinot Noir was spot on!", false, "2 days ago")
                )
            )
        )
    }

    var selectedContactIndex by remember { mutableStateOf(0) }

    // Simulated network users for follow suggestions
    val suggestions = remember {
        mutableStateListOf(
            "Michael Jameson" to "Whiskey Distiller & Cask Expert",
            "Elena Rostova" to "Barista Champion • Espresso Aficionado",
            "David Sterling" to "Sommelier & Organic Vineyards Consultant",
            "Claire Dupont" to "Craft Cider Brewer • Fruit Fermentation Specialist"
        )
    }

    val coroutineScope = rememberCoroutineScope()

    fun loadInitialFeed() {
        isLoading = true
        errorMsg = null
        posts = emptyList()
        nextCursor = null
        hasMore = true
        coroutineScope.launch {
            try {
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
                // Fail silently
            } finally {
                isPaginationLoading = false
            }
        }
    }

    LaunchedEffect(feedRefreshKey) {
        loadInitialFeed()
    }

    Scaffold(
        topBar = {
            // LinkedIn Style top navigation
            TopAppBar(
                backgroundColor = LinkedInCardBackground,
                elevation = 1.dp,
                modifier = Modifier.border(0.5.dp, LinkedInBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo box
                    Box(
                        modifier = Modifier
                            .background(LinkedInBlue, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { onTabChange(DashboardTab.HOME) }
                    ) {
                        Text("D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Simulated Search bar
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .height(36.dp)
                            .background(LinkedInLightGray, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = LinkedInMutedGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search drinks, notes...", color = LinkedInMutedGray, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Tab Item helpers
                    @Composable
                    fun TabItem(tab: DashboardTab, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
                        val isSelected = currentTab == tab
                        val color = if (isSelected) LinkedInBlue else LinkedInMutedGray
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable { onTabChange(tab) }
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
                        Text("Sign Out", color = LinkedInBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinkedInLightGray)
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
                // LEFT COLUMN: Profile summary card (about 250dp)
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
                            // Avatar container
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(LinkedInBlue.copy(alpha = 0.1f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Avatar", tint = LinkedInBlue, modifier = Modifier.size(36.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Your Professional Profile", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = LinkedInTextBlack)
                            Text("Beverage Enthusiast", style = MaterialTheme.typography.caption, color = LinkedInMutedGray, textAlign = TextAlign.Center)

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Profile viewers", color = LinkedInMutedGray, fontSize = 12.sp)
                                Text("48", color = LinkedInBlue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Connections", color = LinkedInMutedGray, fontSize = 12.sp)
                                Text("135", color = LinkedInBlue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            Text("About Me", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold, color = LinkedInTextBlack, modifier = Modifier.align(Alignment.Start))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(userAboutText, fontSize = 12.sp, color = LinkedInMutedGray, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.Start))
                        }
                    }

                    // Network info / Group info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Your Groups", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🍹", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wine Tasting Network", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("☕", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Specialty Coffee Pros", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // MIDDLE COLUMN: Main Content depending on currentTab
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentTab) {
                        DashboardTab.HOME -> {
                            // LinkedIn Style "Start a Post" box
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
                                                .background(LinkedInLightGray, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = "Avatar", tint = LinkedInMutedGray, modifier = Modifier.size(20.dp))
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Clickable input box
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .border(1.dp, LinkedInBorderColor, shape = RoundedCornerShape(24.dp))
                                                .clickable { showStartPostModal = true }
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text("Start a post about a drink experience...", color = LinkedInMutedGray, fontSize = 14.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Action buttons in Post Box
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.clickable { showStartPostModal = true },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🍹", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Alcoholic", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LinkedInMutedGray)
                                        }

                                        Row(
                                            modifier = Modifier.clickable { showStartPostModal = true },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("☕", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Non-Alcoholic", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LinkedInMutedGray)
                                        }

                                        Row(
                                            modifier = Modifier.clickable { showStartPostModal = true },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF1C40F), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Rating", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LinkedInMutedGray)
                                        }
                                    }
                                }
                            }

                            // Feed posts
                            if (isLoading) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = LinkedInBlue)
                                }
                            } else if (errorMsg != null) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(errorMsg!!, color = MaterialTheme.colors.error)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(onClick = { loadInitialFeed() }) { Text("Retry") }
                                    }
                                }
                            } else if (posts.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No drink posts in your network. Start a post to share yours!", textAlign = TextAlign.Center)
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

                                        WebLinkedInPostCard(
                                            post = post,
                                            isLiked = isLiked,
                                            likeCount = displayedLikeCount,
                                            onLikeToggle = {
                                                coroutineScope.launch {
                                                    val newIsLiked = !isLiked
                                                    likedPostIds = if (newIsLiked) likedPostIds + post.id else likedPostIds - post.id
                                                    likeCountOffsets = likeCountOffsets + (post.id to (if (newIsLiked) offset + 1 else offset - 1))

                                                    try {
                                                        if (newIsLiked) {
                                                            apiClient.likePost(post.id)
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

                                    if (isPaginationLoading) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LinkedInBlue)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DashboardTab.MY_GROUP -> {
                            // Network Tab
                            Text("My Professional Drink Network", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = LinkedInTextBlack)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                elevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Connections & Groups", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    suggestions.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(40.dp).background(LinkedInBlue.copy(alpha = 0.1f), shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = LinkedInBlue)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.first, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(item.second, color = LinkedInMutedGray, fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    // Follow simulation
                                                    onForceFeedRefresh()
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LinkedInBlue),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        if (index < suggestions.size - 1) {
                                            Divider(color = LinkedInBorderColor, thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }

                        DashboardTab.CHAT -> {
                            // Dual-Pane Interactive Chat Tab
                            Row(
                                modifier = Modifier.fillMaxSize().border(0.5.dp, LinkedInBorderColor, shape = RoundedCornerShape(8.dp)),
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                // Contacts Pane (35% width)
                                Card(
                                    modifier = Modifier.weight(0.35f).fillMaxHeight(),
                                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                                    elevation = 1.dp,
                                    backgroundColor = LinkedInCardBackground
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().background(LinkedInBlue).padding(16.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text("Messaging", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }

                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            itemsIndexed(contacts) { index, contact ->
                                                val isSelected = selectedContactIndex == index
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isSelected) LinkedInLightGray else LinkedInCardBackground)
                                                        .clickable { selectedContactIndex = index }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(36.dp).background(LinkedInBlue.copy(alpha = 0.1f), shape = CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = LinkedInBlue)
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text(contact.headline, color = LinkedInMutedGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                                Divider(color = LinkedInBorderColor, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }

                                // Messages Pane (65% width)
                                val activeContact = contacts.getOrNull(selectedContactIndex)
                                Card(
                                    modifier = Modifier.weight(0.65f).fillMaxHeight(),
                                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                    elevation = 1.dp,
                                    backgroundColor = LinkedInCardBackground
                                ) {
                                    if (activeContact != null) {
                                        var chatInput by remember(selectedContactIndex) { mutableStateOf("") }
                                        val chatMessages = remember(selectedContactIndex) { mutableStateListOf<ChatMessage>().apply { addAll(activeContact.messages) } }

                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Top active contact bar
                                            Row(
                                                modifier = Modifier.fillMaxWidth().border(0.5.dp, LinkedInBorderColor).padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(activeContact.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text(activeContact.headline, color = LinkedInMutedGray, fontSize = 11.sp)
                                                }
                                            }

                                            // Messages List
                                            LazyColumn(
                                                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(chatMessages) { msg ->
                                                    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
                                                    val bgColor = if (msg.isMe) LinkedInBlue else LinkedInLightGray
                                                    val textColor = if (msg.isMe) Color.White else LinkedInTextBlack
                                                    val shape = if (msg.isMe) RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp) else RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)

                                                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(bgColor, shape = shape)
                                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                        ) {
                                                            Text(msg.messageText, color = textColor, fontSize = 13.sp)
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(msg.timestamp, color = LinkedInMutedGray, fontSize = 9.sp)
                                                    }
                                                }
                                            }

                                            // Input message row
                                            Row(
                                                modifier = Modifier.fillMaxWidth().border(0.5.dp, LinkedInBorderColor).padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = chatInput,
                                                    onValueChange = { chatInput = it },
                                                    placeholder = { Text("Write a message...", fontSize = 13.sp) },
                                                    modifier = Modifier.weight(1f).height(44.dp),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        if (chatInput.isNotBlank()) {
                                                            val typedText = chatInput
                                                            chatMessages.add(ChatMessage("Me", typedText, true, "Just Now"))
                                                            chatInput = ""

                                                            // Simulated conversational reply
                                                            coroutineScope.launch {
                                                                delay(1500)
                                                                val botReply = when {
                                                                    typedText.contains("wine", ignoreCase = true) -> "I highly recommend trying a 2018 Bourgogne Pinot Noir! It pairs incredibly with duck."
                                                                    typedText.contains("beer", ignoreCase = true) || typedText.contains("stout", ignoreCase = true) -> "A double dry-hopped IPA from New England or a robust imperial stout is definitely worth seeking out!"
                                                                    else -> "That is a great perspective! I always log my drink experiences on DrinkinIn to track profiles."
                                                                }
                                                                chatMessages.add(ChatMessage(activeContact.name, botReply, false, "Just Now"))
                                                            }
                                                        }
                                                    },
                                                    enabled = chatInput.isNotBlank()
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = LinkedInBlue)
                                                }
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Select a contact to start conversation")
                                        }
                                    }
                                }
                            }
                        }

                        DashboardTab.SAVED -> {
                            // Saved Posts Tab
                            Text("Your Bookmarked Drink Experiences", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = LinkedInTextBlack)

                            if (savedPosts.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("You haven't saved any drink reviews yet. Click 'Save' on any update card to keep it here!", textAlign = TextAlign.Center)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(savedPosts) { post ->
                                        val isLiked = likedPostIds.contains(post.id)
                                        val offset = likeCountOffsets[post.id] ?: 0
                                        val displayedLikeCount = post.likeCount + offset

                                        WebLinkedInPostCard(
                                            post = post,
                                            isLiked = isLiked,
                                            likeCount = displayedLikeCount,
                                            onLikeToggle = {
                                                coroutineScope.launch {
                                                    val newIsLiked = !isLiked
                                                    likedPostIds = if (newIsLiked) likedPostIds + post.id else likedPostIds - post.id
                                                    likeCountOffsets = likeCountOffsets + (post.id to (if (newIsLiked) offset + 1 else offset - 1))
                                                }
                                            },
                                            isSaved = true,
                                            onSaveToggle = {
                                                savedPosts.removeAll { it.id == post.id }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        DashboardTab.PROFILE -> {
                            // Full Profile Tab with edit capabilities
                            var bioText by remember { mutableStateOf("Beverage enthusiast specializing in craft IPAs and micro-lot single-origin coffee.") }
                            var drinkPref by remember { mutableStateOf("IPA, Espresso, Cabernet") }
                            var isEditingProfile by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                elevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(72.dp).background(LinkedInBlue.copy(alpha = 0.1f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = LinkedInBlue, modifier = Modifier.size(44.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Your Professional Drink Identity", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                                            Text("@professional_reviewer", color = LinkedInMutedGray, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        OutlinedButton(
                                            onClick = { isEditingProfile = !isEditingProfile },
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text(if (isEditingProfile) "Save Profile" else "Edit Info")
                                        }
                                    }

                                    Divider()

                                    if (isEditingProfile) {
                                        OutlinedTextField(
                                            value = bioText,
                                            onValueChange = { bioText = it },
                                            label = { Text("Headline / Bio") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = drinkPref,
                                            onValueChange = { drinkPref = it },
                                            label = { Text("Favorite Drink Classes") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Column {
                                            Text("Headline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(bioText, color = LinkedInTextBlack, fontSize = 14.sp)
                                        }

                                        Column {
                                            Text("Drink Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(drinkPref, color = LinkedInTextBlack, fontSize = 14.sp)
                                        }
                                    }

                                    Divider()

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Professional About Me Section", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LinkedInTextBlack)
                                        OutlinedTextField(
                                            value = userAboutText,
                                            onValueChange = onUserAboutChange,
                                            modifier = Modifier.fillMaxWidth().height(120.dp),
                                            maxLines = 6
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT COLUMN: Recommendations / Saved Posts quick panel (about 250dp)
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
                            Text("Saved Experience Cards", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold, color = LinkedInTextBlack)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (savedPosts.isEmpty()) {
                                Text("No saved reviews. Bookmark posts to quickly access them here.", fontSize = 12.sp, color = LinkedInMutedGray)
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
                                            Icon(Icons.Default.Star, contentDescription = null, tint = LinkedInBlue, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                item.text,
                                                fontSize = 12.sp,
                                                color = LinkedInTextBlack,
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

                    // Network Connections Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Drinkers in Your Group", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold, color = LinkedInTextBlack)
                            Spacer(modifier = Modifier.height(12.dp))

                            suggestions.take(3).forEach { user ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(28.dp).background(LinkedInBlue.copy(alpha = 0.05f), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = LinkedInBlue, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(user.first, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(user.second, color = LinkedInMutedGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        Text("Share Drink Review update", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LinkedInTextBlack)
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
                            Text("Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LinkedInMutedGray)
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
                            Text("Your Rating (Optional)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LinkedInMutedGray)
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
                                Text("Cancel", color = LinkedInMutedGray)
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
                                colors = ButtonDefaults.buttonColors(backgroundColor = LinkedInBlue),
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

@Composable
fun WebLinkedInPostCard(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    onLikeToggle: () -> Unit,
    isSaved: Boolean,
    onSaveToggle: () -> Unit
) {
    var showComments by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = LinkedInCardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(LinkedInBlue.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = LinkedInBlue)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.Bold, color = LinkedInTextBlack, fontSize = 14.sp)
                    Text("Beverage Aficionado • @${post.author.username}", color = LinkedInMutedGray, fontSize = 11.sp)
                    Text(post.createdAt.take(10), color = LinkedInMutedGray, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post content text
            Text(post.text, color = LinkedInTextBlack, style = MaterialTheme.typography.body2, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Badges row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(LinkedInBlue.copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        post.drinkCategory.name,
                        style = MaterialTheme.typography.caption,
                        color = LinkedInBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                post.drinkType?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF8B5A2B).copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.caption,
                            color = Color(0xFF8B5A2B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                post.rating?.let {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("⭐ $it/5", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                }
            }

            if (!post.tastingNotes.isNullOrEmpty() || !post.scenario.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LinkedInLightGray, shape = RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    if (!post.tastingNotes.isNullOrEmpty()) {
                        Text("Notes: ${post.tastingNotes}", fontSize = 11.sp, color = LinkedInTextBlack)
                    }
                    if (!post.scenario.isNullOrEmpty()) {
                        Text("Scenario: ${post.scenario}", fontSize = 11.sp, color = LinkedInTextBlack)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total social bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = LinkedInBlue, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$likeCount", color = LinkedInMutedGray, fontSize = 11.sp)
                }
                Text("${post.commentCount} comments", color = LinkedInMutedGray, fontSize = 11.sp)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = LinkedInBorderColor, thickness = 0.5.dp)

            // Dynamic Action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                Row(
                    modifier = Modifier
                        .clickable { onLikeToggle() }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) LinkedInBlue else LinkedInMutedGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Like", color = if (isLiked) LinkedInBlue else LinkedInMutedGray, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                // Comment Toggle Button
                Row(
                    modifier = Modifier
                        .clickable { showComments = !showComments }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💬", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comment", color = LinkedInMutedGray, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                // Save Toggle Button
                Row(
                    modifier = Modifier
                        .clickable { onSaveToggle() }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Save",
                        tint = if (isSaved) LinkedInBlue else LinkedInMutedGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSaved) "Saved" else "Save", color = if (isSaved) LinkedInBlue else LinkedInMutedGray, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            // Inline expandable comment section
            if (showComments) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = LinkedInBorderColor, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                var localCommentInput by remember { mutableStateOf("") }
                val localComments = remember {
                    mutableStateListOf(
                        "David Sterling" to "Amazing tasting notes! Very insightful.",
                        "Elena Rostova" to "Agreed. Adding this to my wishlist."
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick add comment row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = localCommentInput,
                            onValueChange = { localCommentInput = it },
                            placeholder = { Text("Add a comment...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (localCommentInput.isNotBlank()) {
                                    localComments.add("Me" to localCommentInput)
                                    localCommentInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = LinkedInBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(32.dp),
                            enabled = localCommentInput.isNotBlank()
                        ) {
                            Text("Post", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Existing comment list
                    localComments.forEach { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LinkedInLightGray, shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(comment.first, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(comment.second, fontSize = 12.sp, color = LinkedInTextBlack)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
