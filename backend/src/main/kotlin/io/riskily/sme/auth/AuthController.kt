package io.riskily.sme.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val passwordResetService: PasswordResetService,
    private val emailVerificationService: EmailVerificationService,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): AuthResponse =
        authService.refresh(request)

    /**
     * Stateless logout: tokens are discarded client-side. Endpoint exists for a clean
     * client contract; server-side revocation (token blacklist) is a TODO if required.
     */
    @PostMapping("/logout")
    fun logout(): ResponseEntity<Void> = ResponseEntity.noContent().build()

    /** Start a password reset. Always returns 204 (no account enumeration). */
    @PostMapping("/password/forgot")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        passwordResetService.requestReset(request.email)
        return ResponseEntity.noContent().build()
    }

    /** Complete a password reset using the token from the email link. */
    @PostMapping("/password/reset")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        passwordResetService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.noContent().build()
    }

    /** Consume the verification token from the email link. Always 204 on success. */
    @PostMapping("/email/verify")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): ResponseEntity<Void> {
        emailVerificationService.verifyEmail(request.token)
        return ResponseEntity.noContent().build()
    }

    /** Resend a verification email. Always 204 (no account enumeration). */
    @PostMapping("/email/resend")
    fun resendVerification(@Valid @RequestBody request: ResendVerificationRequest): ResponseEntity<Void> {
        emailVerificationService.resendVerification(request.email)
        return ResponseEntity.noContent().build()
    }
}
