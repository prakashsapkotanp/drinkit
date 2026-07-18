package app.drinkin.auth

import app.drinkin.user.UserEntity
import app.drinkin.user.UserRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.Period

data class RegisterRequest(
    @field:Email val email: String,
    @field:NotBlank @field:Size(min = 3, max = 50) val username: String,
    @field:Size(min = 8) val password: String,
    val dateOfBirth: LocalDate
)

data class LoginRequest(
    @field:Email val email: String,
    @field:NotBlank val password: String
)

data class AuthResponse(val token: String, val userId: String, val username: String)

private const val MIN_DRINKING_AGE = 18

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest): ResponseEntity<Any> {
        val emailTrimmed = req.email.trim()
        val usernameTrimmed = req.username.trim()
        val passwordTrimmed = req.password.trim()
        val age = Period.between(req.dateOfBirth, LocalDate.now()).years
        if (age < MIN_DRINKING_AGE) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Must be at least $MIN_DRINKING_AGE years old"))
        }
        if (userRepository.existsByEmail(emailTrimmed) || userRepository.existsByUsername(usernameTrimmed)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Email or username already exists"))
        }

        val user = UserEntity(
            email = emailTrimmed,
            username = usernameTrimmed,
            passwordHash = passwordEncoder.encode(passwordTrimmed),
            dateOfBirth = req.dateOfBirth,
            ageVerified = true
        )
        userRepository.save(user)

        val token = jwtService.generateToken(user.id.toString())
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AuthResponse(token, user.id.toString(), user.username))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ResponseEntity<Any> {
        val emailTrimmed = req.email.trim()
        val passwordTrimmed = req.password.trim()
        val user = userRepository.findByEmail(emailTrimmed)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))

        if (!passwordEncoder.matches(passwordTrimmed, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))
        }

        val token = jwtService.generateToken(user.id.toString())
        return ResponseEntity.ok(AuthResponse(token, user.id.toString(), user.username))
    }
}
