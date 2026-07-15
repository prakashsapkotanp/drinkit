package app.drinkin.user

import jakarta.persistence.*
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

@Embeddable
class FollowId(
    @Column(name = "follower_id")
    val followerId: UUID,

    @Column(name = "following_id")
    val followingId: UUID
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FollowId) return false
        return followerId == other.followerId && followingId == other.followingId
    }

    override fun hashCode(): Int {
        return 31 * followerId.hashCode() + followingId.hashCode()
    }
}

@Entity
@Table(name = "follows")
class FollowEntity(
    @EmbeddedId
    val id: FollowId,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
