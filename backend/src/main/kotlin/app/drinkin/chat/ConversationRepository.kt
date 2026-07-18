package app.drinkin.chat

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface ConversationRepository : JpaRepository<ConversationEntity, UUID> {

    fun findByUserAIdAndUserBId(userAId: UUID, userBId: UUID): ConversationEntity?

    @Query("SELECT c FROM ConversationEntity c WHERE (c.userAId = :userId OR c.userBId = :userId) AND (cast(:cursor as timestamp) IS NULL OR c.lastMessageAt < :cursor) ORDER BY c.lastMessageAt DESC, c.createdAt DESC")
    fun findConversationsWithCursor(
        @Param("userId") userId: UUID,
        @Param("cursor") cursor: OffsetDateTime?,
        pageable: Pageable
    ): List<ConversationEntity>
}
