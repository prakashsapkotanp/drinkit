package app.drinkin.user

import app.drinkin.shared.model.UpdateProfileRequest
import app.drinkin.shared.model.UserProfile
import app.drinkin.shared.model.Post
import app.drinkin.shared.model.PostPage
import app.drinkin.post.PostRepository
import app.drinkin.post.PostEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val connectionRepository: ConnectionRepository,
    private val postRepository: PostRepository
) {

    private fun getCurrentUserId(): UUID {
        val principal = SecurityContextHolder.getContext().authentication.principal as String
        return UUID.fromString(principal)
    }

    private fun getCurrentUserIdOrNull(): UUID? {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || auth.principal == "anonymousUser") return null
        val principal = auth.principal as? String ?: return null
        return try {
            UUID.fromString(principal)
        } catch (e: Exception) {
            null
        }
    }

    private fun getUserProfile(user: UserEntity, currentUserId: UUID?): UserProfile {
        val followerCount = followRepository.countByIdFollowingId(user.id)
        val followingCount = followRepository.countByIdFollowerId(user.id)

        // Compute connection status relative to the current logged-in user
        val connectionStatus = if (currentUserId == null || currentUserId == user.id) {
            null
        } else {
            val conn = connectionRepository.findConnectionBetween(currentUserId, user.id)
            if (conn == null) {
                "NONE"
            } else if (conn.status == "ACCEPTED") {
                "CONNECTED"
            } else if (conn.status == "PENDING") {
                if (conn.requesterId == currentUserId) "PENDING_SENT" else "PENDING_RECEIVED"
            } else {
                "NONE"
            }
        }

        return UserProfile(
            id = user.id.toString(),
            username = user.username,
            displayName = user.displayName,
            bio = user.bio,
            avatarUrl = user.avatarUrl,
            drinkPreferences = user.drinkPreferences.toList(),
            followerCount = followerCount,
            followingCount = followingCount,
            createdAt = user.createdAt.toString(),
            connectionStatus = connectionStatus
        )
    }

    private fun mapToPostDto(entity: PostEntity): Post {
        val author = entity.author
        val followerCount = followRepository.countByIdFollowingId(author.id)
        val followingCount = followRepository.countByIdFollowerId(author.id)
        val authorProfile = UserProfile(
            id = author.id.toString(),
            username = author.username,
            displayName = author.displayName,
            bio = author.bio,
            avatarUrl = author.avatarUrl,
            drinkPreferences = author.drinkPreferences.toList(),
            followerCount = followerCount,
            followingCount = followingCount,
            createdAt = author.createdAt.toString()
        )
        val totalLikes = entity.reactionCounts.values.sum()

        return Post(
            id = entity.id.toString(),
            author = authorProfile,
            text = entity.text,
            drinkCategory = entity.drinkCategory,
            drinkType = entity.drinkType,
            rating = entity.rating,
            tastingNotes = entity.tastingNotes,
            scenario = entity.scenario,
            mediaUrls = entity.mediaUrls.toList(),
            likeCount = totalLikes,
            commentCount = entity.commentCount,
            createdAt = entity.createdAt.toString()
        )
    }

    @GetMapping("/me")
    fun getMe(): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))
        return ResponseEntity.ok(getUserProfile(user, userId))
    }

    @PutMapping("/me")
    fun updateMe(@RequestBody req: UpdateProfileRequest): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        val reqDisplayName = req.displayName
        if (reqDisplayName != null) {
            if (reqDisplayName.length > 100) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Display name is too long (max 100 characters)"))
            }
            user.displayName = reqDisplayName
        }

        val reqBio = req.bio
        if (reqBio != null) {
            if (reqBio.length > 500) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Bio is too long (max 500 characters)"))
            }
            user.bio = reqBio
        }

        val reqAvatarUrl = req.avatarUrl
        if (reqAvatarUrl != null) {
            user.avatarUrl = reqAvatarUrl
        }

        val reqDrinkPreferences = req.drinkPreferences
        if (reqDrinkPreferences != null) {
            for (pref in reqDrinkPreferences) {
                if (pref.length > 50) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Drink preference '$pref' is too long (max 50 characters)"))
                }
            }
            user.drinkPreferences = reqDrinkPreferences.toTypedArray()
        }

        userRepository.save(user)
        return ResponseEntity.ok(getUserProfile(user, userId))
    }

    @GetMapping("/search")
    fun searchUsers(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<Any> {
        if (q.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Query parameter 'q' is required and cannot be empty"))
        }

        val currentUserId = getCurrentUserIdOrNull()
        val pageLimit = 10
        val pageable = org.springframework.data.domain.PageRequest.of(0, pageLimit + 1)
        val users = userRepository.searchUsers(q, cursor, pageable)

        val hasNext = users.size > pageLimit
        val items = if (hasNext) users.subList(0, pageLimit) else users
        val nextCursor = if (hasNext) items.last().username else null

        val mappedItems = items.map { getUserProfile(it, currentUserId) }
        val userPage = app.drinkin.shared.model.UserPage(
            items = mappedItems,
            nextCursor = nextCursor
        )
        return ResponseEntity.ok(userPage)
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): ResponseEntity<Any> {
        val user = userRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))
        val currentUserId = getCurrentUserIdOrNull()
        return ResponseEntity.ok(getUserProfile(user, currentUserId))
    }

    @GetMapping("/{id}/posts")
    fun getUserPosts(
        @PathVariable id: UUID,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<Any> {
        val parsedCursor = cursor?.let {
            try {
                OffsetDateTime.parse(it)
            } catch (e: Exception) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid cursor format"))
            }
        }

        val pageLimit = 10
        val pageable = org.springframework.data.domain.PageRequest.of(0, pageLimit + 1)
        val posts = postRepository.findFeedPosts(listOf(id), parsedCursor, pageable)

        val hasNext = posts.size > pageLimit
        val items = if (hasNext) posts.subList(0, pageLimit) else posts
        val nextCursor = if (hasNext) items.last().createdAt.toString() else null

        val page = PostPage(
            items = items.map { mapToPostDto(it) },
            nextCursor = nextCursor
        )
        return ResponseEntity.ok(page)
    }

    @PostMapping("/{id}/follow")
    fun followUser(@PathVariable id: UUID): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        if (currentUserId == id) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Cannot follow yourself"))
        }

        val targetUserExists = userRepository.existsById(id)
        if (!targetUserExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User to follow not found"))
        }

        val followId = FollowId(followerId = currentUserId, followingId = id)
        if (followRepository.existsById(followId)) {
            return ResponseEntity.noContent().build()
        }

        val follow = FollowEntity(id = followId)
        followRepository.save(follow)

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/follow")
    fun unfollowUser(@PathVariable id: UUID): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        if (currentUserId == id) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Cannot unfollow yourself"))
        }

        val followId = FollowId(followerId = currentUserId, followingId = id)
        if (followRepository.existsById(followId)) {
            followRepository.deleteById(followId)
        }

        return ResponseEntity.noContent().build()
    }
}
