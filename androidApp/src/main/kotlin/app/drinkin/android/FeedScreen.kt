package app.drinkin.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.Post
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
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

    // Locally track liked post IDs and optimistic like count offsets
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
                // Fail silently on pagination but keep hasMore true so user can retry scroll
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
                title = { Text("Drinkin' Feed") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colors.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreatePost) {
                Text("+", style = MaterialTheme.typography.h4)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    Button(onClick = { loadInitialFeed() }) {
                        Text("Retry")
                    }
                }
            } else if (posts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "It looks like your feed is empty!",
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Find people to follow to see their drink posts here.",
                        style = MaterialTheme.typography.body2
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { loadInitialFeed() }) {
                        Text("Refresh Feed")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(posts) { index, post ->
                        // Infinite scroll trigger
                        if (index >= posts.size - 2 && hasMore && !isPaginationLoading) {
                            loadNextPage()
                        }

                        val isLiked = likedPostIds.contains(post.id)
                        val offset = likeCountOffsets[post.id] ?: 0
                        val displayedLikeCount = post.likeCount + offset

                        PostCard(
                            post = post,
                            isLiked = isLiked,
                            likeCount = displayedLikeCount,
                            onLikeToggle = {
                                coroutineScope.launch {
                                    val newIsLiked = !isLiked
                                    // Optimistic UI updates
                                    likedPostIds = if (newIsLiked) {
                                        likedPostIds + post.id
                                    } else {
                                        likedPostIds - post.id
                                    }
                                    likeCountOffsets = likeCountOffsets + (post.id to (if (newIsLiked) offset + 1 else offset - 1))

                                    try {
                                        if (newIsLiked) {
                                            apiClient.likePost(post.id)
                                        } else {
                                            apiClient.unlikePost(post.id)
                                        }
                                    } catch (e: Exception) {
                                        // Revert on error
                                        likedPostIds = if (newIsLiked) {
                                            likedPostIds - post.id
                                        } else {
                                            likedPostIds + post.id
                                        }
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

@Composable
fun PostCard(
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
            // Header: Author details
            Text(
                text = post.author.displayName ?: post.author.username,
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = "@${post.author.username} • ${post.createdAt.take(10)}",
                style = MaterialTheme.typography.caption,
                color = LocalContentColor.current.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Body: Text and properties
            Text(text = post.text, style = MaterialTheme.typography.body1)
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

            // Tasting Notes & Scenario
            if (!post.tastingNotes.isNull_or_empty() || !post.scenario.isNull_or_empty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    if (!post.tastingNotes.isNull_or_empty()) {
                        Text(
                            "Notes: ${post.tastingNotes}",
                            style = MaterialTheme.typography.caption,
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                    if (!post.scenario.isNull_or_empty()) {
                        Text(
                            "Scenario: ${post.scenario}",
                            style = MaterialTheme.typography.caption,
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Likes and Comments count
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

// Inline helper for empty check on nullable strings
private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
