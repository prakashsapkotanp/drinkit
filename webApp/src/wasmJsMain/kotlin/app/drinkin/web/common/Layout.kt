package app.drinkin.web.common
import app.drinkin.web.*
import app.drinkin.web.feed.*
import app.drinkin.web.group.*
import app.drinkin.web.chat.*
import app.drinkin.web.saved.*
import app.drinkin.web.profile.*


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
import androidx.compose.ui.input.key.*
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
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image


@Composable
fun DashboardTopBar(
    apiClient: DrinkinApiClient,
    currentTab: DashboardTab,
    viewingOtherUserId: String?,
    pendingCount: Int,
    unreadMessagesCount: Int,
    onTabChange: (DashboardTab) -> Unit,
    onLogout: () -> Unit,
    onUserSelect: (String) -> Unit
) {
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
                    modifier = Modifier.width(220.dp).heightIn(min = 40.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )

                if (searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .padding(top = 52.dp),
                        elevation = 8.dp,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            searchResults.take(5).forEach { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUserSelect(result.id)
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
                val color = if (isSelected) DrinkinAccentBlue  else DrinkinMutedGray
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable {
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

@Composable
fun LeftSidebarProfileCard(
    myProfileState: UserProfile?,
    activeConnectionsSize: Int,
    userAboutText: String
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
                Text("$activeConnectionsSize", color = DrinkinAccentBlue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text("About Me", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold, color = DrinkinTextBlack, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(4.dp))
            Text(if (userAboutText.isBlank()) "No bio provided." else userAboutText, fontSize = 12.sp, color = DrinkinMutedGray, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.Start))
        }
    }
}

@Composable
fun RightSidebarSavedCard(
    savedPosts: List<Post>,
    onTabChange: (DashboardTab) -> Unit
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

@Composable
fun StartPostModal(
    apiClient: DrinkinApiClient,
    onDismiss: () -> Unit,
    onForceFeedRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    TextButton(onClick = onDismiss) {
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
                                    onDismiss()
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

@Composable
fun DashboardLayout(
    apiClient: DrinkinApiClient,
    currentTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    viewingOtherUserId: String?,
    pendingCount: Int,
    unreadMessagesCount: Int,
    onLogout: () -> Unit,
    onUserSelect: (String) -> Unit,
    myProfileState: UserProfile?,
    activeConnections: List<UserProfile>,
    userAboutText: String,
    errorMsg: String?,
    onErrorMsgClose: () -> Unit,
    isOtherProfileLoading: Boolean,
    otherUserProfile: UserProfile?,
    otherUserPosts: List<Post>,
    isOtherUserPostsLoading: Boolean,
    onConnectClick: () -> Unit,
    onAcceptConnectionClick: () -> Unit,
    onRejectConnectionClick: () -> Unit,
    onMessageClick: () -> Unit,
    onCloseOtherProfileClick: () -> Unit,
    onOtherProfileLikeToggle: (Post, String) -> Unit,
    posts: List<Post>,
    hasMore: Boolean,
    isPaginationLoading: Boolean,
    isLoading: Boolean,
    likedPostIds: Set<String>,
    likeCountOffsets: Map<String, Int>,
    savedPosts: MutableList<Post>,
    onStartPostClick: () -> Unit,
    loadNextPage: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onLikeToggle: (Post, String) -> Unit,
    onSaveToggle: (Post) -> Unit,
    pendingConnections: List<PendingConnectionRequest>,
    onAcceptGroupConnection: (PendingConnectionRequest) -> Unit,
    onRejectGroupConnection: (PendingConnectionRequest) -> Unit,
    onGroupConnectionClick: (UserProfile) -> Unit,
    realConversations: List<Conversation>,
    selectedConversationId: String?,
    realMessages: List<Message>,
    onSelectConversation: (String) -> Unit,
    onStartConversation: (UserProfile) -> Unit,
    onSendMessage: (Conversation, String) -> Unit,
    onQuickGreetingClick: (Conversation, String) -> Unit,
    onUpdateProfile: (String, String, String, List<String>) -> Unit
) {
    Scaffold(
        topBar = {
            DashboardTopBar(
                apiClient = apiClient,
                currentTab = currentTab,
                viewingOtherUserId = viewingOtherUserId,
                pendingCount = pendingCount,
                unreadMessagesCount = unreadMessagesCount,
                onTabChange = onTabChange,
                onLogout = onLogout,
                onUserSelect = onUserSelect
            )
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
                    LeftSidebarProfileCard(
                        myProfileState = myProfileState,
                        activeConnectionsSize = activeConnections.size,
                        userAboutText = userAboutText
                    )
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
                                Text(errorMsg, color = Color(0xFFC53030), fontSize = 13.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = onErrorMsgClose, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFC53030), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (viewingOtherUserId != null) {
                        if (isOtherProfileLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = DrinkinAccentBlue)
                        } else otherUserProfile?.let { profile ->
                            val isMe = profile.id == (myProfileState?.id ?: "")
                            OtherUserProfileContent(
                                profile = profile,
                                isMe = isMe,
                                otherUserPosts = otherUserPosts,
                                isOtherUserPostsLoading = isOtherUserPostsLoading,
                                onConnectClick = onConnectClick,
                                onAcceptConnection = onAcceptConnectionClick,
                                onRejectConnection = onRejectConnectionClick,
                                onMessageClick = onMessageClick,
                                onCloseClick = onCloseOtherProfileClick,
                                onLikeToggle = onOtherProfileLikeToggle,
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
                                    onStartPostClick = onStartPostClick,
                                    loadNextPage = loadNextPage,
                                    onAuthorClick = onAuthorClick,
                                    onLikeToggle = onLikeToggle,
                                    onSaveToggle = onSaveToggle,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            DashboardTab.MY_GROUP -> {
                                GroupTabContent(
                                    pendingConnections = pendingConnections,
                                    activeConnections = activeConnections,
                                    onAcceptConnection = onAcceptGroupConnection,
                                    onRejectConnection = onRejectGroupConnection,
                                    onConnectionClick = onGroupConnectionClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            DashboardTab.CHAT -> {
                                ChatTabContent(
                                    activeConnections = activeConnections,
                                    realConversations = realConversations,
                                    selectedConversationId = selectedConversationId,
                                    realMessages = realMessages,
                                    onSelectConversation = onSelectConversation,
                                    onStartConversation = onStartConversation,
                                    onSendMessage = onSendMessage,
                                    onQuickGreetingClick = onQuickGreetingClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            DashboardTab.SAVED -> {
                                SavedTabContent(
                                    savedPosts = savedPosts,
                                    onAuthorClick = onAuthorClick,
                                    onSaveToggle = onSaveToggle,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            DashboardTab.PROFILE -> {
                                ProfileTabContent(
                                    myProfileState = myProfileState,
                                    onUpdateProfile = onUpdateProfile,
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
                    RightSidebarSavedCard(
                        savedPosts = savedPosts,
                        onTabChange = onTabChange
                    )
                }
            }
        }
    }
}