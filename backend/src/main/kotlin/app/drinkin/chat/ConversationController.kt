package app.drinkin.chat

import app.drinkin.shared.model.*
import app.drinkin.user.ConnectionRepository
import app.drinkin.user.FollowRepository
import app.drinkin.user.UserRepository
import app.drinkin.user.UserEntity
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val connectionRepository: ConnectionRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository
) {

    private fun getCurrentUserId(): UUID {
        val principal = SecurityContextHolder.getContext().authentication.principal as String
        return UUID.fromString(principal)
    }

    private fun mapToUserProfile(user: UserEntity, currentUserId: UUID): UserProfile {
        val followerCount = followRepository.countByIdFollowingId(user.id)
        val followingCount = followRepository.countByIdFollowerId(user.id)

        // Compute connection status relative to current user
        val conn = connectionRepository.findConnectionBetween(currentUserId, user.id)
        val connectionStatus = if (currentUserId == user.id) {
            null
        } else if (conn == null) {
            "NONE"
        } else if (conn.status == "ACCEPTED") {
            "CONNECTED"
        } else if (conn.status == "PENDING") {
            if (conn.requesterId == currentUserId) "PENDING_SENT" else "PENDING_RECEIVED"
        } else {
            "NONE"
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

    private fun mapToConversationDto(entity: ConversationEntity, currentUserId: UUID): Conversation {
        val otherUserId = if (entity.userAId == currentUserId) entity.userBId else entity.userAId
        val otherUser = userRepository.findById(otherUserId).orElseThrow { RuntimeException("User not found") }

        val latestMessage = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(entity.id)
        return Conversation(
            id = entity.id.toString(),
            otherUser = mapToUserProfile(otherUser, currentUserId),
            lastMessagePreview = latestMessage?.text,
            lastMessageAt = latestMessage?.createdAt?.toString() ?: entity.createdAt.toString()
        )
    }

    private fun mapToMessageDto(entity: MessageEntity): Message {
        return Message(
            id = entity.id.toString(),
            conversationId = entity.conversationId.toString(),
            senderId = entity.senderId.toString(),
            text = entity.text,
            read = entity.read,
            createdAt = entity.createdAt.toString()
        )
    }

    @PostMapping
    fun getOrCreateConversation(@RequestBody req: ConversationRequest): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val otherUserId = try {
            UUID.fromString(req.otherUserId)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid otherUserId format"))
        }

        if (currentUserId == otherUserId) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Cannot have a conversation with yourself"))
        }

        // Check if there is an accepted connection
        val isConnected = connectionRepository.areConnected(currentUserId, otherUserId)
        if (!isConnected) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Gated: You must be connected to start a conversation"))
        }

        val sorted = listOf(currentUserId, otherUserId).sorted()
        val userAId = sorted[0]
        val userBId = sorted[1]

        val existing = conversationRepository.findByUserAIdAndUserBId(userAId, userBId)
        if (existing != null) {
            return ResponseEntity.ok(mapToConversationDto(existing, currentUserId))
        }

        val conversation = ConversationEntity(
            userAId = userAId,
            userBId = userBId
        )
        conversationRepository.save(conversation)

        return ResponseEntity.ok(mapToConversationDto(conversation, currentUserId))
    }

    @GetMapping
    fun getConversations(
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val parsedCursor = cursor?.let {
            try {
                OffsetDateTime.parse(it)
            } catch (e: Exception) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid cursor format"))
            }
        }

        val pageLimit = 10
        val pageable = PageRequest.of(0, pageLimit + 1)
        val list = conversationRepository.findConversationsWithCursor(currentUserId, parsedCursor, pageable)

        val hasNext = list.size > pageLimit
        val items = if (hasNext) list.subList(0, pageLimit) else list
        val nextCursor = if (hasNext) {
            val lastItem = items.last()
            (lastItem.lastMessageAt ?: lastItem.createdAt).toString()
        } else {
            null
        }

        val mappedItems = items.map { mapToConversationDto(it, currentUserId) }
        val page = ConversationPage(
            items = mappedItems,
            nextCursor = nextCursor
        )
        return ResponseEntity.ok(page)
    }

    @GetMapping("/{id}/messages")
    fun getMessages(
        @PathVariable id: UUID,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val conversation = conversationRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Conversation not found"))

        if (conversation.userAId != currentUserId && conversation.userBId != currentUserId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Not a participant in this conversation"))
        }

        val parsedCursor = cursor?.let {
            try {
                OffsetDateTime.parse(it)
            } catch (e: Exception) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid cursor format"))
            }
        }

        val pageLimit = 20
        val pageable = PageRequest.of(0, pageLimit + 1)
        val list = messageRepository.findMessagesWithCursor(id, parsedCursor, pageable)

        val hasNext = list.size > pageLimit
        val items = if (hasNext) list.subList(0, pageLimit) else list
        val nextCursor = if (hasNext) items.last().createdAt.toString() else null

        val mappedItems = items.map { mapToMessageDto(it) }
        val page = MessagePage(
            items = mappedItems,
            nextCursor = nextCursor
        )
        return ResponseEntity.ok(page)
    }

    @PostMapping("/{id}/messages")
    fun sendMessage(
        @PathVariable id: UUID,
        @RequestBody req: MessageRequest
    ): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val conversation = conversationRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Conversation not found"))

        if (conversation.userAId != currentUserId && conversation.userBId != currentUserId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Not a participant in this conversation"))
        }

        val reqText = req.text
        if (reqText.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Message text cannot be empty"))
        }
        if (reqText.length > 2000) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Message text cannot exceed 2000 characters"))
        }

        val message = MessageEntity(
            conversationId = id,
            senderId = currentUserId,
            text = reqText
        )
        messageRepository.save(message)

        // Touch the conversation last message timestamp
        conversation.lastMessageAt = message.createdAt
        conversationRepository.save(conversation)

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToMessageDto(message))
    }
}
