package app.drinkin.post

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface PostRepository : JpaRepository<PostEntity, UUID> {

    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.author.id IN :authorIds
          AND (cast(:cursor as timestamp) IS NULL OR p.createdAt < :cursor)
        ORDER BY p.createdAt DESC
    """)
    fun findFeedPosts(
        @Param("authorIds") authorIds: List<UUID>,
        @Param("cursor") cursor: OffsetDateTime?,
        pageable: Pageable
    ): List<PostEntity>

    @Query("""
        SELECT p FROM PostEntity p
        WHERE (cast(:cursor as timestamp) IS NULL OR p.createdAt < :cursor)
        ORDER BY p.createdAt DESC
    """)
    fun findAllPosts(
        @Param("cursor") cursor: OffsetDateTime?,
        pageable: Pageable
    ): List<PostEntity>
}
