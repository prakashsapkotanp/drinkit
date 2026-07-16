package app.drinkin.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.CreatePostRequest
import app.drinkin.shared.model.DrinkCategory
import app.drinkin.shared.model.LoginRequest
import app.drinkin.shared.model.Post
import app.drinkin.shared.model.RegisterRequest
import io.ktor.client.plugins.ResponseException
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Drinkin' Web — Login") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Login to Drinkin'", style = MaterialTheme.typography.h5)

                    TextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMsg = null
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMsg = null
                        },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    errorMsg?.let {
                        Text(it, color = MaterialTheme.colors.error)
                    }

                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                if (!isValidEmail(email)) {
                                    errorMsg = "Please enter a valid email address."
                                    return@Button
                                }
                                if (password.length < 8) {
                                    errorMsg = "Password must be at least 8 characters long."
                                    return@Button
                                }

                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.login(LoginRequest(email = email, password = password))
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
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = email.isNotBlank() && password.isNotBlank()
                        ) {
                            Text("Login")
                        }
                    }

                    TextButton(onClick = onNavigateToRegister) {
                        Text("Don't have an account? Register")
                    }
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Drinkin' Web — Register") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Create Drinkin' Account", style = MaterialTheme.typography.h5)

                    TextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMsg = null
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMsg = null
                        },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMsg = null
                        },
                        label = { Text("Password (8+ chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    TextField(
                        value = dob,
                        onValueChange = {
                            dob = it
                            errorMsg = null
                        },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMsg?.let {
                        Text(it, color = MaterialTheme.colors.error)
                    }

                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                if (!isValidEmail(email)) {
                                    errorMsg = "Please enter a valid email address."
                                    return@Button
                                }
                                if (username.length !in 3..50) {
                                    errorMsg = "Username must be between 3 and 50 characters."
                                    return@Button
                                }
                                if (password.length < 8) {
                                    errorMsg = "Password must be at least 8 characters long."
                                    return@Button
                                }
                                val dobRegex = "^\\d{4}-\\d{2}-\\d{2}\$".toRegex()
                                if (!dobRegex.matches(dob)) {
                                    errorMsg = "Date of Birth must be in YYYY-MM-DD format."
                                    return@Button
                                }
                                if (isUnderage(dob)) {
                                    errorMsg = "Registration failed: Must be at least 18 years old."
                                    return@Button
                                }

                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.register(
                                            RegisterRequest(
                                                email = email,
                                                username = username,
                                                password = password,
                                                dateOfBirth = dob
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
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = email.isNotBlank() && username.isNotBlank() && password.isNotBlank() && dob.isNotBlank()
                        ) {
                            Text("Register")
                        }
                    }

                    TextButton(onClick = onNavigateToLogin) {
                        Text("Already have an account? Login")
                    }
                }
            }
        }
    }
}

