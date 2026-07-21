package app.drinkin.chat

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface MessageRepository : JpaRepository<MessageEntity, UUID> {

    @Query("SELECT m FROM MessageEntity m WHERE m.conversationId = :conversationId AND (cast(:cursor as timestamp) IS NULL OR m.createdAt < :cursor) ORDER BY m.createdAt DESC")
    fun findMessagesWithCursor(
        @Param("conversationId") conversationId: UUID,
        @Param("cursor") cursor: OffsetDateTime?,
        pageable: Pageable
    ): List<MessageEntity>

    fun findFirstByConversationIdOrderByCreatedAtDesc(conversationId: UUID): MessageEntity?

    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.read = false AND m.senderId <> :userId AND m.conversationId IN (SELECT c.id FROM ConversationEntity c WHERE c.userAId = :userId OR c.userBId = :userId)")
    fun countUnreadMessagesForUser(@Param("userId") userId: UUID): Int
}
