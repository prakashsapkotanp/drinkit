package app.drinkin.post

import org.springframework.data.jpa.repository.JpaRepository

interface ReactionTypeRepository : JpaRepository<ReactionTypeEntity, Short> {
    fun findByCode(code: String): ReactionTypeEntity?
}
