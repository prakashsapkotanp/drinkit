package app.drinkin.web.profile

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
fun ProfileTabContent(
    myProfileState: UserProfile?,
    onUpdateProfile: (String, String, String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var editDisplayName by remember(myProfileState) { mutableStateOf(myProfileState?.displayName ?: "") }
    var editBio by remember(myProfileState) { mutableStateOf(myProfileState?.bio ?: "") }
    var editAvatarUrl by remember(myProfileState) { mutableStateOf(myProfileState?.avatarUrl ?: "") }
    var editPrefs by remember(myProfileState) { mutableStateOf(myProfileState?.drinkPreferences?.joinToString(", ") ?: "") }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp
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
                    Text(if (editDisplayName.isNotBlank()) editDisplayName else (myProfileState?.username ?: "Professional User"), style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                    Text("@${myProfileState?.username ?: ""}", color = DrinkinMutedGray, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        if (isEditing) {
                            onUpdateProfile(
                                editDisplayName,
                                editBio,
                                editAvatarUrl,
                                editPrefs.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            )
                        }
                        isEditing = !isEditing
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isEditing) "Save Profile" else "Edit Info")
                }
            }

            Divider()

            if (isEditing) {
                OutlinedTextField(
                    value = editDisplayName,
                    onValueChange = { editDisplayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Headline / Bio") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPrefs,
                    onValueChange = { editPrefs = it },
                    label = { Text("Drink Preferences (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column {
                    Text("Headline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(editBio, color = DrinkinTextBlack, fontSize = 14.sp)
                }

                Column {
                    Text("Drink Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(editPrefs, color = DrinkinTextBlack, fontSize = 14.sp)
                }
            }
        }
    }
}