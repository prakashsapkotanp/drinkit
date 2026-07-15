package app.drinkin.user

import app.drinkin.shared.model.UserProfile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val drinkPreferences: List<String>? = null
)

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository
) {

    private fun getCurrentUserId(): UUID {
        val principal = SecurityContextHolder.getContext().authentication.principal as String
        return UUID.fromString(principal)
    }

    private fun getUserProfile(user: UserEntity): UserProfile {
        val followerCount = followRepository.countByIdFollowingId(user.id)
        val followingCount = followRepository.countByIdFollowerId(user.id)
        return UserProfile(
            id = user.id.toString(),
            username = user.username,
            displayName = user.displayName,
            bio = user.bio,
            avatarUrl = user.avatarUrl,
            drinkPreferences = user.drinkPreferences.toList(),
            followerCount = followerCount,
            followingCount = followingCount,
            createdAt = user.createdAt.toString()
        )
    }

    @GetMapping("/me")
    fun getMe(): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))
        return ResponseEntity.ok(getUserProfile(user))
    }

    @PutMapping("/me")
    fun updateMe(@RequestBody req: UpdateProfileRequest): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        if (req.displayName != null) {
            if (req.displayName.length > 100) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Display name is too long (max 100 characters)"))
            }
            user.displayName = req.displayName
        }

        if (req.bio != null) {
            if (req.bio.length > 500) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Bio is too long (max 500 characters)"))
            }
            user.bio = req.bio
        }

        if (req.avatarUrl != null) {
            user.avatarUrl = req.avatarUrl
        }

        if (req.drinkPreferences != null) {
            for (pref in req.drinkPreferences) {
                if (pref.length > 50) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Drink preference '$pref' is too long (max 50 characters)"))
                }
            }
            user.drinkPreferences = req.drinkPreferences.toTypedArray()
        }

        userRepository.save(user)
        return ResponseEntity.ok(getUserProfile(user))
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): ResponseEntity<Any> {
        val user = userRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))
        return ResponseEntity.ok(getUserProfile(user))
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
