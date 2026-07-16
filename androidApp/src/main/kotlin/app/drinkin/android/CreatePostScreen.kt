package app.drinkin.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.drinkin.shared.api.DrinkinApiClient
import app.drinkin.shared.model.CreatePostRequest
import app.drinkin.shared.model.DrinkCategory
import kotlinx.coroutines.launch

@Composable
fun CreatePostScreen(
    apiClient: DrinkinApiClient,
    onBackToFeed: () -> Unit,
    onPostCreated: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var drinkCategory by remember { mutableStateOf(DrinkCategory.ALCOHOLIC) }
    var drinkType by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf<Int?>(null) }
    var tastingNotes by remember { mutableStateOf("") }
    var scenario by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share a Drink Experience") },
                navigationIcon = {
                    TextButton(onClick = onBackToFeed) {
                        Text("< Back", color = MaterialTheme.colors.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Text Body
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    errorMsg = null
                },
                label = { Text("What did you drink? Share your review...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                minLines = 3
            )

            // Drink Category Selection
            Column {
                Text("Drink Category", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = drinkCategory == DrinkCategory.ALCOHOLIC,
                        onClick = { drinkCategory = DrinkCategory.ALCOHOLIC }
                    )
                    Text(
                        "Alcoholic",
                        modifier = Modifier.clickable { drinkCategory = DrinkCategory.ALCOHOLIC }
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    RadioButton(
                        selected = drinkCategory == DrinkCategory.NON_ALCOHOLIC,
                        onClick = { drinkCategory = DrinkCategory.NON_ALCOHOLIC }
                    )
                    Text(
                        "Non-Alcoholic",
                        modifier = Modifier.clickable { drinkCategory = DrinkCategory.NON_ALCOHOLIC }
                    )
                }
            }

            // Optional Drink Type
            TextField(
                value = drinkType,
                onValueChange = { drinkType = it },
                label = { Text("Drink Type (e.g. IPA, Espresso, Merlot) - Optional") },
                modifier = Modifier.fillMaxWidth()
            )

            // Optional Rating Selector (1 to 5 Stars)
            Column {
                Text("Rating (Optional)", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (rating != null && rating!! >= i) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = "Star $i",
                            tint = if (rating != null && rating!! >= i) MaterialTheme.colors.primary else LocalContentColor.current.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    rating = if (rating == i) null else i
                                }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (rating != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { rating = null }) {
                            Text("Clear")
                        }
                    }
                }
            }

            // Optional Tasting Notes
            TextField(
                value = tastingNotes,
                onValueChange = { tastingNotes = it },
                label = { Text("Tasting Notes (e.g. Citrus, Oaky, Sweet) - Optional") },
                modifier = Modifier.fillMaxWidth()
            )

            // Optional Scenario Tag
            TextField(
                value = scenario,
                onValueChange = { scenario = it },
                label = { Text("Scenario Tag (e.g. Brewery Tour, Beach, Picnic) - Optional") },
                modifier = Modifier.fillMaxWidth()
            )

            errorMsg?.let {
                Text(it, color = MaterialTheme.colors.error)
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                    onClick = {
                        // Form Validations
                        if (text.trim().isEmpty()) {
                            errorMsg = "Review text cannot be empty."
                            return@Button
                        }
                        if (text.length > 1000) {
                            errorMsg = "Review text must be under 1000 characters."
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                apiClient.createPost(
                                    CreatePostRequest(
                                        text = text,
                                        drinkCategory = drinkCategory,
                                        drinkType = drinkType.takeIf { it.isNotBlank() },
                                        rating = rating,
                                        tastingNotes = tastingNotes.takeIf { it.isNotBlank() },
                                        scenario = scenario.takeIf { it.isNotBlank() }
                                    )
                                )
                                onPostCreated()
                            } catch (e: Exception) {
                                errorMsg = "Failed to create post. Please try again."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = text.isNotBlank()
                ) {
                    Text("Share Review")
                }
            }
        }
    }
}
