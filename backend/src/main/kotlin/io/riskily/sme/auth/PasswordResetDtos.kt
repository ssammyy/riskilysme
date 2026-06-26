package io.riskily.sme.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ForgotPasswordRequest(
    @field:Email @field:NotBlank val email: String,
)

data class ResetPasswordRequest(
    @field:NotBlank val token: String,
    @field:NotBlank @field:Size(min = 8, max = 100) val newPassword: String,
)

data class VerifyEmailRequest(
    @field:NotBlank val token: String,
)

data class ResendVerificationRequest(
    @field:Email @field:NotBlank val email: String,
)
