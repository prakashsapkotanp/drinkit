package app.drinkin.post

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface CommentRepository : JpaRepository<CommentEntity, UUID> {

    fun countByPostId(postId: UUID): Int

    @Query("""
        SELECT c FROM CommentEntity c
        WHERE c.postId = :postId
          AND (:cursor IS NULL OR c.createdAt < :cursor)
        ORDER BY c.createdAt DESC
    """)
    fun findCommentsByPostIdWithCursor(
        @Param("postId") postId: UUID,
        @Param("cursor") cursor: OffsetDateTime?,
        pageable: Pageable
    ): List<CommentEntity>
}
