package io.riskily.sme.common

import io.riskily.sme.auth.EmailAlreadyExistsException
import io.riskily.sme.auth.InvalidTokenException
import io.riskily.sme.entitlement.FeatureAccessDeniedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "invalid")
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors)
    }

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailExists(ex: EmailAlreadyExistsException): ResponseEntity<ApiError> =
        build(HttpStatus.CONFLICT, ex.message ?: "Email already exists")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ApiError> =
        build(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request")

    @ExceptionHandler(FeatureAccessDeniedException::class)
    fun handleFeatureDenied(ex: FeatureAccessDeniedException): ResponseEntity<ApiError> =
        build(HttpStatus.FORBIDDEN, ex.message ?: "Upgrade required")

    @ExceptionHandler(
        BadCredentialsException::class,
        DisabledException::class,
        LockedException::class,
        InvalidTokenException::class,
    )
    fun handleAuth(ex: Exception): ResponseEntity<ApiError> =
        build(HttpStatus.UNAUTHORIZED, "Invalid email or password")

    private fun build(
        status: HttpStatus,
        message: String,
        fieldErrors: Map<String, String>? = null,
    ): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(status.value(), status.reasonPhrase, message, fieldErrors),
        )
}
