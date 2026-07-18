package app.drinkin.user

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "connections")
class ConnectionEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "requester_id", nullable = false)
    val requesterId: UUID,

    @Column(name = "addressee_id", nullable = false)
    val addresseeId: UUID,

    @Column(nullable = false)
    var status: String = "PENDING",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
