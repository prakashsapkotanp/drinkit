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

        // User 1's feed should show global posts since they follow nobody
        mockMvc.perform(
            get("/api/feed")
                .header("Authorization", "Bearer $user1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(2)))
    }

    @Test
    fun testConnectionsAndMessagingWorkflow() {
        // Register User A
        val userAReg = mapOf(
            "email" to "usera@drinkin.app",
            "username" to "userA",
            "password" to "password123",
            "dateOfBirth" to "1995-01-01"
        )
        val respA = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userAReg))
        ).andExpect(status().isCreated).andReturn()
        val tokenA = objectMapper.readTree(respA.response.contentAsString).get("token").asText()
        val idA = objectMapper.readTree(respA.response.contentAsString).get("userId").asText()

        // Register User B
        val userBReg = mapOf(
            "email" to "userb@drinkin.app",
            "username" to "userB",
            "password" to "password123",
            "dateOfBirth" to "1993-01-01"
        )
        val respB = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userBReg))
        ).andExpect(status().isCreated).andReturn()
        val tokenB = objectMapper.readTree(respB.response.contentAsString).get("token").asText()
        val idB = objectMapper.readTree(respB.response.contentAsString).get("userId").asText()

        // Register User C
        val userCReg = mapOf(
            "email" to "userc@drinkin.app",
            "username" to "userC",
            "password" to "password123",
            "dateOfBirth" to "1992-01-01"
        )
        val respC = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userCReg))
        ).andExpect(status().isCreated).andReturn()
        val tokenC = objectMapper.readTree(respC.response.contentAsString).get("token").asText()
        val idC = objectMapper.readTree(respC.response.contentAsString).get("userId").asText()

        // 1. User search validation
        mockMvc.perform(
            get("/api/users/search")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            get("/api/users/search?q=")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            get("/api/users/search?q=userB")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].username", `is`("userB")))

        // 2. Initial connectionStatus check (should be NONE)
        mockMvc.perform(
            get("/api/users/$idB")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionStatus", `is`("NONE")))

        // 3. Send connection request A -> B
        val sendReq = mapOf("addresseeId" to idB)
        val connResp = mockMvc.perform(
            post("/api/connections")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendReq))
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.status", `is`("PENDING")))
            .andReturn()
        val connectionId = objectMapper.readTree(connResp.response.contentAsString).get("id").asText()

        // 4. Bad requests: self-connection & duplicate
        val selfReq = mapOf("addresseeId" to idA)
        mockMvc.perform(
            post("/api/connections")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(selfReq))
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/api/connections")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendReq))
        ).andExpect(status().isConflict)

        // 5. Verify pending request status on both sides
        // For Requester (User A), other user status is PENDING_SENT
        mockMvc.perform(
            get("/api/users/$idB")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionStatus", `is`("PENDING_SENT")))

        // For Addressee (User B), other user status is PENDING_RECEIVED
        mockMvc.perform(
            get("/api/users/$idA")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionStatus", `is`("PENDING_RECEIVED")))

        // User B gets requests list
        mockMvc.perform(
            get("/api/connections/requests")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].id", `is`(connectionId)))
            .andExpect(jsonPath("$.items[0].requester.username", `is`("userA")))

        // 6. Security: User C cannot accept/reject the request
        mockMvc.perform(
            put("/api/connections/$connectionId/accept")
                .header("Authorization", "Bearer $tokenC")
        ).andExpect(status().isForbidden)

        // 7. Accept connection
        mockMvc.perform(
            put("/api/connections/$connectionId/accept")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(status().isNoContent)

        // 8. Verify accepted connection
        mockMvc.perform(
            get("/api/users/$idB")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionStatus", `is`("CONNECTED")))

        // User B's accepted connections list
        mockMvc.perform(
            get("/api/connections")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].username", `is`("userA")))

        // 9. Conversational/Messaging workflows
        // Try to message C (should return 403)
        val convWithC = mapOf("otherUserId" to idC)
        mockMvc.perform(
            post("/api/conversations")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(convWithC))
        ).andExpect(status().isForbidden)

        // Create conversation A <-> B
        val convWithB = mapOf("otherUserId" to idB)
        val convResp = mockMvc.perform(
            post("/api/conversations")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(convWithB))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.otherUser.username", `is`("userB")))
            .andReturn()
        val conversationId = objectMapper.readTree(convResp.response.contentAsString).get("id").asText()

        // Send a message A -> B
        val msgReq = mapOf("text" to "Hello User B, cheers!")
        mockMvc.perform(
            post("/api/conversations/$conversationId/messages")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msgReq))
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.text", `is`("Hello User B, cheers!")))

        // User C tries to read B's messages (403 expected)
        mockMvc.perform(
            get("/api/conversations/$conversationId/messages")
                .header("Authorization", "Bearer $tokenC")
        ).andExpect(status().isForbidden)

        // User B reads messages
        mockMvc.perform(
            get("/api/conversations/$conversationId/messages")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].text", `is`("Hello User B, cheers!")))

        // Retrieve conversations list for User B
        mockMvc.perform(
            get("/api/conversations")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].lastMessagePreview", `is`("Hello User B, cheers!")))

        // 10. DELETE Connection (Unconnect)
        mockMvc.perform(
            delete("/api/connections/$connectionId")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isNoContent)

        // Verify status is back to NONE
        mockMvc.perform(
            get("/api/users/$idB")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.connectionStatus", `is`("NONE")))
    }
}
