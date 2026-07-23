package app.drinkin.web.common

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
import androidx.compose.ui.input.key.*
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
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image


// JS date helpers for wasmJs to get current local date components
private fun getYearFromJs(): Int = js("new Date().getFullYear()")
private fun getMonthFromJs(): Int = js("new Date().getMonth() + 1")
private fun getDayFromJs(): Int = js("new Date().getDate()")

suspend fun getErrorMessage(e: Exception): String {
    if (e is ResponseException) {
        try {
            val bodyText = e.response.body<String>()
            if (bodyText.contains("\"error\":")) {
                val start = bodyText.indexOf("\"error\":\"") + 9
                val end = bodyText.indexOf("\"", start)
                if (start in 9 until end) {
                    val msg = bodyText.substring(start, end)
                    if (msg.isNotBlank()) return msg
                }
            }
            return when (e.response.status.value) {
                401 -> "Invalid credentials."
                403 -> "Forbidden. Access is denied."
                404 -> "Not found."
                409 -> "Conflict occurred."
                else -> "Server error (${e.response.status.value})"
            }
        } catch (ex: Exception) {
            // ignore
        }
    }
    return e.message ?: "An unexpected error occurred."
}

private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

fun isValidEmail(email: String): Boolean = emailRegex.matches(email)

fun isUnderage(dobString: String): Boolean {
    return try {
        val parts = dobString.split("-")
        if (parts.size != 3) return true
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        val currentYear = getYearFromJs()
        val currentMonth = getMonthFromJs()
        val currentDay = getDayFromJs()

        var age = currentYear - year
        if (currentMonth < month || (currentMonth == month && currentDay < day)) {
            age--
        }
        age < 18
    } catch (e: Exception) {
        true
    }
}

fun updatePostReaction(post: Post, newReactionType: String?): Post {
    val oldReaction = post.myReaction
    val counts = post.reactionCounts.toMutableMap()

    if (oldReaction != null) {
        val currentCount = counts[oldReaction] ?: 0
        if (currentCount > 1) {
            counts[oldReaction] = currentCount - 1
        } else {
            counts.remove(oldReaction)
        }
    }

    if (newReactionType != null) {
        counts[newReactionType] = (counts[newReactionType] ?: 0) + 1
    }

    val totalLikes = counts.values.sum()

    return post.copy(
        myReaction = newReactionType,
        reactionCounts = counts,
        likeCount = totalLikes
    )
}

fun setBridgeToken(token: String): Unit =
    js(" { var b = document.getElementById('upload-bridge'); if (!b) { b = document.createElement('div'); b.id = 'upload-bridge'; b.style.display = 'none'; document.body.appendChild(b); } b.setAttribute('data-token', token); } ")

fun triggerUploadJs(): Unit =
    js("window.triggerImageUpload()")

fun getBridgeStatus(): String =
    js(" (document.getElementById('upload-bridge') ? document.getElementById('upload-bridge').getAttribute('data-status') || 'idle' : 'idle') ")

fun getBridgeUrl(): String =
    js(" (document.getElementById('upload-bridge') ? document.getElementById('upload-bridge').getAttribute('data-url') || '' : '') ")

fun getBridgeError(): String =
    js(" (document.getElementById('upload-bridge') ? document.getElementById('upload-bridge').getAttribute('data-error') || '' : '') ")
