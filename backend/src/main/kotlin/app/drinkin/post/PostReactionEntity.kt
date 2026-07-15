package app.drinkin.post

import jakarta.persistence.*
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

class PostReactionId(
    val postId: UUID = UUID.randomUUID(),
    val userId: UUID = UUID.randomUUID()
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostReactionId) return false
        return postId == other.postId && userId == other.userId
    }

    override fun hashCode(): Int {
        return 31 * postId.hashCode() + userId.hashCode()
    }
}

@Entity
@Table(name = "post_reactions")
@IdClass(PostReactionId::class)
class PostReactionEntity(
    @Id
    @Column(name = "post_id")
    val postId: UUID,

    @Id
    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "reaction_type_id", nullable = false)
    var reactionTypeId: Short,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
