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
    private val followRepository: FollowRepository,
    private val connectionRepository: app.drinkin.user.ConnectionRepository
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
            reactionCounts = entity.reactionCounts,
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

        val poolSize = 50
        val pageLimit = 10
        val pageable = PageRequest.of(0, poolSize)
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

        val scoredPosts = filteredPosts.map { post ->
            var score = 0.0

            // 1. Connection priority (+100.0 if author is a connection)
            if (connectionRepository.areConnected(userId, post.author.id)) {
                score += 100.0
            }

            // 2. Drink preferences similarity
            user.drinkPreferences.forEach { pref ->
                if (pref.isNotBlank()) {
                    val p = pref.trim().lowercase()
                    if (post.drinkType?.lowercase()?.contains(p) == true) {
                        score += 50.0
                    }
                    if (post.tastingNotes?.lowercase()?.contains(p) == true) {
                        score += 30.0
                    }
                    if (post.text.lowercase().contains(p)) {
                        score += 20.0
                    }
                }
            }

            // 3. Reactions focus (+10.0 per reaction)
            val totalReactions = post.reactionCounts.values.sum()
            score += totalReactions * 10.0

            post to score
        }

        val sortedScoredPosts = scoredPosts.sortedByDescending { it.second }.map { it.first }

        val hasNext = filteredPosts.size > pageLimit
        val items = if (hasNext) sortedScoredPosts.subList(0, pageLimit) else sortedScoredPosts
        val nextCursor = if (hasNext && posts.isNotEmpty()) posts.last().createdAt.toString() else null

        val responsePage = PostPage(
            items = items.map { mapToPostDto(it) },
            nextCursor = nextCursor
        )

        return ResponseEntity.ok(responsePage)
    }
}
