package app.drinkin.web.auth

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
fun WebLoginScreen(
    apiClient: DrinkinApiClient,
    onNavigateToRegister: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val performLogin = {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (!isValidEmail(trimmedEmail)) {
            errorMsg = "Please enter a valid email address."
        } else if (trimmedPassword.length < 8) {
            errorMsg = "Password must be at least 8 characters long."
        } else {
            isLoading = true
            coroutineScope.launch {
                try {
                    val response = apiClient.login(LoginRequest(email = trimmedEmail, password = trimmedPassword))
                    apiClient.setAuthToken(response.token)
                    onAuthSuccess(response.token)
                } catch (e: Exception) {
                    errorMsg = getErrorMessage(e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DrinkinLightGray),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 6.dp,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Drinkin", style = MaterialTheme.typography.h5, color = DrinkinAccentBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(DrinkinAccentBlue, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Text("Welcome to your professional drink network", style = MaterialTheme.typography.body2, color = DrinkinMutedGray)

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it.trim()
                        errorMsg = null
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it.trim()
                        errorMsg = null
                    },
                    label = { Text("Password (8+ characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
                                performLogin()
                            }
                        }
                    )
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                }

                if (isLoading) {
                    CircularProgressIndicator(color = DrinkinAccentBlue)
                } else {
                    Button(
                        onClick = { performLogin() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DrinkinAccentBlue),
                        shape = RoundedCornerShape(24.dp),
                        enabled = email.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text("Join now", color = DrinkinAccentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WebRegisterScreen(
    apiClient: DrinkinApiClient,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val performRegister = {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedDob = dob.trim()

        if (!isValidEmail(trimmedEmail)) {
            errorMsg = "Please enter a valid email address."
        } else if (trimmedUsername.length !in 3..50) {
            errorMsg = "Username must be between 3 and 50 characters."
        } else if (trimmedPassword.length < 8) {
            errorMsg = "Password must be at least 8 characters long."
        } else {
            val dobRegex = "^\\d{4}-\\d{2}-\\d{2}\$".toRegex()
            if (!dobRegex.matches(trimmedDob)) {
                errorMsg = "Date of Birth must be in YYYY-MM-DD format."
            } else if (isUnderage(trimmedDob)) {
                errorMsg = "Registration failed: Must be at least 18 years old."
            } else {
                isLoading = true
                coroutineScope.launch {
                    try {
                        val response = apiClient.register(
                            RegisterRequest(
                                email = trimmedEmail,
                                username = trimmedUsername,
                                password = trimmedPassword,
                                dateOfBirth = trimmedDob
                            )
                        )
                        apiClient.setAuthToken(response.token)
                        onAuthSuccess(response.token)
                    } catch (e: Exception) {
                        errorMsg = getErrorMessage(e)
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DrinkinLightGray),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 6.dp,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Drinkin", style = MaterialTheme.typography.h5, color = DrinkinAccentBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(DrinkinAccentBlue, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Text("Join the professional drink network", style = MaterialTheme.typography.body2, color = DrinkinMutedGray)

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it.trim()
                        errorMsg = null
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.trim()
                        errorMsg = null
                    },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it.trim()
                        errorMsg = null
                    },
                    label = { Text("Password (8+ chars)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                OutlinedTextField(
                    value = dob,
                    onValueChange = {
                        dob = it.trim()
                        errorMsg = null
                    },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (email.isNotBlank() && username.isNotBlank() && password.isNotBlank() && dob.isNotBlank() && !isLoading) {
                                performRegister()
                            }
                        }
                    )
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                }

                if (isLoading) {
                    CircularProgressIndicator(color = DrinkinAccentBlue)
                } else {
                    Button(
                        onClick = { performRegister() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DrinkinAccentBlue),
                        shape = RoundedCornerShape(24.dp),
                        enabled = email.isNotBlank() && username.isNotBlank() && password.isNotBlank() && dob.isNotBlank()
                    ) {
                        Text("Agree & Join", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text("Already on Drinkin? Sign in", color = DrinkinAccentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}