package app.drinkin.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.*
import app.drinkin.web.common.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    var viewingOtherUserId by remember { mutableStateOf<String?>(null) }
    var otherUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isOtherProfileLoading by remember { mutableStateOf(false) }
    var otherUserPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isOtherUserPostsLoading by remember { mutableStateOf(false) }

    var myUserId by remember { mutableStateOf<String?>(null) }
    var myProfileState by remember { mutableStateOf<UserProfile?>(null) }

    var pendingConnections by remember { mutableStateOf<List<PendingConnectionRequest>>(emptyList()) }
    var activeConnections by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var pendingCount by remember { mutableStateOf(0) }
    var unreadMessagesCount by remember { mutableStateOf(0) }

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
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        while (true) { loadUnreadCounts(); delay(5000) }
    }

    fun loadInitialFeed() {
        isLoading = true; errorMsg = null; posts = emptyList(); nextCursor = null; hasMore = true
        coroutineScope.launch {
            try {
                val myProfile = apiClient.getUserProfile("me")
                myUserId = myProfile.id; myProfileState = myProfile; onUserAboutChange(myProfile.bio ?: "")
                val page = apiClient.getFeed(cursor = null)
                posts = page.items; nextCursor = page.nextCursor; hasMore = page.nextCursor != null; loadUnreadCounts()
            } catch (e: Exception) {
                errorMsg = "Failed to load feed. Please check your connection."
            } finally { isLoading = false }
        }
    }

    fun loadNextPage() {
        if (isPaginationLoading || !hasMore) return
        isPaginationLoading = true
        coroutineScope.launch {
            try {
                val page = apiClient.getFeed(cursor = nextCursor)
                posts = posts + page.items; nextCursor = page.nextCursor; hasMore = page.nextCursor != null
            } catch (e: Exception) {} finally { isPaginationLoading = false }
        }
    }

    fun loadNetworkData() {
        coroutineScope.launch {
            try {
                pendingConnections = apiClient.getPendingRequests().items
                activeConnections = apiClient.getConnections().items
            } catch (e: Exception) {}
        }
    }

    fun loadConversationsList() {
        coroutineScope.launch {
            try { realConversations = apiClient.getConversations().items } catch (e: Exception) {}
        }
    }

    fun loadMessagesForActiveConv(convId: String) {
        coroutineScope.launch {
            try { realMessages = apiClient.getMessages(convId).items; loadUnreadCounts() } catch (e: Exception) {}
        }
    }

    fun loadOtherUserProfileAndPosts(userId: String) {
        isOtherProfileLoading = true
        coroutineScope.launch {
            try {
                val profile = apiClient.getUserProfile(userId)
                otherUserProfile = profile; isOtherUserPostsLoading = true
                val postsPage = apiClient.getUserPosts(profile.id)
                otherUserPosts = postsPage.items
            } catch (e: Exception) {} finally { isOtherProfileLoading = false; isOtherUserPostsLoading = false }
        }
    }

    LaunchedEffect(feedRefreshKey) { loadInitialFeed() }

    LaunchedEffect(currentTab, selectedConversationId) {
        while (true) {
            if (currentTab == DashboardTab.MY_GROUP) { loadNetworkData() }
            else if (currentTab == DashboardTab.CHAT) {
                loadNetworkData(); loadConversationsList()
                selectedConversationId?.let { convId ->
                    try {
                        val msgs = apiClient.getMessages(convId).items
                        if (msgs.size != realMessages.size || msgs.firstOrNull()?.id != realMessages.firstOrNull()?.id) { realMessages = msgs }
                    } catch (e: Exception) {}
                }
            }
            delay(2000)
        }
    }

    DashboardLayout(
        apiClient = apiClient, currentTab = currentTab, onTabChange = onTabChange,
        viewingOtherUserId = viewingOtherUserId, pendingCount = pendingCount, unreadMessagesCount = unreadMessagesCount,
        onLogout = onLogout, onUserSelect = { viewingOtherUserId = it; loadOtherUserProfileAndPosts(it) },
        myProfileState = myProfileState, activeConnections = activeConnections, userAboutText = userAboutText,
        errorMsg = errorMsg, onErrorMsgClose = { errorMsg = null }, isOtherProfileLoading = isOtherProfileLoading,
        otherUserProfile = otherUserProfile, otherUserPosts = otherUserPosts, isOtherUserPostsLoading = isOtherUserPostsLoading,
        onConnectClick = { coroutineScope.launch { try { apiClient.sendConnectionRequest(ConnectionRequest(addresseeId = otherUserProfile?.id ?: "")); loadOtherUserProfileAndPosts(otherUserProfile?.id ?: "") } catch (e: Exception) {} } },
        onAcceptConnectionClick = { coroutineScope.launch { try { apiClient.getPendingRequests().items.firstOrNull { it.requester.id == otherUserProfile?.id }?.let { apiClient.acceptConnection(it.id); loadOtherUserProfileAndPosts(otherUserProfile?.id ?: "") } } catch (e: Exception) {} } },
        onRejectConnectionClick = { coroutineScope.launch { try { apiClient.getPendingRequests().items.firstOrNull { it.requester.id == otherUserProfile?.id }?.let { apiClient.rejectConnection(it.id); loadOtherUserProfileAndPosts(otherUserProfile?.id ?: "") } } catch (e: Exception) {} } },
        onMessageClick = { coroutineScope.launch { try { val conv = apiClient.getOrCreateConversation(ConversationRequest(otherUserId = otherUserProfile?.id ?: "")); selectedConversationId = conv.id; viewingOtherUserId = null; onTabChange(DashboardTab.CHAT) } catch (e: Exception) {} } },
        onCloseOtherProfileClick = { viewingOtherUserId = null },
        onOtherProfileLikeToggle = { post, reactionType ->
            val isRemove = post.myReaction == reactionType
            otherUserPosts = otherUserPosts.map { if (it.id == post.id) updatePostReaction(post, if (isRemove) null else reactionType) else it }
            coroutineScope.launch { try { if (isRemove) apiClient.unlikePost(post.id) else apiClient.likePost(post.id, reactionType) } catch (e: Exception) { otherUserPosts = otherUserPosts.map { if (it.id == post.id) post else it } } }
        },
        posts = posts, hasMore = hasMore, isPaginationLoading = isPaginationLoading, isLoading = isLoading,
        likedPostIds = likedPostIds, likeCountOffsets = likeCountOffsets, savedPosts = savedPosts,
        onStartPostClick = { showStartPostModal = true }, loadNextPage = { loadNextPage() },
        onAuthorClick = { viewingOtherUserId = it; loadOtherUserProfileAndPosts(it) },
        onLikeToggle = { post, reactionType ->
            val isRemove = post.myReaction == reactionType
            posts = posts.map { if (it.id == post.id) updatePostReaction(post, if (isRemove) null else reactionType) else it }
            coroutineScope.launch { try { if (isRemove) apiClient.unlikePost(post.id) else apiClient.likePost(post.id, reactionType) } catch (e: Exception) { posts = posts.map { if (it.id == post.id) post else it } } }
        },
        onSaveToggle = { post -> if (savedPosts.any { it.id == post.id }) savedPosts.removeAll { it.id == post.id } else savedPosts.add(post) },
        pendingConnections = pendingConnections,
        onAcceptGroupConnection = { req -> coroutineScope.launch { try { apiClient.acceptConnection(req.id); loadNetworkData() } catch (e: Exception) {} } },
        onRejectGroupConnection = { req -> coroutineScope.launch { try { apiClient.rejectConnection(req.id); loadNetworkData() } catch (e: Exception) {} } },
        onGroupConnectionClick = { viewingOtherUserId = it.id; loadOtherUserProfileAndPosts(it.id) },
        realConversations = realConversations, selectedConversationId = selectedConversationId, realMessages = realMessages,
        onSelectConversation = { selectedConversationId = it; loadMessagesForActiveConv(it) },
        onStartConversation = { conn -> coroutineScope.launch { try { val conv = apiClient.getOrCreateConversation(ConversationRequest(otherUserId = conn.id)); selectedConversationId = conv.id; loadConversationsList(); loadMessagesForActiveConv(conv.id) } catch (e: Exception) { errorMsg = getErrorMessage(e) } } },
        onSendMessage = { activeConv, text -> coroutineScope.launch { try { val sent = apiClient.sendMessage(activeConv.id, MessageRequest(text = text)); realMessages = realMessages + sent; loadConversationsList(); loadUnreadCounts() } catch (e: Exception) { errorMsg = getErrorMessage(e) } } },
        onQuickGreetingClick = { activeConv, option -> coroutineScope.launch { try { val sent = apiClient.sendMessage(activeConv.id, MessageRequest(text = option)); realMessages = realMessages + sent; loadConversationsList(); loadUnreadCounts() } catch (e: Exception) { errorMsg = getErrorMessage(e) } } },
        onUpdateProfile = { displayName, bio, avatarUrl, drinkPreferences -> coroutineScope.launch { try { val updated = apiClient.updateProfile(UpdateProfileRequest(displayName = displayName.takeIf { it.isNotBlank() }, bio = bio.takeIf { it.isNotBlank() }, avatarUrl = avatarUrl.takeIf { it.isNotBlank() }, drinkPreferences = drinkPreferences)); myProfileState = updated; onUserAboutChange(updated.bio ?: "") } catch (e: Exception) {} } }
    )

    if (showStartPostModal) {
        StartPostModal(apiClient = apiClient, onDismiss = { showStartPostModal = false }, onForceFeedRefresh = onForceFeedRefresh)
    }
}
