package app.drinkin.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FollowRepository : JpaRepository<FollowEntity, FollowId> {
    fun countByIdFollowingId(followingId: UUID): Int
    fun countByIdFollowerId(followerId: UUID): Int
    fun existsByIdFollowerIdAndIdFollowingId(followerId: UUID, followingId: UUID): Boolean

    @Query("SELECT f.id.followingId FROM FollowEntity f WHERE f.id.followerId = :followerId")
    fun findFollowingIds(@Param("followerId") followerId: UUID): List<UUID>
}
