package app.drinkin.post

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReactionTransactionRepository : JpaRepository<ReactionTransactionEntity, UUID>
