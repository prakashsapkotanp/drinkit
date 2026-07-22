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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.*
import app.drinkin.web.auth.*
import app.drinkin.web.chat.*
import app.drinkin.web.common.*
import app.drinkin.web.feed.*
import app.drinkin.web.group.*
import app.drinkin.web.profile.*
import app.drinkin.web.saved.*
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

@Composable
fun OtherUserProfileContent(
    profile: UserProfile,
    isMe: Boolean,
    otherUserPosts: List<Post>,
    isOtherUserPostsLoading: Boolean,
    onConnectClick: () -> Unit,
    onAcceptConnection: () -> Unit,
    onRejectConnection: () -> Unit,
    onMessageClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
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
                        IconButton(onClick = onCloseClick) {
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
                                        onClick = onConnectClick,
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
                                            onClick = onAcceptConnection,
                                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                                        ) {
                                            Text("Accept", color = Color.White)
                                        }

                                        Button(
                                            onClick = onRejectConnection,
                                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
                                        ) {
                                            Text("Reject", color = Color.White)
                                        }
                                    }
                                }
                                "CONNECTED" -> {
                                    Button(
                                        onClick = onMessageClick,
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

@Composable
fun WebDashboardScreen(
    apiClient: DrinkinApiClient,
    token: String?,
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

    var pendingCount by remember { mutableStateOf(0) }
    var unreadMessagesCount by remember { mutableStateOf(0) }

    // Real Conversations list
    var realConversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var realMessages by remember { mutableStateOf<List<Message>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    fun loadUnreadCounts() {
        coroutineScope.launch {
            try {
                val counts = apiClient.getUnreadCounts()
                pendingCount = counts.pendingConnectionsCount
                unreadMessagesCount = counts.unreadMessagesCount
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            loadUnreadCounts()
            delay(5000)
        }
    }

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
                loadUnreadCounts()
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
                loadUnreadCounts()
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
            loadNetworkData()
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
                            Box {
                                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
                                val badgeCount = when (tab) {
                                    DashboardTab.MY_GROUP -> pendingCount
                                    DashboardTab.CHAT -> unreadMessagesCount
                                    else -> 0
                                }
                                if (badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-6).dp)
                                            .background(Color.Red, shape = CircleShape)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeCount.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
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
                    if (errorMsg != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFFDE8E8),
                            shape = RoundedCornerShape(8.dp),
                            elevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFFE53E3E))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMsg!!, color = Color(0xFFC53030), fontSize = 13.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { errorMsg = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFC53030), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (viewingOtherUserId != null) {
                        if (isOtherProfileLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = DrinkinAccentBlue)
                        } else otherUserProfile?.let { profile ->
                            val isMe = profile.id == myUserId
                            OtherUserProfileContent(
                                profile = profile,
                                isMe = isMe,
                                otherUserPosts = otherUserPosts,
                                isOtherUserPostsLoading = isOtherUserPostsLoading,
                                onConnectClick = {
                                    coroutineScope.launch {
                                        try {
                                            apiClient.sendConnectionRequest(ConnectionRequest(addresseeId = profile.id))
                                            loadOtherUserProfileAndPosts(profile.id)
                                        } catch (e: Exception) {}
                                    }
                                },
                                onAcceptConnection = {
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
                                onRejectConnection = {
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
                                onMessageClick = {
                                    coroutineScope.launch {
                                        try {
                                            val conv = apiClient.getOrCreateConversation(ConversationRequest(otherUserId = profile.id))
                                            selectedConversationId = conv.id
                                            viewingOtherUserId = null
                                            onTabChange(DashboardTab.CHAT)
                                        } catch (e: Exception) {}
                                    }
                                },
                                onCloseClick = {
                                    viewingOtherUserId = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        when (currentTab) {
                            DashboardTab.HOME -> {
                                FeedTabContent(
                                    posts = posts,
                                    hasMore = hasMore,
                                    isPaginationLoading = isPaginationLoading,
                                    isLoading = isLoading,
                                    likedPostIds = likedPostIds,
                                    likeCountOffsets = likeCountOffsets,
                                    savedPosts = savedPosts,
                                    onStartPostClick = { showStartPostModal = true },
                                    loadNextPage = { loadNextPage() },
                                    onAuthorClick = { authorId ->
                                        viewingOtherUserId = authorId
                                        loadOtherUserProfileAndPosts(authorId)
                                    },
                                    onLikeToggle = { post, reactionType ->
                                        val isLiked = likedPostIds.contains(post.id)
                                        val offset = likeCountOffsets[post.id] ?: 0
                                        val isRemove = isLiked && reactionType == "LIKE"
                                        val newIsLiked = !isRemove
                                        likedPostIds = if (newIsLiked) likedPostIds + post.id else likedPostIds - post.id
                                        likeCountOffsets = likeCountOffsets + (post.id to (if (newIsLiked) (if (isLiked) offset else offset + 1) else offset - 1))

                                        coroutineScope.launch {
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
                                    onSaveToggle = { post ->
                                        val isSaved = savedPosts.any { it.id == post.id }
                                        if (isSaved) {
                                            savedPosts.removeAll { it.id == post.id }
                                        } else {
                                            savedPosts.add(post)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            DashboardTab.MY_GROUP -> {
                                GroupTabContent(
                                    pendingConnections = pendingConnections,
                                    activeConnections = activeConnections,
                                    onAcceptConnection = { req ->
                                        coroutineScope.launch {
                                            try {
                                                apiClient.acceptConnection(req.id)
                                                loadNetworkData()
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    onRejectConnection = { req ->
                                        coroutineScope.launch {
                                            try {
                                                apiClient.rejectConnection(req.id)
                                                loadNetworkData()
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    onConnectionClick = { conn ->
                                        viewingOtherUserId = conn.id
                                        loadOtherUserProfileAndPosts(conn.id)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            DashboardTab.CHAT -> {
                                ChatTabContent(
                                    activeConnections = activeConnections,
                                    realConversations = realConversations,
                                    selectedConversationId = selectedConversationId,
                                    realMessages = realMessages,
                                    onSelectConversation = { convId ->
                                        selectedConversationId = convId
                                        loadMessagesForActiveConv(convId)
                                    },
                                    onStartConversation = { conn ->
                                        coroutineScope.launch {
                                            try {
                                                val conv = apiClient.getOrCreateConversation(ConversationRequest(otherUserId = conn.id))
                                                selectedConversationId = conv.id
                                                loadConversationsList()
                                                loadMessagesForActiveConv(conv.id)
                                            } catch (e: Exception) {
                                                errorMsg = getErrorMessage(e)
                                            }
                                        }
                                    },
                                    onSendMessage = { activeConv, text ->
                                        coroutineScope.launch {
                                            try {
                                                val sent = apiClient.sendMessage(activeConv.id, MessageRequest(text = text))
                                                realMessages = realMessages + sent
                                                loadConversationsList()
                                                loadUnreadCounts()
                                            } catch (e: Exception) {
                                                errorMsg = getErrorMessage(e)
                                            }
                                        }
                                    },
                                    onQuickGreetingClick = { activeConv, option ->
                                        coroutineScope.launch {
                                            try {
                                                val sent = apiClient.sendMessage(activeConv.id, MessageRequest(text = option))
                                                realMessages = realMessages + sent
                                                loadConversationsList()
                                                loadUnreadCounts()
                                            } catch (e: Exception) {
                                                errorMsg = getErrorMessage(e)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            DashboardTab.SAVED -> {
                                SavedTabContent(
                                    savedPosts = savedPosts,
                                    onAuthorClick = { authorId ->
                                        viewingOtherUserId = authorId
                                        loadOtherUserProfileAndPosts(authorId)
                                    },
                                    onSaveToggle = { post ->
                                        savedPosts.removeAll { it.id == post.id }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            DashboardTab.PROFILE -> {
                                ProfileTabContent(
                                    myProfileState = myProfileState,
                                    onUpdateProfile = { displayName, bio, avatarUrl, drinkPreferences ->
                                        coroutineScope.launch {
                                            try {
                                                val updated = apiClient.updateProfile(
                                                    UpdateProfileRequest(
                                                        displayName = displayName.takeIf { it.isNotBlank() },
                                                        bio = bio.takeIf { it.isNotBlank() },
                                                        avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                                                        drinkPreferences = drinkPreferences
                                                    )
                                                )
                                                myProfileState = updated
                                                onUserAboutChange(updated.bio ?: "")
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
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
                                            inlineError = getErrorMessage(e)
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