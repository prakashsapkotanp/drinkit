package app.drinkin.chat

import app.drinkin.shared.model.UnreadCountsResponse
import app.drinkin.user.ConnectionRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val connectionRepository: ConnectionRepository,
    private val messageRepository: MessageRepository
) {

    private fun getCurrentUserId(): UUID {
        val principal = SecurityContextHolder.getContext().authentication.principal as String
        return UUID.fromString(principal)
    }

    @GetMapping("/unread-counts")
    fun getUnreadCounts(): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val pendingConnections = connectionRepository.countByAddresseeIdAndStatus(currentUserId, "PENDING")
        val unreadMessages = messageRepository.countUnreadMessagesForUser(currentUserId)
        return ResponseEntity.ok(
            UnreadCountsResponse(
                pendingConnectionsCount = pendingConnections,
                unreadMessagesCount = unreadMessages
            )
        )
    }
}
