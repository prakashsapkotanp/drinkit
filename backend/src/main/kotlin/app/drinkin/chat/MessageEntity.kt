package app.drinkin.chat

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "messages")
class MessageEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "conversation_id", nullable = false)
    val conversationId: UUID,

    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,

    @Column(nullable = false)
    val text: String,

    @Column(nullable = false)
    var read: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
