package app.drinkin.post

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeedPrioritizationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun testFeedPrioritizationScoring() {
        // 1. Register User A (has drinkPreferences: ["IPA", "Espresso"])
        val userAReg = mapOf(
            "email" to "ai_usera@drinkin.app",
            "username" to "ai_userA",
            "password" to "password123",
            "dateOfBirth" to "1995-01-01"
        )
        val respA = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userAReg))
        ).andExpect(status().isCreated).andReturn()
        val tokenA = objectMapper.readTree(respA.response.contentAsString).get("token").asText()

        // Set preferences for User A
        val updateProfileReq = mapOf(
            "drinkPreferences" to listOf("IPA", "Espresso")
        )
        mockMvc.perform(
            put("/api/users/me")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProfileReq))
        ).andExpect(status().isOk)

        // 2. Register User B (will be connected to User A)
        val userBReg = mapOf(
            "email" to "ai_userb@drinkin.app",
            "username" to "ai_userB",
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

        // Establish connection between A and B
        val sendReq = mapOf("addresseeId" to idB)
        val connResp = mockMvc.perform(
            post("/api/connections")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendReq))
        ).andExpect(status().isCreated).andReturn()
        val connectionId = objectMapper.readTree(connResp.response.contentAsString).get("id").asText()

        mockMvc.perform(
            put("/api/connections/$connectionId/accept")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(status().isNoContent)

        // 3. Register User C (not connected)
        val userCReg = mapOf(
            "email" to "ai_userc@drinkin.app",
            "username" to "ai_userC",
            "password" to "password123",
            "dateOfBirth" to "1992-01-01"
        )
        val respC = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userCReg))
        ).andExpect(status().isCreated).andReturn()
        val tokenC = objectMapper.readTree(respC.response.contentAsString).get("token").asText()

        // Create posts:
        // Post 1: User C (not connected), doesn't match preferences
        val post1Req = mapOf(
            "text" to "Water is cool.",
            "drinkCategory" to "NON_ALCOHOLIC",
            "drinkType" to "Water"
        )
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $tokenC")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(post1Req))
        ).andExpect(status().isCreated)

        // Post 2: User C (not connected), matches preference "Espresso"
        val post2Req = mapOf(
            "text" to "Drinking an amazing double Espresso shot!",
            "drinkCategory" to "NON_ALCOHOLIC",
            "drinkType" to "Espresso"
        )
        val respPost2 = mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $tokenC")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(post2Req))
        ).andExpect(status().isCreated).andReturn()
        val post2Id = objectMapper.readTree(respPost2.response.contentAsString).get("id").asText()

        // Post 3: User B (connected), does not match preferences
        val post3Req = mapOf(
            "text" to "Having standard tea.",
            "drinkCategory" to "NON_ALCOHOLIC",
            "drinkType" to "Tea"
        )
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $tokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(post3Req))
        ).andExpect(status().isCreated)

        // Since User A has no followings, they get all posts sorted by score.
        // Ranking order should be:
        // 1st: Post 3 (from User B, connected, score: 100.0)
        // 2nd: Post 2 (User C, preference match, score: 50.0 + 20.0 = 70.0)
        // 3rd: Post 1 (User C, no match, score: 0.0)
        val feedResp = mockMvc.perform(
            get("/api/feed")
                .header("Authorization", "Bearer $tokenA")
        ).andExpect(status().isOk).andReturn()

        val json = feedResp.response.contentAsString
        val root = objectMapper.readTree(json)
        val items = root.get("items")

        // Verify we got the items prioritized correctly
        org.junit.jupiter.api.Assertions.assertTrue(items.size() >= 3)
        // Item 0 should be Post 3 (Tea from B)
        org.junit.jupiter.api.Assertions.assertEquals("ai_userB", items.get(0).get("author").get("username").asText())
        // Item 1 should be Post 2 (Espresso from C)
        org.junit.jupiter.api.Assertions.assertEquals("ai_userC", items.get(1).get("author").get("username").asText())
        org.junit.jupiter.api.Assertions.assertEquals("Espresso", items.get(1).get("drinkType").asText())
        // Item 2 should be Post 1 (Water from C)
        org.junit.jupiter.api.Assertions.assertEquals("Water", items.get(2).get("drinkType").asText())
    }
}
