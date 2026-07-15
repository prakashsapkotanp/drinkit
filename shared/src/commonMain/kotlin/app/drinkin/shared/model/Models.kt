package app.drinkin.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class DrinkCategory { ALCOHOLIC, NON_ALCOHOLIC }

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val drinkPreferences: List<String> = emptyList(),
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: String? = null
)

@Serializable
data class Post(
    val id: String,
    val author: UserProfile,
    val text: String,
    val drinkCategory: DrinkCategory,
    val drinkType: String? = null,
    val rating: Int? = null,
    val tastingNotes: String? = null,
    val scenario: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: String
)

@Serializable
data class Comment(
    val id: String,
    val postId: String,
    val author: UserProfile,
    val text: String,
    val createdAt: String
)

@Serializable
data class PostPage(
    val items: List<Post>,
    val nextCursor: String? = null
)

@Serializable
data class CommentPage(
    val items: List<Comment>,
    val nextCursor: String? = null
)

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val dateOfBirth: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val username: String
)

@Serializable
data class CreatePostRequest(
    val text: String,
    val drinkCategory: DrinkCategory,
    val drinkType: String? = null,
    val rating: Int? = null,
    val tastingNotes: String? = null,
    val scenario: String? = null,
    val mediaUrls: List<String> = emptyList()
)
