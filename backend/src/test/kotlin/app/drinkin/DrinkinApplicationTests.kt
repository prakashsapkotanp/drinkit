package app.drinkin

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrinkinApplicationTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun contextLoads() {
        // Sanity check
    }

    @Test
    fun testFullWorkflowEndToEnd() {
        // 1. Underage registration must fail (DOB is under 18 years old)
        val underageRegisterReq = mapOf(
            "email" to "underage@drinkin.app",
            "username" to "underage",
            "password" to "password123",
            "dateOfBirth" to LocalDate.now().minusYears(10).toString()
        )
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(underageRegisterReq))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Must be at least 18")))

        // 2. Successful registration of User 1 (born in 1995, > 18 years old)
        val user1RegisterReq = mapOf(
            "email" to "user1@drinkin.app",
            "username" to "user1",
            "password" to "password123",
            "dateOfBirth" to "1995-05-15"
        )
        val user1RegisterResponse = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1RegisterReq))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.userId").exists())
            .andReturn()

        val user1Token = objectMapper.readTree(user1RegisterResponse.response.contentAsString).get("token").asText()
        val user1Id = objectMapper.readTree(user1RegisterResponse.response.contentAsString).get("userId").asText()

        // 3. Successful registration of User 2 (born in 1990, > 18 years old)
        val user2RegisterReq = mapOf(
            "email" to "user2@drinkin.app",
            "username" to "user2",
            "password" to "password123",
            "dateOfBirth" to "1990-10-10"
        )
        val user2RegisterResponse = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2RegisterReq))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.userId").exists())
            .andReturn()

        val user2Token = objectMapper.readTree(user2RegisterResponse.response.contentAsString).get("token").asText()
        val user2Id = objectMapper.readTree(user2RegisterResponse.response.contentAsString).get("userId").asText()

        // 4. Verify unauthorized endpoint access (should fail with 401 Unauthorized)
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)

        // 5. Retrieve own profile for User 1
        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username", `is`("user1")))
            .andExpect(jsonPath("$.followerCount", `is`(0)))

        // 6. Update User 1's profile
        val updateProfileReq = mapOf(
            "displayName" to "The Real User 1",
            "bio" to "Love tasting craft beers!",
            "drinkPreferences" to listOf("IPA", "Stout")
        )
        mockMvc.perform(
            put("/api/users/me")
                .header("Authorization", "Bearer $user1Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfileReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName", `is`("The Real User 1")))
            .andExpect(jsonPath("$.bio", `is`("Love tasting craft beers!")))
            .andExpect(jsonPath("$.drinkPreferences", containsInAnyOrder("IPA", "Stout")))

        // 7. User 1 follows User 2
        mockMvc.perform(
            post("/api/users/$user2Id/follow")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isNoContent)

        // Self-follow must fail
        mockMvc.perform(
            post("/api/users/$user1Id/follow")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isBadRequest)

        // Verify counts of User 2 (should have 1 follower)
        mockMvc.perform(
            get("/api/users/$user2Id")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.followerCount", `is`(1)))
            .andExpect(jsonPath("$.followingCount", `is`(0)))

        // Verify counts of User 1 (should be following 1 user)
        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.followingCount", `is`(1)))
            .andExpect(jsonPath("$.followerCount", `is`(0)))

        // 8. User 2 creates posts
        val createPost1Req = mapOf(
            "text" to "This is a delicious craft lager!",
            "drinkCategory" to "ALCOHOLIC",
            "drinkType" to "Beer",
            "rating" to 5,
            "tastingNotes" to "Crisp and refreshing",
            "scenario" to "Brewery tour"
        )
        val post1Response = mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $user2Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createPost1Req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.text", `is`("This is a delicious craft lager!")))
            .andExpect(jsonPath("$.drinkCategory", `is`("ALCOHOLIC")))
            .andReturn()

        val post1Id = objectMapper.readTree(post1Response.response.contentAsString).get("id").asText()

        // Create second post (non-alcoholic)
        val createPost2Req = mapOf(
            "text" to "Amazing cold brew morning starter!",
            "drinkCategory" to "NON_ALCOHOLIC",
            "drinkType" to "Coffee",
            "rating" to 4,
            "scenario" to "Morning routine"
        )
        val post2Response = mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $user2Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createPost2Req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.text", `is`("Amazing cold brew morning starter!")))
            .andExpect(jsonPath("$.drinkCategory", `is`("NON_ALCOHOLIC")))
            .andReturn()

        val post2Id = objectMapper.readTree(post2Response.response.contentAsString).get("id").asText()

        // 9. User 1 views User 2's alcoholic post (allowed since User 1 is 18+ and ageVerified is true)
        mockMvc.perform(
            get("/api/posts/$post1Id")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.text", `is`("This is a delicious craft lager!")))

        // 10. Multi-Reaction workflow
        // User 1 reacts with "LOVE" to User 2's post
        val reactReq = mapOf("reactionType" to "LOVE")
        mockMvc.perform(
            post("/api/posts/$post1Id/reactions")
                .header("Authorization", "Bearer $user1Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reactReq))
        )
            .andExpect(status().isNoContent)

        // Check reaction counts on the post DTO (total likeCount should reflect total reactions = 1)
        mockMvc.perform(
            get("/api/posts/$post1Id")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.likeCount", `is`(1)))

        // Get paginated reactions on post
        mockMvc.perform(
            get("/api/posts/$post1Id/reactions")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].reactionType", `is`("LOVE")))
            .andExpect(jsonPath("$.items[0].user.username", `is`("user1")))

        // Change reaction to "LIKE"
        val reactLikeReq = mapOf("reactionType" to "LIKE")
        mockMvc.perform(
            post("/api/posts/$post1Id/reactions")
                .header("Authorization", "Bearer $user1Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reactLikeReq))
        )
            .andExpect(status().isNoContent)

        // Verify the reaction type has changed
        mockMvc.perform(
            get("/api/posts/$post1Id/reactions")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].reactionType", `is`("LIKE")))

        // Delete the reaction
        mockMvc.perform(
            delete("/api/posts/$post1Id/reactions")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isNoContent)

        // Check reaction list is empty and likeCount is 0
        mockMvc.perform(
            get("/api/posts/$post1Id/reactions")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(0)))

        mockMvc.perform(
            get("/api/posts/$post1Id")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.likeCount", `is`(0)))

        // 11. User 1 comments on User 2's post
        val commentReq = mapOf("text" to "I totally agree, this looks amazing!")
        mockMvc.perform(
            post("/api/posts/$post1Id/comments")
                .header("Authorization", "Bearer $user1Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentReq))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.text", `is`("I totally agree, this looks amazing!")))
            .andExpect(jsonPath("$.author.username", `is`("user1")))

        // Retrieve comments
        mockMvc.perform(
            get("/api/posts/$post1Id/comments")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].text", `is`("I totally agree, this looks amazing!")))

        // 12. Retrieve User 1's feed (which should contain User 2's posts)
        mockMvc.perform(
            get("/api/feed")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(2)))
            .andExpect(jsonPath("$.items[0].text", `is`("Amazing cold brew morning starter!")))
            .andExpect(jsonPath("$.items[1].text", `is`("This is a delicious craft lager!")))

        // 13. User 1 unfollows User 2
        mockMvc.perform(
            delete("/api/users/$user2Id/follow")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isNoContent)

        // User 1's feed should now be empty (gracefully)
        mockMvc.perform(
            get("/api/feed")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(0)))
    }
}
