package app.drinkin.post

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface PostReactionRepository : JpaRepository<PostReactionEntity, PostReactionId> {

    fun findByPostIdAndUserId(postId: UUID, userId: UUID): PostReactionEntity?
    fun countByPostIdAndReactionTypeId(postId: UUID, reactionTypeId: Short): Int

    @Query("""
        SELECT r FROM PostReactionEntity r
        WHERE r.postId = :postId
          AND (:cursor IS NULL OR r.createdAt < :cursor)
        ORDER BY r.createdAt DESC
    """)
    fun findReactionsByPostIdWithCursor(
        @Param("postId") postId: UUID,
        @Param("cursor") cursor: OffsetDateTime?,
        pageable: Pageable
    ): List<PostReactionEntity>
}
