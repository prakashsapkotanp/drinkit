package app.drinkin.post

import app.drinkin.shared.model.DrinkCategory
import app.drinkin.user.UserEntity
import app.drinkin.user.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime

@Component
class ReactionSeeder(
    private val reactionTypeRepository: ReactionTypeRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val passwordEncoder: PasswordEncoder,
    private val environment: org.springframework.core.env.Environment
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        val count = reactionTypeRepository.count()
        if (count == 0L) {
            val types = listOf(
                ReactionTypeEntity(1, "LIKE", "Like", "👍"),
                ReactionTypeEntity(2, "LOVE", "Love", "❤️"),
                ReactionTypeEntity(3, "CHEERS", "Cheers", "🥂"),
                ReactionTypeEntity(4, "WOW", "Wow", "😮"),
                ReactionTypeEntity(5, "SAD", "Sad", "😢")
            )
            reactionTypeRepository.saveAll(types)
        }

        val isTestProfile = environment.activeProfiles.contains("test")
        if (!isTestProfile) {
            if (!userRepository.existsByEmail("user1@drinkin.app")) {
                val defaultUser = UserEntity(
                    email = "user1@drinkin.app",
                    username = "user1",
                    passwordHash = passwordEncoder.encode("password123"),
                    dateOfBirth = LocalDate.of(1995, 5, 15),
                    ageVerified = true
                )
                userRepository.save(defaultUser)

                val post1 = PostEntity(
                    author = defaultUser,
                    text = "Welcome to Drinkin'! This is a default seeded post about an amazing robust craft stout. Dark chocolate and espresso notes are incredibly present!",
                    drinkCategory = DrinkCategory.ALCOHOLIC,
                    drinkType = "Stout",
                    rating = 5,
                    tastingNotes = "Roasty, dark chocolate, espresso",
                    scenario = "Evening relaxation",
                    createdAt = OffsetDateTime.now().minusHours(2)
                )

                val post2 = PostEntity(
                    author = defaultUser,
                    text = "Just started my morning with a pour-over of single-origin Ethiopian coffee. Extremely bright floral and citrus notes!",
                    drinkCategory = DrinkCategory.NON_ALCOHOLIC,
                    drinkType = "Coffee",
                    rating = 4,
                    tastingNotes = "Bright, citrus, jasmine",
                    scenario = "Morning routine",
                    createdAt = OffsetDateTime.now().minusHours(1)
                )

                postRepository.saveAll(listOf(post1, post2))
            }
        }
    }
}
