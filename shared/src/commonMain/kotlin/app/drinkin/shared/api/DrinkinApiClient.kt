package app.drinkin.shared.api

import app.drinkin.shared.model.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
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

    suspend fun likePost(postId: String) {
        client.post("$baseUrl/posts/$postId/reactions") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody(ReactRequest(reactionType = "LIKE"))
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
}
