package app.drinkin.post

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.springframework.security.test.context.support.WithMockUser

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class MediaControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun testUploadImageSuccessfully() {
        val file = MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image content".toByteArray()
        )

        val result = mockMvc.perform(
            multipart("/api/media/upload")
                .file(file)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.url").exists())
            .andReturn()

        val url = objectMapper.readTree(result.response.contentAsString).get("url").asText()

        // Fetch the file back
        mockMvc.perform(get(url))
            .andExpect(status().isOk)
    }

    @Test
    fun testUploadImageTooLarge() {
        // Exceeds 5MB (5 * 1024 * 1024 + 1 bytes = 5,242,881 bytes)
        val largeBytes = ByteArray(5_242_881)
        val file = MockMultipartFile(
            "file",
            "large.jpg",
            "image/jpeg",
            largeBytes
        )

        mockMvc.perform(
            multipart("/api/media/upload")
                .file(file)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("exceeds the 5MB limit")))
    }

    @Test
    fun testUploadNonImageRejected() {
        val file = MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "not an image".toByteArray()
        )

        mockMvc.perform(
            multipart("/api/media/upload")
                .file(file)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error", containsString("Only image files are allowed")))
    }
}
