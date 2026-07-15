package app.drinkin.post

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class ReactionSeeder(
    private val reactionTypeRepository: ReactionTypeRepository
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
    }
}
