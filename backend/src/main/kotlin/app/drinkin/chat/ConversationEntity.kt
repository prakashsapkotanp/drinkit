package app.drinkin.chat

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "conversations")
class ConversationEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_a_id", nullable = false)
    val userAId: UUID,

    @Column(name = "user_b_id", nullable = false)
    val userBId: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "last_message_at")
    var lastMessageAt: OffsetDateTime? = null
)
