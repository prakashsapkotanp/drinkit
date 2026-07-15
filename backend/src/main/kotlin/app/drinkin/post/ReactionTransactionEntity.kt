package app.drinkin.post

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "reaction_transactions")
class ReactionTransactionEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "post_id", nullable = false)
    val postId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 10)
    val action: String, // ADDED / CHANGED / REMOVED

    @Column(name = "reaction_type_id")
    val reactionTypeId: Short?,

    @Column(name = "previous_reaction_type_id")
    val previousReactionTypeId: Short?,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
