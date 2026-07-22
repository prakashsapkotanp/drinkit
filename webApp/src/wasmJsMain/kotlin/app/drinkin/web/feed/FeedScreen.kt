package app.drinkin.web.feed

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
import app.drinkin.web.common.*
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

@Composable
fun FeedTabContent(
    posts: List<Post>,
    hasMore: Boolean,
    isPaginationLoading: Boolean,
    isLoading: Boolean,
    likedPostIds: Set<String>,
    likeCountOffsets: Map<String, Int>,
    savedPosts: List<Post>,
    onStartPostClick: () -> Unit,
    loadNextPage: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onLikeToggle: (Post, String) -> Unit,
    onSaveToggle: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                            .clickable { onStartPostClick() }
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
                            onAuthorClick(post.author.id)
                        },
                        onLikeToggle = { reactionType ->
                            onLikeToggle(post, reactionType)
                        },
                        isSaved = isSaved,
                        onSaveToggle = {
                            onSaveToggle(post)
                        }
                    )
                }
            }
        }
    }
}