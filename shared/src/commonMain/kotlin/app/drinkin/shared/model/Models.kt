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
    val createdAt: String? = null,
    val connectionStatus: String? = null // NONE / PENDING_SENT / PENDING_RECEIVED / CONNECTED
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

@Serializable
data class ReactRequest(
    val reactionType: String
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val drinkPreferences: List<String>? = null
)

@Serializable
data class ConnectionRequest(
    val addresseeId: String
)

@Serializable
data class ConnectionRecord(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PendingConnectionRequest(
    val id: String,
    val requester: UserProfile,
    val createdAt: String
)

@Serializable
data class PendingConnectionRequestPage(
    val items: List<PendingConnectionRequest>,
    val nextCursor: String? = null
)

@Serializable
data class UserPage(
    val items: List<UserProfile>,
    val nextCursor: String? = null
)

@Serializable
data class ConversationRequest(
    val otherUserId: String
)

@Serializable
data class Conversation(
    val id: String,
    val otherUser: UserProfile,
    val lastMessagePreview: String? = null,
    val lastMessageAt: String? = null
)

@Serializable
data class ConversationPage(
    val items: List<Conversation>,
    val nextCursor: String? = null
)

@Serializable
data class MessageRequest(
    val text: String
)

@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val read: Boolean,
    val createdAt: String
)

@Serializable
data class MessagePage(
    val items: List<Message>,
    val nextCursor: String? = null
)

@Serializable
data class UploadResponse(
    val url: String
)

@Serializable
data class UnreadCountsResponse(
    val pendingConnectionsCount: Int,
    val unreadMessagesCount: Int
)
