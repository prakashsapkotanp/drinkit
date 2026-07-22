package app.drinkin.web.chat

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
fun ChatTabContent(
    activeConnections: List<UserProfile>,
    realConversations: List<Conversation>,
    selectedConversationId: String?,
    realMessages: List<Message>,
    onSelectConversation: (String) -> Unit,
    onStartConversation: (UserProfile) -> Unit,
    onSendMessage: (Conversation, String) -> Unit,
    onQuickGreetingClick: (Conversation, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize().border(0.5.dp, DrinkinBorderColor, shape = RoundedCornerShape(8.dp)),
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

                var connectionQuery by remember { mutableStateOf("") }
                var connectionSearchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

                LaunchedEffect(connectionQuery) {
                    if (connectionQuery.isNotBlank()) {
                        connectionSearchResults = activeConnections.filter {
                            it.username.contains(connectionQuery, ignoreCase = true) ||
                            (it.displayName ?: "").contains(connectionQuery, ignoreCase = true)
                        }
                    } else {
                        connectionSearchResults = emptyList()
                    }
                }

                OutlinedTextField(
                    value = connectionQuery,
                    onValueChange = { connectionQuery = it },
                    placeholder = { Text("Search connections to message...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().padding(8.dp).height(44.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )

                if (connectionQuery.isNotBlank()) {
                    Text("Search Results", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = DrinkinMutedGray)
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(connectionSearchResults) { conn ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        connectionQuery = ""
                                        onStartConversation(conn)
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
                                    Text(conn.displayName ?: conn.username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("@${conn.username}", color = DrinkinMutedGray, fontSize = 11.sp)
                                }
                            }
                            Divider()
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(realConversations) { conv ->
                            val isSelected = selectedConversationId == conv.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) DrinkinLightGray else DrinkinCardBackground)
                                    .clickable {
                                        onSelectConversation(conv.id)
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

                    // Quick Greeting Options with Emojis
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quick greetings:", fontSize = 11.sp, color = DrinkinMutedGray)
                        val options = listOf(
                            "Hey \uD83D\uDC4B",
                            "Hello \uD83D\uDE0A",
                            "Hi! \u2615",
                            "Wave \uD83D\uDC4B"
                        )
                        options.forEach { option ->
                            Box(
                                modifier = Modifier
                                    .background(DrinkinAccentBlue.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp))
                                    .clickable {
                                        onQuickGreetingClick(activeConv, option)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(option, color = DrinkinAccentBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                    onSendMessage(activeConv, typed)
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