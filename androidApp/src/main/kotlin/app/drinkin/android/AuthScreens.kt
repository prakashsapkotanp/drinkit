package app.drinkin.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.LoginRequest
import app.drinkin.shared.model.RegisterRequest
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import java.time.LocalDate
import java.time.Period

private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

fun isValidEmail(email: String): Boolean = emailRegex.matches(email)

@SuppressLint("NewApi")
fun isUnderage(dobString: String): Boolean {
    return try {
        val dob = LocalDate.parse(dobString)
        val age = Period.between(dob, LocalDate.now()).years
        age < 18
    } catch (e: Exception) {
        true // Assume invalid dates or errors are not allowed (so treat as underage/invalid)
    }
}

@Composable
fun LoginScreen(
    apiClient: DrinkinApiClient,
    tokenManager: TokenManager,
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
                    tokenManager.saveToken(response.token)
                    apiClient.setAuthToken(response.token)
                    onAuthSuccess(response.token)
                } catch (e: ResponseException) {
                    errorMsg = when (e.response.status.value) {
                        401 -> "Invalid credentials. Email or password is incorrect."
                        409 -> "Email already registered."
                        400 -> "Bad request. Please verify inputs."
                        else -> "Server error (${e.response.status.value}). Please try again."
                    }
                } catch (e: Exception) {
                    errorMsg = "Network failure. Please check your connection and try again."
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        "Drinkin",
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colors.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "In",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    "Welcome to your drink experience network",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMsg = null
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
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
                        password = it
                        errorMsg = null
                    },
                    label = { Text("Password (8+ characters)") },
                    modifier = Modifier.fillMaxWidth(),
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
                    CircularProgressIndicator(color = MaterialTheme.colors.primary)
                } else {
                    Button(
                        onClick = { performLogin() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                        shape = RoundedCornerShape(24.dp),
                        enabled = email.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text("Join now", color = MaterialTheme.colors.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    apiClient: DrinkinApiClient,
    tokenManager: TokenManager,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") } // YYYY-MM-DD format

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val performRegister = {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedDob = dob.trim()

        // Client-side validations
        if (!isValidEmail(trimmedEmail)) {
            errorMsg = "Please enter a valid email address."
        } else if (trimmedUsername.length !in 3..50) {
            errorMsg = "Username must be between 3 and 50 characters."
        } else if (trimmedPassword.length < 8) {
            errorMsg = "Password must be at least 8 characters long."
        } else {
            // Validate Date format
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
                        tokenManager.saveToken(response.token)
                        apiClient.setAuthToken(response.token)
                        onAuthSuccess(response.token)
                    } catch (e: ResponseException) {
                        errorMsg = when (e.response.status.value) {
                            401 -> "Invalid credentials."
                            409 -> "Email or username already in use."
                            400 -> "Registration failed: Must be at least 18 years old."
                            else -> "Server error (${e.response.status.value}). Please try again."
                        }
                    } catch (e: Exception) {
                        errorMsg = "Network failure. Please check your connection and try again."
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
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        "Drinkin",
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colors.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "In",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    "Join the professional drink network",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMsg = null
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
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
                        username = it
                        errorMsg = null
                    },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
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
                        password = it
                        errorMsg = null
                    },
                    label = { Text("Password (8+ chars)") },
                    modifier = Modifier.fillMaxWidth(),
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
                        dob = it
                        errorMsg = null
                    },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
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
                    CircularProgressIndicator(color = MaterialTheme.colors.primary)
                } else {
                    Button(
                        onClick = { performRegister() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                        shape = RoundedCornerShape(24.dp),
                        enabled = email.isNotBlank() && username.isNotBlank() && password.isNotBlank() && dob.isNotBlank()
                    ) {
                        Text("Agree & Join", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text("Already on DrinkinIn? Sign in", color = MaterialTheme.colors.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
