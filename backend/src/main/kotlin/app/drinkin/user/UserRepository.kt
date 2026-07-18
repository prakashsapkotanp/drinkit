package app.drinkin.user

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findByUsername(username: String): UserEntity?
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean

    @Query("SELECT u FROM UserEntity u WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))) AND (:cursor IS NULL OR u.username > :cursor) ORDER BY u.username ASC")
    fun searchUsers(
        @Param("query") query: String,
        @Param("cursor") cursor: String?,
        pageable: Pageable
    ): List<UserEntity>
}
