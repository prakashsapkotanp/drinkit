package app.drinkin.post

import app.drinkin.shared.model.DrinkCategory
import app.drinkin.shared.model.Post
import app.drinkin.shared.model.PostPage
import app.drinkin.shared.model.UserProfile
import app.drinkin.user.FollowRepository
import app.drinkin.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/feed")
class FeedController(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository
) {

    private fun getCurrentUserId(): UUID? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        val principal = auth.principal as? String ?: return null
        return try {
            UUID.fromString(principal)
        } catch (e: Exception) {
            null
        }
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

    @GetMapping
    fun getFeed(
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<Any> {
        val userId = getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized"))

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        val followedIds = followRepository.findFollowingIds(userId).toMutableList()
        val authorIds = followedIds.apply { add(userId) }

        val parsedCursor = cursor?.let {
            try {
                OffsetDateTime.parse(it)
            } catch (e: Exception) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid cursor format"))
            }
        }

        val pageLimit = 10
        val pageable = PageRequest.of(0, pageLimit + 1)
        val posts = if (authorIds.size > 1) {
            postRepository.findFeedPosts(authorIds, parsedCursor, pageable)
        } else {
            postRepository.findAllPosts(parsedCursor, pageable)
        }

        val filteredPosts = if (!user.ageVerified) {
            posts.filter { it.drinkCategory != DrinkCategory.ALCOHOLIC }
        } else {
            posts
        }

        val hasNext = filteredPosts.size > pageLimit
        val items = if (hasNext) filteredPosts.subList(0, pageLimit) else filteredPosts
        val nextCursor = if (hasNext) items.last().createdAt.toString() else null

        val responsePage = PostPage(
            items = items.map { mapToPostDto(it) },
            nextCursor = nextCursor
        )

        return ResponseEntity.ok(responsePage)
    }
}
