package app.drinkin.post

import app.drinkin.shared.model.DrinkCategory
import app.drinkin.shared.model.Post
import app.drinkin.shared.model.UserProfile
import app.drinkin.user.FollowRepository
import app.drinkin.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

data class CreatePostRequest(
    val text: String? = null,
    val drinkCategory: DrinkCategory? = null,
    val drinkType: String? = null,
    val rating: Int? = null,
    val tastingNotes: String? = null,
    val scenario: String? = null,
    val mediaUrls: List<String>? = null
)

data class ReactRequest(
    val reactionType: String? = null
)

data class ReactionDto(
    val id: String,
    val user: UserProfile,
    val reactionType: String,
    val createdAt: String
)

data class ReactionPageDto(
    val items: List<ReactionDto>,
    val nextCursor: String?
)

data class CreateCommentRequest(
    val text: String? = null
)

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val reactionTypeRepository: ReactionTypeRepository,
    private val postReactionRepository: PostReactionRepository,
    private val reactionTransactionRepository: ReactionTransactionRepository,
    private val commentRepository: CommentRepository
) {

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

    private fun mapToCommentDto(entity: CommentEntity): app.drinkin.shared.model.Comment {
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
        return app.drinkin.shared.model.Comment(
            id = entity.id.toString(),
            postId = entity.postId.toString(),
            author = authorProfile,
            text = entity.text,
            createdAt = entity.createdAt.toString()
        )
    }

    @PostMapping
    fun createPost(@RequestBody req: CreatePostRequest): ResponseEntity<Any> {
        val userId = getCurrentUserIdOrNull()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized"))

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        if (req.text.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Text is required"))
        }
        if (req.text.length > 3000) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Text cannot exceed 3000 characters"))
        }
        if (req.drinkCategory == null) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Drink category is required"))
        }

        // DRK-102: Block alcohol posts for underage/non-ageVerified accounts
        if (req.drinkCategory == DrinkCategory.ALCOHOLIC && !user.ageVerified) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Underage or unverified accounts cannot create alcoholic posts"))
        }

        if (req.rating != null && (req.rating < 1 || req.rating > 5)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Rating must be between 1 and 5"))
        }

        val post = PostEntity(
            author = user,
            text = req.text,
            drinkCategory = req.drinkCategory,
            drinkType = req.drinkType,
            rating = req.rating,
            tastingNotes = req.tastingNotes,
            scenario = req.scenario,
            mediaUrls = req.mediaUrls?.toTypedArray() ?: emptyArray()
        )

        postRepository.save(post)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToPostDto(post))
    }

    @GetMapping("/{id}")
    fun getPost(@PathVariable id: UUID): ResponseEntity<Any> {
        val post = postRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Post not found"))

        // DRK-102: Block access to alcohol-tagged content for non-ageVerified accounts
        if (post.drinkCategory == DrinkCategory.ALCOHOLIC) {
            val userId = getCurrentUserIdOrNull()
            if (userId != null) {
                val user = userRepository.findById(userId).orElse(null)
                if (user == null || !user.ageVerified) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Access to alcoholic content is restricted"))
                }
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Access to alcoholic content requires age verification"))
            }
        }

        return ResponseEntity.ok(mapToPostDto(post))
    }

    private fun syncReactionCounts(post: PostEntity) {
        val counts = mutableMapOf<String, Int>()
        val types = reactionTypeRepository.findAll()
        for (type in types) {
            val count = postReactionRepository.countByPostIdAndReactionTypeId(post.id, type.id)
            if (count > 0) {
                counts[type.code] = count
            }
        }
        post.reactionCounts = counts
        postRepository.save(post)
    }

    @PostMapping("/{id}/reactions")
    @Transactional
    fun reactToPost(
        @PathVariable id: UUID,
        @RequestBody req: ReactRequest
    ): ResponseEntity<Any> {
        val userId = getCurrentUserIdOrNull()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized"))

        val post = postRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Post not found"))

        if (req.reactionType.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Reaction type is required"))
        }

        val reactionType = reactionTypeRepository.findByCode(req.reactionType)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid reaction type: ${req.reactionType}"))

        val existingReaction = postReactionRepository.findByPostIdAndUserId(id, userId)

        if (existingReaction == null) {
            val reaction = PostReactionEntity(
                postId = id,
                userId = userId,
                reactionTypeId = reactionType.id
            )
            postReactionRepository.save(reaction)

            val transaction = ReactionTransactionEntity(
                postId = id,
                userId = userId,
                action = "ADDED",
                reactionTypeId = reactionType.id,
                previousReactionTypeId = null
            )
            reactionTransactionRepository.save(transaction)
        } else {
            if (existingReaction.reactionTypeId != reactionType.id) {
                val previousTypeId = existingReaction.reactionTypeId
                existingReaction.reactionTypeId = reactionType.id
                existingReaction.updatedAt = OffsetDateTime.now()
                postReactionRepository.save(existingReaction)

                val transaction = ReactionTransactionEntity(
                    postId = id,
                    userId = userId,
                    action = "CHANGED",
                    reactionTypeId = reactionType.id,
                    previousReactionTypeId = previousTypeId
                )
                reactionTransactionRepository.save(transaction)
            }
        }

        syncReactionCounts(post)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/reactions")
    @Transactional
    fun removeReaction(@PathVariable id: UUID): ResponseEntity<Any> {
        val userId = getCurrentUserIdOrNull()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized"))

        val post = postRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Post not found"))

        val existingReaction = postReactionRepository.findByPostIdAndUserId(id, userId)
        if (existingReaction != null) {
            postReactionRepository.delete(existingReaction)

            val transaction = ReactionTransactionEntity(
                postId = id,
                userId = userId,
                action = "REMOVED",
                reactionTypeId = null,
                previousReactionTypeId = existingReaction.reactionTypeId
            )
            reactionTransactionRepository.save(transaction)

            syncReactionCounts(post)
        }

        return ResponseEntity.noContent().build()
    }

    private fun mapToReactionDto(entity: PostReactionEntity): ReactionDto {
        val user = userRepository.findById(entity.userId).orElse(null)
        val followerCount = followRepository.countByIdFollowingId(entity.userId)
        val followingCount = followRepository.countByIdFollowerId(entity.userId)
        val userProfile = UserProfile(
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
        val type = reactionTypeRepository.findById(entity.reactionTypeId).orElse(null)
        return ReactionDto(
            id = "${entity.postId}_${entity.userId}",
            user = userProfile,
            reactionType = type.code,
            createdAt = entity.createdAt.toString()
        )
    }

    @GetMapping("/{id}/reactions")
    fun getReactions(
        @PathVariable id: UUID,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<Any> {
        val postExists = postRepository.existsById(id)
        if (!postExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Post not found"))
        }

        val parsedCursor = cursor?.let {
            try {
                OffsetDateTime.parse(it)
            } catch (e: Exception) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid cursor format"))
            }
        }

        val pageLimit = 10
        val pageable = org.springframework.data.domain.PageRequest.of(0, pageLimit + 1)
        val reactions = postReactionRepository.findReactionsByPostIdWithCursor(id, parsedCursor, pageable)

        val hasNext = reactions.size > pageLimit
        val items = if (hasNext) reactions.subList(0, pageLimit) else reactions
        val nextCursor = if (hasNext) items.last().createdAt.toString() else null

        val responsePage = ReactionPageDto(
            items = items.map { mapToReactionDto(it) },
            nextCursor = nextCursor
        )

        return ResponseEntity.ok(responsePage)
    }

    @PostMapping("/{id}/comments")
    fun addComment(
        @PathVariable id: UUID,
        @RequestBody req: CreateCommentRequest
    ): ResponseEntity<Any> {
        val userId = getCurrentUserIdOrNull()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized"))

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        val post = postRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Post not found"))

        if (req.text.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Comment text is required"))
        }
        if (req.text.length > 1000) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Comment text cannot exceed 1000 characters"))
        }

        val comment = CommentEntity(
            postId = id,
            author = user,
            text = req.text
        )
        commentRepository.save(comment)

        post.commentCount = commentRepository.countByPostId(id)
        postRepository.save(post)

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToCommentDto(comment))
    }

    @GetMapping("/{id}/comments")
    fun getComments(
        @PathVariable id: UUID,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<Any> {
        val postExists = postRepository.existsById(id)
        if (!postExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Post not found"))
        }

        val parsedCursor = cursor?.let {
            try {
                OffsetDateTime.parse(it)
            } catch (e: Exception) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid cursor format"))
            }
        }

        val pageLimit = 10
        val pageable = org.springframework.data.domain.PageRequest.of(0, pageLimit + 1)
        val comments = commentRepository.findCommentsByPostIdWithCursor(id, parsedCursor, pageable)

        val hasNext = comments.size > pageLimit
        val items = if (hasNext) comments.subList(0, pageLimit) else comments
        val nextCursor = if (hasNext) items.last().createdAt.toString() else null

        val responsePage = app.drinkin.shared.model.CommentPage(
            items = items.map { mapToCommentDto(it) },
            nextCursor = nextCursor
        )

        return ResponseEntity.ok(responsePage)
    }
}
