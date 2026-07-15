package app.drinkin.user

import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false, unique = true)
    val username: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "display_name")
    var displayName: String? = null,

    var bio: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column(name = "date_of_birth", nullable = false)
    val dateOfBirth: LocalDate,

    @Column(name = "age_verified", nullable = false)
    var ageVerified: Boolean = false,

    @Column(name = "drink_preferences")
    var drinkPreferences: Array<String> = emptyArray(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
