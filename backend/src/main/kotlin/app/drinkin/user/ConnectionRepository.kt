package app.drinkin.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface ConnectionRepository : JpaRepository<ConnectionEntity, UUID> {

    @Query("SELECT c FROM ConnectionEntity c WHERE (c.requesterId = :userA AND c.addresseeId = :userB) OR (c.requesterId = :userB AND c.addresseeId = :userA)")
    fun findConnectionBetween(@Param("userA") userA: UUID, @Param("userB") userB: UUID): ConnectionEntity?

    @Query("SELECT c FROM ConnectionEntity c WHERE c.addresseeId = :addresseeId AND c.status = 'PENDING' AND (cast(:cursor as timestamp) IS NULL OR c.createdAt < :cursor) ORDER BY c.createdAt DESC")
    fun findPendingRequestsWithCursor(
        @Param("addresseeId") addresseeId: UUID,
        @Param("cursor") cursor: OffsetDateTime?,
        pageable: Pageable
    ): List<ConnectionEntity>

    @Query("SELECT c FROM ConnectionEntity c WHERE (c.requesterId = :userId OR c.addresseeId = :userId) AND c.status = 'ACCEPTED' AND (cast(:cursor as timestamp) IS NULL OR c.createdAt < :cursor) ORDER BY c.createdAt DESC")
    fun findAcceptedWithCursor(
        @Param("userId") userId: UUID,
        @Param("cursor") cursor: OffsetDateTime?,
        pageable: Pageable
    ): List<ConnectionEntity>

    @Query("SELECT COUNT(c) > 0 FROM ConnectionEntity c WHERE ((c.requesterId = :userA AND c.addresseeId = :userB) OR (c.requesterId = :userB AND c.addresseeId = :userA)) AND c.status = 'ACCEPTED'")
    fun areConnected(@Param("userA") userA: UUID, @Param("userB") userB: UUID): Boolean
}
