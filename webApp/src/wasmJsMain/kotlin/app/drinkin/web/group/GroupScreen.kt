package app.drinkin.web.group

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
fun GroupTabContent(
    pendingConnections: List<PendingConnectionRequest>,
    activeConnections: List<UserProfile>,
    onAcceptConnection: (PendingConnectionRequest) -> Unit,
    onRejectConnection: (PendingConnectionRequest) -> Unit,
    onConnectionClick: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("My Network Connections", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = DrinkinTextBlack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Pending Requests (${pendingConnections.size})", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                pendingConnections.forEach { req ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.requester.displayName ?: req.requester.username, fontWeight = FontWeight.Bold)
                            Text("@${req.requester.username}", color = DrinkinMutedGray, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAcceptConnection(req) },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                            ) {
                                Text("Accept", color = Color.White)
                            }

                            Button(
                                onClick = { onRejectConnection(req) },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
                            ) {
                                Text("Reject", color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("My Connections (${activeConnections.size})", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                activeConnections.forEach { conn ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnectionClick(conn) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(DrinkinAccentBlue.copy(alpha = 0.1f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = DrinkinAccentBlue)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(conn.displayName ?: conn.username, fontWeight = FontWeight.Bold)
                            Text("@${conn.username}", color = DrinkinMutedGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}