@Composable
fun WebFeedScreen(
    apiClient: DrinkinApiClient,
    onNavigateToCreatePost: () -> Unit,
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

    LaunchedEffect(Unit) {
        loadInitialFeed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drinkin' Web Feed") },
                actions = {
                    Button(onClick = onNavigateToCreatePost) {
                        Text("Create Post")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colors.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Centered maximum width container for a clean, responsive layout on desktop
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator()
                } else if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(errorMsg!!, color = MaterialTheme.colors.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { loadInitialFeed() }) {
                        Text("Retry")
                    }
                } else if (posts.isEmpty()) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text("It looks like your feed is empty!", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Find people to follow to see their drink posts here.", style = MaterialTheme.typography.body2)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(posts) { index, post ->
                            if (index >= posts.size - 2 && hasMore && !isPaginationLoading) {
                                loadNextPage()
                            }

                            val isLiked = likedPostIds.contains(post.id)
                            val offset = likeCountOffsets[post.id] ?: 0
                            val displayedLikeCount = post.likeCount + offset

                            WebPostCard(
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
                                }
                            )
                        }

                        if (isPaginationLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebPostCard(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    onLikeToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(post.author.displayName ?: post.author.username, style = MaterialTheme.typography.subtitle1)
            Text("@${post.author.username} • ${post.createdAt.take(10)}", style = MaterialTheme.typography.caption)
            Spacer(modifier = Modifier.height(8.dp))

            Text(post.text, style = MaterialTheme.typography.body1)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)) {
                    Text(
                        post.drinkCategory.name,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                post.drinkType?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f)) {
                        Text(
                            it,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                post.rating?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⭐ $it/5", style = MaterialTheme.typography.caption)
                }
            }

            if (!post.tastingNotes.isNull_or_empty() || !post.scenario.isNull_or_empty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    if (!post.tastingNotes.isNull_or_empty()) {
                        Text("Notes: ${post.tastingNotes}", style = MaterialTheme.typography.caption)
                    }
                    if (!post.scenario.isNull_or_empty()) {
                        Text("Scenario: ${post.scenario}", style = MaterialTheme.typography.caption)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLikeToggle, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colors.primary else LocalContentColor.current
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$likeCount", style = MaterialTheme.typography.body2)
                }

                Text("${post.commentCount} comments", style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
fun WebCreatePostScreen(
    apiClient: DrinkinApiClient,
    onBackToFeed: () -> Unit,
    onPostCreated: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var drinkCategory by remember { mutableStateOf(DrinkCategory.ALCOHOLIC) }
    var drinkType by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf<Int?>(null) }
    var tastingNotes by remember { mutableStateOf("") }
    var scenario by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Drink Review") },
                navigationIcon = {
                    TextButton(onClick = onBackToFeed) {
                        Text("< Back", color = MaterialTheme.colors.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Share Drink Experience", style = MaterialTheme.typography.h5)

                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                        errorMsg = null
                    },
                    label = { Text("Share your thoughts/review...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    minLines = 3
                )

                Column {
                    Text("Drink Category", style = MaterialTheme.typography.subtitle1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = drinkCategory == DrinkCategory.ALCOHOLIC,
                            onClick = { drinkCategory = DrinkCategory.ALCOHOLIC }
                        )
                        Text("Alcoholic", modifier = Modifier.clickable { drinkCategory = DrinkCategory.ALCOHOLIC })
                        Spacer(modifier = Modifier.width(24.dp))
                        RadioButton(
                            selected = drinkCategory == DrinkCategory.NON_ALCOHOLIC,
                            onClick = { drinkCategory = DrinkCategory.NON_ALCOHOLIC }
                        )
                        Text("Non-Alcoholic", modifier = Modifier.clickable { drinkCategory = DrinkCategory.NON_ALCOHOLIC })
                    }
                }

                TextField(
                    value = drinkType,
                    onValueChange = { drinkType = it },
                    label = { Text("Drink Type (e.g. IPA, Merlot) - Optional") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Rating (Optional)", style = MaterialTheme.typography.subtitle1)
                    Row {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (rating != null && rating!! >= i) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = "Star $i",
                                tint = if (rating != null && rating!! >= i) MaterialTheme.colors.primary else LocalContentColor.current.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { rating = if (rating == i) null else i }
                            )
                        }
                    }
                }

                TextField(
                    value = tastingNotes,
                    onValueChange = { tastingNotes = it },
                    label = { Text("Tasting Notes (e.g. Citrus) - Optional") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = scenario,
                    onValueChange = { scenario = it },
                    label = { Text("Scenario (e.g. Picnic) - Optional") },
                    modifier = Modifier.fillMaxWidth()
                )

                // STUB FOR IMAGE UPLOAD (Phase 2)
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload Image (Coming Soon in Phase 2)")
                }

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colors.error)
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            if (text.trim().isEmpty()) {
                                errorMsg = "Review text cannot be empty."
                                return@Button
                            }
                            if (text.length > 1000) {
                                errorMsg = "Review text must be under 1000 characters."
                                return@Button
                            }

                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    apiClient.createPost(
                                        CreatePostRequest(
                                            text = text,
                                            drinkCategory = drinkCategory,
                                            drinkType = drinkType.takeIf { it.isNotBlank() },
                                            rating = rating,
                                            tastingNotes = tastingNotes.takeIf { it.isNotBlank() },
                                            scenario = scenario.takeIf { it.isNotBlank() }
                                        )
                                    )
                                    onPostCreated()
                                } catch (e: Exception) {
                                    errorMsg = "Failed to create post. Please try again."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = text.isNotBlank()
                    ) {
                        Text("Create Post")
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
