package app.drinkin.post

import app.drinkin.shared.model.DrinkCategory
import app.drinkin.user.UserEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "posts")
class PostEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: UserEntity,

    @Column(nullable = false, length = 3000)
    var text: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "drink_category", nullable = false, length = 20)
    var drinkCategory: DrinkCategory,

    @Column(name = "drink_type", length = 50)
    var drinkType: String? = null,

    @JdbcTypeCode(SqlTypes.SMALLINT)
    var rating: Int? = null,

    @Column(name = "tasting_notes", length = 1000)
    var tastingNotes: String? = null,

    @Column(length = 100)
    var scenario: String? = null,

    @Column(name = "media_urls")
    var mediaUrls: Array<String> = emptyArray(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reaction_counts", nullable = false)
    var reactionCounts: Map<String, Int> = emptyMap(),

    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
