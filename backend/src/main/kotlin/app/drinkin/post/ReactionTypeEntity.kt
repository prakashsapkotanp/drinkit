package app.drinkin.post

import jakarta.persistence.*

@Entity
@Table(name = "reaction_types")
class ReactionTypeEntity(
    @Id
    val id: Short,

    @Column(nullable = false, unique = true, length = 20)
    val code: String,

    @Column(nullable = false, length = 50)
    val label: String,

    @Column(length = 10)
    val emoji: String? = null
)
