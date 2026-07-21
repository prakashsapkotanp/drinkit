package app.drinkin.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val errorMessage = ex.bindingResult.allErrors.mapNotNull { error ->
            val fieldName = (error as? FieldError)?.field
            val msg = error.defaultMessage
            if (fieldName != null) "$fieldName: $msg" else msg
        }.firstOrNull() ?: "Validation error"

        return ResponseEntity.badRequest().body(mapOf("error" to errorMessage))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<Map<String, String>> {
        val status = ex.statusCode
        val reason = ex.reason ?: "An error occurred"
        val httpStatus = HttpStatus.resolve(status.value()) ?: HttpStatus.BAD_REQUEST
        return ResponseEntity.status(httpStatus).body(mapOf("error" to reason))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        val message = ex.message ?: "Invalid request argument"
        return ResponseEntity.badRequest().body(mapOf("error" to message))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<Map<String, String>> {
        val message = ex.message ?: "Invalid request state"
        return ResponseEntity.badRequest().body(mapOf("error" to message))
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception): ResponseEntity<Map<String, String>> {
        val message = ex.message ?: "An unexpected error occurred"
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to message))
    }
}
