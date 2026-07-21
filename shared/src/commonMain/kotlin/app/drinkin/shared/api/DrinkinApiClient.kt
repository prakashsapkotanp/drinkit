package app.drinkin.shared.api

import app.drinkin.shared.model.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Shared API client consumed by both the Android app and the Web (Wasm) app.
 * Holds the JWT token in memory; caller is responsible for persisting it
 * (EncryptedSharedPreferences on Android, secure storage strategy on Web).
 */
class DrinkinApiClient(
    private val baseUrl: String,
    private var authToken: String? = null
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private fun HttpRequestBuilder.withAuth() {
        authToken?.let { header("Authorization", "Bearer $it") }
    }

    suspend fun register(request: RegisterRequest): AuthResponse =
        client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun uploadMedia(fileName: String, fileBytes: ByteArray): UploadResponse =
        client.submitFormWithBinaryData(
            url = "$baseUrl/media/upload",
            formData = io.ktor.client.request.forms.formData {
                append("file", fileBytes, io.ktor.http.Headers.build {
                    append(io.ktor.http.HttpHeaders.ContentType, "image/jpeg")
                    append(io.ktor.http.HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        ) {
            withAuth()
        }.body()

    suspend fun login(request: LoginRequest): AuthResponse =
        client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getFeed(cursor: String? = null): PostPage =
        client.get("$baseUrl/feed") {
            withAuth()
            cursor?.let { parameter("cursor", it) }
        }.body()

    suspend fun createPost(request: CreatePostRequest): Post =
        client.post("$baseUrl/posts") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun likePost(postId: String, reactionType: String = "LIKE") {
        client.post("$baseUrl/posts/$postId/reactions") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody(ReactRequest(reactionType = reactionType))
        }
    }

    suspend fun unlikePost(postId: String) {
        client.delete("$baseUrl/posts/$postId/reactions") { withAuth() }
    }

    suspend fun followUser(userId: String) {
        client.post("$baseUrl/users/$userId/follow") { withAuth() }
    }

    suspend fun getUserProfile(userId: String): UserProfile =
        client.get("$baseUrl/users/$userId") { withAuth() }.body()

    suspend fun getUserPosts(userId: String, cursor: String? = null): PostPage =
        client.get("$baseUrl/users/$userId/posts") {
            withAuth()
            cursor?.let { parameter("cursor", it) }
        }.body()

    suspend fun updateProfile(request: UpdateProfileRequest): UserProfile =
        client.put("$baseUrl/users/me") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun sendConnectionRequest(request: ConnectionRequest): ConnectionRecord =
        client.post("$baseUrl/connections") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getPendingRequests(cursor: String? = null): PendingConnectionRequestPage =
        client.get("$baseUrl/connections/requests") {
            withAuth()
            cursor?.let { parameter("cursor", it) }
        }.body()

    suspend fun acceptConnection(connectionId: String) {
        client.put("$baseUrl/connections/$connectionId/accept") {
            withAuth()
        }
    }

    suspend fun rejectConnection(connectionId: String) {
        client.put("$baseUrl/connections/$connectionId/reject") {
            withAuth()
        }
    }

    suspend fun getConnections(cursor: String? = null): UserPage =
        client.get("$baseUrl/connections") {
            withAuth()
            cursor?.let { parameter("cursor", it) }
        }.body()

    suspend fun removeConnection(connectionId: String) {
        client.delete("$baseUrl/connections/$connectionId") {
            withAuth()
        }
    }

    suspend fun searchUsers(query: String, cursor: String? = null): UserPage =
        client.get("$baseUrl/users/search") {
            withAuth()
            parameter("q", query)
            cursor?.let { parameter("cursor", it) }
        }.body()

    suspend fun getOrCreateConversation(request: ConversationRequest): Conversation =
        client.post("$baseUrl/conversations") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getConversations(cursor: String? = null): ConversationPage =
        client.get("$baseUrl/conversations") {
            withAuth()
            cursor?.let { parameter("cursor", it) }
        }.body()

    suspend fun getMessages(conversationId: String, cursor: String? = null): MessagePage =
        client.get("$baseUrl/conversations/$conversationId/messages") {
            withAuth()
            cursor?.let { parameter("cursor", it) }
        }.body()

    suspend fun sendMessage(conversationId: String, request: MessageRequest): Message =
        client.post("$baseUrl/conversations/$conversationId/messages") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
