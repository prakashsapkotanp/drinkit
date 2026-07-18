package app.drinkin.user

import app.drinkin.shared.model.*
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/connections")
class ConnectionsController(
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

    @PostMapping
    fun sendRequest(@RequestBody req: ConnectionRequest): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val addresseeId = try {
            UUID.fromString(req.addresseeId)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid addresseeId format"))
        }

        if (currentUserId == addresseeId) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Cannot connect with yourself"))
        }

        val targetUserExists = userRepository.existsById(addresseeId)
        if (!targetUserExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Target user not found"))
        }

        val existing = connectionRepository.findConnectionBetween(currentUserId, addresseeId)
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Connection or request already exists"))
        }

        val connection = ConnectionEntity(
            requesterId = currentUserId,
            addresseeId = addresseeId,
            status = "PENDING"
        )
        connectionRepository.save(connection)

        val record = ConnectionRecord(
            id = connection.id.toString(),
            requesterId = connection.requesterId.toString(),
            addresseeId = connection.addresseeId.toString(),
            status = connection.status,
            createdAt = connection.createdAt.toString(),
            updatedAt = connection.updatedAt.toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(record)
    }

    @GetMapping("/requests")
    fun getPendingRequests(
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
        val list = connectionRepository.findPendingRequestsWithCursor(currentUserId, parsedCursor, pageable)

        val hasNext = list.size > pageLimit
        val items = if (hasNext) list.subList(0, pageLimit) else list
        val nextCursor = if (hasNext) items.last().createdAt.toString() else null

        val mappedItems = items.mapNotNull { conn ->
            val requester = userRepository.findById(conn.requesterId).orElse(null) ?: return@mapNotNull null
            PendingConnectionRequest(
                id = conn.id.toString(),
                requester = mapToUserProfile(requester, currentUserId),
                createdAt = conn.createdAt.toString()
            )
        }

        val page = PendingConnectionRequestPage(
            items = mappedItems,
            nextCursor = nextCursor
        )
        return ResponseEntity.ok(page)
    }

    @PutMapping("/{id}/accept")
    fun acceptRequest(@PathVariable id: UUID): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val connection = connectionRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Connection request not found"))

        if (connection.addresseeId != currentUserId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Only the addressee can accept the request"))
        }

        if (connection.status != "PENDING") {
            return ResponseEntity.badRequest().body(mapOf("error" to "Connection request is not pending"))
        }

        connection.status = "ACCEPTED"
        connection.updatedAt = OffsetDateTime.now()
        connectionRepository.save(connection)

        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/reject")
    fun rejectRequest(@PathVariable id: UUID): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val connection = connectionRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Connection request not found"))

        if (connection.addresseeId != currentUserId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Only the addressee can reject the request"))
        }

        if (connection.status != "PENDING") {
            return ResponseEntity.badRequest().body(mapOf("error" to "Connection request is not pending"))
        }

        connection.status = "REJECTED"
        connection.updatedAt = OffsetDateTime.now()
        connectionRepository.save(connection)

        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getAcceptedConnections(
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
        val list = connectionRepository.findAcceptedWithCursor(currentUserId, parsedCursor, pageable)

        val hasNext = list.size > pageLimit
        val items = if (hasNext) list.subList(0, pageLimit) else list
        val nextCursor = if (hasNext) items.last().createdAt.toString() else null

        val mappedItems = items.mapNotNull { conn ->
            val otherUserId = if (conn.requesterId == currentUserId) conn.addresseeId else conn.requesterId
            val otherUser = userRepository.findById(otherUserId).orElse(null) ?: return@mapNotNull null
            mapToUserProfile(otherUser, currentUserId)
        }

        val page = UserPage(
            items = mappedItems,
            nextCursor = nextCursor
        )
        return ResponseEntity.ok(page)
    }

    @DeleteMapping("/{id}")
    fun removeConnection(@PathVariable id: UUID): ResponseEntity<Any> {
        val currentUserId = getCurrentUserId()
        val connection = connectionRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Connection not found"))

        if (connection.requesterId != currentUserId && connection.addresseeId != currentUserId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Not a participant in this connection"))
        }

        if (connection.status != "ACCEPTED") {
            return ResponseEntity.badRequest().body(mapOf("error" to "Only accepted connections can be removed"))
        }

        connectionRepository.delete(connection)
        return ResponseEntity.noContent().build()
    }
}
