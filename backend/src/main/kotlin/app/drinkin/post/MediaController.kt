package app.drinkin.post

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.util.UUID

@RestController
@RequestMapping("/api/media")
class MediaController {

    private val uploadDir = File(System.getProperty("java.io.tmpdir"), "drinkin-uploads").apply {
        if (!exists()) mkdirs()
    }

    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Any> {
        // 5MB limit: 5 * 1024 * 1024 = 5,242,880 bytes
        if (file.size > 5_242_880) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "File size exceeds the 5MB limit."))
        }
        val contentType = file.contentType ?: ""
        if (!contentType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "Only image files are allowed."))
        }

        val fileExtension = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "bin"
        }

        val uniqueFilename = "${UUID.randomUUID()}.$fileExtension"
        val destFile = File(uploadDir, uniqueFilename)
        file.transferTo(destFile)

        val fileUrl = "/api/media/$uniqueFilename"
        return ResponseEntity.ok(mapOf("url" to fileUrl))
    }

    @GetMapping("/{filename}")
    fun getFile(@PathVariable filename: String): ResponseEntity<ByteArray> {
        val file = File(uploadDir, filename)
        if (!file.exists()) {
            return ResponseEntity.notFound().build()
        }
        val data = Files.readAllBytes(file.toPath())
        val contentType = when (filename.substringAfterLast('.')) {
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG
            "png" -> MediaType.IMAGE_PNG
            "webp" -> MediaType.parseMediaType("image/webp")
            "gif" -> MediaType.IMAGE_GIF
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
        return ResponseEntity.ok().contentType(contentType).body(data)
    }
}
