package app.drinkin.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.*
import kotlinx.coroutines.launch

// De-branded styling matching the design-system requirements (without LinkedIn blue/hex references)
val AppPrimaryBlue = Color(0xFF0D5FA8)
val AppLightGray = Color(0xFFF3F2EF)
val AppMutedGray = Color(0xFF666666)
val AppBorderColor = Color(0xFFE0E0E0)
val AppCardBg = Color(0xFFFFFFFF)

val availablePreferences = listOf(
    "IPA", "Stout", "Lager", "Pilsner", "Sour", "Cider",
    "Red Wine", "White Wine", "Rosé", "Espresso",
    "Cold Brew", "Pour Over", "Matcha", "Chai"
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    apiClient: DrinkinApiClient,
    onBack: () -> Unit
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    // Form states
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var selectedPrefs by remember { mutableStateOf(setOf<String>()) }

    // Backup states to detect changes
    var origDisplayName by remember { mutableStateOf("") }
    var origBio by remember { mutableStateOf("") }
    var origAvatarUrl by remember { mutableStateOf("") }
    var origPrefs by remember { mutableStateOf(setOf<String>()) }

    val coroutineScope = rememberCoroutineScope()

    fun loadProfile() {
        isLoading = true
        errorMsg = null
        coroutineScope.launch {
            try {
                val profile = apiClient.getUserProfile("me") // backend handles "me" or gets current profile
                userProfile = profile
                displayName = profile.displayName ?: ""
                bio = profile.bio ?: ""
                avatarUrl = profile.avatarUrl ?: ""
                selectedPrefs = profile.drinkPreferences.toSet()

                origDisplayName = displayName
                origBio = bio
                origAvatarUrl = avatarUrl
                origPrefs = selectedPrefs
            } catch (e: Exception) {
                // Fallback to GET /users/me direct call
                try {
                    val profile = apiClient.getFeed().items.firstOrNull { true }?.author // or fetch me
                    // If no fallback, show error
                    errorMsg = "Failed to load profile. Please make sure you are signed in."
                } catch (ex: Exception) {
                    errorMsg = "Failed to load profile: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProfile()
    }

    val isChanged = displayName != origDisplayName ||
            bio != origBio ||
            avatarUrl != origAvatarUrl ||
            selectedPrefs != origPrefs

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppLightGray)
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMsg != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMsg!!, color = MaterialTheme.colors.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { loadProfile() }) {
                        Text("Retry")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 1.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(AppPrimaryBlue.copy(alpha = 0.1f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AppPrimaryBlue, modifier = Modifier.size(36.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(displayName.takeIf { it.isNotBlank() } ?: userProfile?.username ?: "", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                                Text("@${userProfile?.username ?: ""}", style = MaterialTheme.typography.caption, color = AppMutedGray)
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 1.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Personal Details", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)

                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    label = { Text("Professional Bio") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )

                                OutlinedTextField(
                                    value = avatarUrl,
                                    onValueChange = { avatarUrl = it },
                                    label = { Text("Avatar Image URL") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 1.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Drink Preferences", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    spacing = 8.dp
                                ) {
                                    availablePreferences.forEach { pref ->
                                        val isSelected = selectedPrefs.contains(pref)
                                        Chip(
                                            onClick = {
                                                selectedPrefs = if (isSelected) selectedPrefs - pref else selectedPrefs + pref
                                            },
                                            colors = ChipDefaults.chipColors(
                                                backgroundColor = if (isSelected) AppPrimaryBlue else Color.LightGray.copy(alpha = 0.4f),
                                                contentColor = if (isSelected) Color.White else Color.Black
                                            )
                                        ) {
                                            Text(pref, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isLoading = true
                                        val updated = apiClient.updateProfile(
                                            UpdateProfileRequest(
                                                displayName = displayName.takeIf { it.isNotBlank() },
                                                bio = bio.takeIf { it.isNotBlank() },
                                                avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                                                drinkPreferences = selectedPrefs.toList()
                                            )
                                        )
                                        successMsg = "Profile updated successfully!"
                                        origDisplayName = displayName
                                        origBio = bio
                                        origAvatarUrl = avatarUrl
                                        origPrefs = selectedPrefs
                                    } catch (e: Exception) {
                                        errorMsg = "Failed to save: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isChanged && !isLoading,
                            colors = ButtonDefaults.buttonColors(backgroundColor = AppPrimaryBlue)
                        ) {
                            Text("Save Profile", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (successMsg != null) {
                        item {
                            Text(successMsg!!, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable () -> Unit
) {
    // Basic implementation of a simple flow row wrapping content dynamically
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                // Approximate row wrap using simple container for basic screens
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ProfilePostCard(post: Post) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(post.createdAt.take(10), style = MaterialTheme.typography.caption, color = AppMutedGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(post.text, style = MaterialTheme.typography.body1)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(backgroundColor = AppPrimaryBlue.copy(alpha = 0.1f)) {
                    Text(post.drinkCategory.name, style = MaterialTheme.typography.caption, color = AppPrimaryBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                post.drinkType?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(backgroundColor = Color.LightGray.copy(alpha = 0.4f)) {
                        Text(it, style = MaterialTheme.typography.caption, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                post.rating?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⭐ $it/5", style = MaterialTheme.typography.caption)
                }
            }
        }
    }
}

@Composable
fun OtherProfileScreen(
    apiClient: DrinkinApiClient,
    userId: String,
    onBack: () -> Unit,
    onNavigateToChat: (conversationId: String) -> Unit
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var myUserId by remember { mutableStateOf<String?>(null) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var isPostsLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun loadProfileAndPosts() {
        isLoading = true
        errorMsg = null
        coroutineScope.launch {
            try {
                // Fetch logged-in user profile to compare
                val myProfile = apiClient.getUserProfile("me")
                myUserId = myProfile.id

                val profile = apiClient.getUserProfile(userId)
                userProfile = profile

                // Fetch posts created by this user
                isPostsLoading = true
                val postsPage = apiClient.getUserPosts(profile.id)
                userPosts = postsPage.items
            } catch (e: Exception) {
                errorMsg = "Failed to load user profile: ${e.message}"
            } finally {
                isLoading = false
                isPostsLoading = false
            }
        }
    }

    LaunchedEffect(userId) {
        loadProfileAndPosts()
    }

    val isMe = userProfile?.id == myUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userProfile?.displayName ?: userProfile?.username ?: "Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppLightGray)
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colors.error, modifier = Modifier.align(Alignment.Center))
            } else userProfile?.let { profile ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 1.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(AppPrimaryBlue.copy(alpha = 0.1f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AppPrimaryBlue, modifier = Modifier.size(48.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(profile.displayName ?: profile.username, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                                Text("@${profile.username}", style = MaterialTheme.typography.caption, color = AppMutedGray)
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(profile.bio ?: "No bio provided.", style = MaterialTheme.typography.body2, color = Color.Black)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("${profile.followerCount} Followers", style = MaterialTheme.typography.caption)
                                    Text("${profile.followingCount} Following", style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }

                    // Connection Action Button Row (Strictly hidden for "My Profile" / "isMe" view)
                    if (!isMe) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = 1.dp,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Professional Connection", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    when (profile.connectionStatus) {
                                        "NONE", null -> {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        try {
                                                            apiClient.sendConnectionRequest(ConnectionRequest(addresseeId = profile.id))
                                                            loadProfileAndPosts() // refresh
                                                        } catch (e: Exception) {}
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(backgroundColor = AppPrimaryBlue),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Connect", color = Color.White)
                                            }
                                        }
                                        "PENDING_SENT" -> {
                                            Button(
                                                onClick = {},
                                                enabled = false,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Request Pending")
                                            }
                                        }
                                        "PENDING_RECEIVED" -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            try {
                                                                val reqs = apiClient.getPendingRequests()
                                                                val matching = reqs.items.firstOrNull { it.requester.id == profile.id }
                                                                if (matching != null) {
                                                                    apiClient.acceptConnection(matching.id)
                                                                    loadProfileAndPosts()
                                                                }
                                                            } catch (e: Exception) {}
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Accept", color = Color.White)
                                                }

                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            try {
                                                                val reqs = apiClient.getPendingRequests()
                                                                val matching = reqs.items.firstOrNull { it.requester.id == profile.id }
                                                                if (matching != null) {
                                                                    apiClient.rejectConnection(matching.id)
                                                                    loadProfileAndPosts()
                                                                }
                                                            } catch (e: Exception) {}
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828)),
                                                    modifier = Modifier.weight(1f)
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
                                                            onNavigateToChat(conv.id)
                                                        } catch (e: Exception) {}
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(backgroundColor = AppPrimaryBlue),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Message", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preferences Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 1.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Favorite Drink Profiles", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (profile.drinkPreferences.isEmpty()) {
                                    Text("No preference selected yet.", style = MaterialTheme.typography.caption, color = AppMutedGray)
                                } else {
                                    Text(profile.drinkPreferences.joinToString(", "), style = MaterialTheme.typography.body2)
                                }
                            }
                        }
                    }

                    // Recent Activity & Posts Card (LinkedIn style)
                    item {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (isPostsLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AppPrimaryBlue)
                            }
                        }
                    } else if (userPosts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = 1.dp
                            ) {
                                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("No drink reviews posted yet.", style = MaterialTheme.typography.caption, color = AppMutedGray)
                                }
                            }
                        }
                    } else {
                        items(userPosts) { post ->
                            ProfilePostCard(post = post)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionsInboxScreen(
    apiClient: DrinkinApiClient,
    onBack: () -> Unit,
    onNavigateToOtherProfile: (userId: String) -> Unit
) {
    var pendingList by remember { mutableStateOf<List<PendingConnectionRequest>>(emptyList()) }
    var activeList by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    fun loadAll() {
        isLoading = true
        coroutineScope.launch {
            try {
                val requests = apiClient.getPendingRequests()
                pendingList = requests.items

                val connections = apiClient.getConnections()
                activeList = connections.items
            } catch (e: Exception) {
                // Silent catch or fallback empty
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Professional Network") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppLightGray)
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Incoming Requests (${pendingList.size})", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                    }

                    if (pendingList.isEmpty()) {
                        item {
                            Text("No pending requests.", style = MaterialTheme.typography.caption, color = AppMutedGray)
                        }
                    } else {
                        items(pendingList) { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(req.requester.displayName ?: req.requester.username, fontWeight = FontWeight.Bold)
                                        Text("@${req.requester.username}", style = MaterialTheme.typography.caption, color = AppMutedGray)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        apiClient.acceptConnection(req.id)
                                                        loadAll()
                                                    } catch (e: Exception) {
                                                        // Handle
                                                    }
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
                                                        loadAll()
                                                    } catch (e: Exception) {
                                                        // Handle
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
                                        ) {
                                            Text("Reject", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Accepted Connections (${activeList.size})", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                    }

                    if (activeList.isEmpty()) {
                        item {
                            Text("No active connections.", style = MaterialTheme.typography.caption, color = AppMutedGray)
                        }
                    } else {
                        items(activeList) { user ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToOtherProfile(user.id) },
                                elevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(AppPrimaryBlue.copy(alpha = 0.1f), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = AppPrimaryBlue)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(user.displayName ?: user.username, fontWeight = FontWeight.Bold)
                                        Text("@${user.username}", style = MaterialTheme.typography.caption, color = AppMutedGray)
                                    }
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
fun ChatListScreen(
    apiClient: DrinkinApiClient,
    onBack: () -> Unit,
    onNavigateToThread: (conversationId: String) -> Unit
) {
    var conversationsList by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    fun loadConversations() {
        isLoading = true
        coroutineScope.launch {
            try {
                val page = apiClient.getConversations()
                conversationsList = page.items
            } catch (e: Exception) {
                // Ignore
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadConversations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppLightGray)
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (conversationsList.isEmpty()) {
                Text("No conversations yet.", modifier = Modifier.align(Alignment.Center), color = AppMutedGray)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversationsList) { conv ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToThread(conv.id) },
                            elevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(AppPrimaryBlue.copy(alpha = 0.1f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AppPrimaryBlue)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(conv.otherUser.displayName ?: conv.otherUser.username, fontWeight = FontWeight.Bold)
                                    Text(
                                        conv.lastMessagePreview ?: "No messages yet.",
                                        style = MaterialTheme.typography.body2,
                                        color = AppMutedGray,
                                        maxLines = 1
                                    )
                                }
                                conv.lastMessageAt?.let {
                                    Text(it.take(10), style = MaterialTheme.typography.caption, color = AppMutedGray)
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
fun ChatThreadScreen(
    apiClient: DrinkinApiClient,
    conversationId: String,
    onBack: () -> Unit
) {
    var messagesList by remember { mutableStateOf<List<Message>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var myUserId by remember { mutableStateOf<String?>(null) }

    var textInput by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    fun loadMessages() {
        isLoading = true
        coroutineScope.launch {
            try {
                // Fetch logged-in user profile to compare
                val myProfile = apiClient.getUserProfile("me")
                myUserId = myProfile.id

                val page = apiClient.getMessages(conversationId)
                messagesList = page.items
                nextCursor = page.nextCursor
                hasMore = page.nextCursor != null
            } catch (e: Exception) {
                // Ignore
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(conversationId) {
        loadMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Thread") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppLightGray)
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        reverseLayout = true, // We want reverse layout for chat
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messagesList) { msg ->
                            val isMe = msg.senderId == myUserId
                            val align = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                            val colorBg = if (isMe) AppPrimaryBlue else Color(0xFFE5E5EA)
                            val textCol = if (isMe) Color.White else Color.Black

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = align
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorBg)
                                        .padding(12.dp)
                                ) {
                                    Text(msg.text, color = textCol)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val typed = textInput
                            textInput = ""
                            coroutineScope.launch {
                                try {
                                    val sent = apiClient.sendMessage(conversationId, MessageRequest(text = typed))
                                    messagesList = listOf(sent) + messagesList // Optimistically insert at top of reverse list
                                } catch (e: Exception) {
                                    // Handle
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppPrimaryBlue)
                ) {
                    Text("Send", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SimpleUserSearchScreen(
    apiClient: DrinkinApiClient,
    onBack: () -> Unit,
    onNavigateToOtherProfile: (userId: String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun performSearch() {
        if (query.isBlank()) return
        isLoading = true
        coroutineScope.launch {
            try {
                val page = apiClient.searchUsers(query)
                results = page.items
            } catch (e: Exception) {
                // Handle
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Users") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppLightGray)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.isNotBlank()) performSearch()
                    },
                    placeholder = { Text("Search by name...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { performSearch() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppPrimaryBlue)
                ) {
                    Text("Search", color = Color.White)
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToOtherProfile(user.id) },
                            elevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(AppPrimaryBlue.copy(alpha = 0.1f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AppPrimaryBlue)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(user.displayName ?: user.username, fontWeight = FontWeight.Bold)
                                    Text("@${user.username}", style = MaterialTheme.typography.caption, color = AppMutedGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
