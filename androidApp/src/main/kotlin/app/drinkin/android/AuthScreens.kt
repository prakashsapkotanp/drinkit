package app.drinkin.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Login to Drinkin'") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
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
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMsg = null
                },
                label = { Text("Password") },
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
            Spacer(modifier = Modifier.height(16.dp))

            errorMsg?.let {
                Text(it, color = MaterialTheme.colors.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { performLogin() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank() && password.isNotBlank()
                ) {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("Don't have an account? Register here")
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create Drinkin' Account") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
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
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMsg = null
                },
                label = { Text("Username (3-50 chars)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
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
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
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
            Spacer(modifier = Modifier.height(16.dp))

            errorMsg?.let {
                Text(it, color = MaterialTheme.colors.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { performRegister() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank() && username.isNotBlank() && password.isNotBlank() && dob.isNotBlank()
                ) {
                    Text("Register")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Login here")
            }
        }
    }
}
