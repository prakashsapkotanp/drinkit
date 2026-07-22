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

            val adjustedReactions = post.reactionCounts.toMutableMap()
            val reactionOffset = likeCount - post.likeCount
            if (reactionOffset != 0) {
                val currentCount = adjustedReactions["LIKE"] ?: 0
                val newCount = currentCount + reactionOffset
                if (newCount > 0) {
                    adjustedReactions["LIKE"] = newCount
                } else {
                    adjustedReactions.remove("LIKE")
                }
            }
            val activeReactions = adjustedReactions.filter { it.value > 0 }
            if (activeReactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeReactions.forEach { (type, count) ->
                        val pair = when (type) {
                            "LIKE" -> Pair(Icons.Default.ThumbUp, DrinkinAccentBlue)
                            "LOVE" -> Pair(Icons.Default.Favorite, Color(0xFFE53E3E))
                            "CHEERS" -> Pair(Icons.Default.Star, Color(0xFFD69E2E))
                            "WOW" -> Pair(Icons.Default.Send, Color(0xFF319795))
                            "SAD" -> Pair(Icons.Default.Warning, Color(0xFF805AD5))
                            else -> Pair(Icons.Default.ThumbUp, DrinkinAccentBlue)
                        }
                        val (icon, color) = pair
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = type,
                                tint = color,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("$count", fontSize = 11.sp, color = DrinkinMutedGray)
                        }
                    }
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
                                    "LIKE" to Pair(Icons.Default.ThumbUp, DrinkinAccentBlue),
                                    "LOVE" to Pair(Icons.Default.Favorite, Color(0xFFE53E3E)),
                                    "CHEERS" to Pair(Icons.Default.Star, Color(0xFFD69E2E)),
                                    "WOW" to Pair(Icons.Default.Send, Color(0xFF319795)),
                                    "SAD" to Pair(Icons.Default.Warning, Color(0xFF805AD5))
                                )
                                reactions.forEach { (type, pair) ->
                                    val (icon, color) = pair
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = type,
                                        tint = color,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                onLikeToggle(type)
                                                showReactions = false
                                            }
                                            .padding(2.dp)
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
