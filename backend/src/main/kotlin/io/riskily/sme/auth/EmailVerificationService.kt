package io.riskily.sme.auth

import io.riskily.sme.user.EmailVerificationToken
import io.riskily.sme.user.EmailVerificationTokenRepository
import io.riskily.sme.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class EmailVerificationService(
    private val users: UserRepository,
    private val tokens: EmailVerificationTokenRepository,
    private val mailer: EmailVerificationMailer,
) {
    private val random = SecureRandom()
    private val tokenTtlHours = 24L

    /** Send (or resend) a verification email. Safe to call multiple times. */
    @Transactional
    fun sendVerification(user: io.riskily.sme.user.User) {
        if (user.emailVerified) return
        val rawToken = generateRawToken()
        tokens.save(
            EmailVerificationToken(
                userId = user.id!!,
                tokenHash = sha256(rawToken),
                expiresAt = Instant.now().plus(tokenTtlHours, ChronoUnit.HOURS),
            ),
        )
        mailer.sendVerification(user, rawToken)
    }

    /**
     * Consume the raw token from the email link.
     * Sets [User.emailVerified] = true and marks the token used.
     * Throws [InvalidTokenException] if the token is unknown, expired, or already used.
     */
    @Transactional
    fun verifyEmail(rawToken: String) {
        val token = tokens.findByTokenHash(sha256(rawToken))
            .filter { it.isUsable() }
            .orElseThrow { InvalidTokenException("Verification link is invalid or has expired") }
        val user = users.findById(token.userId)
            .orElseThrow { InvalidTokenException("User no longer exists") }
        user.emailVerified = true
        users.save(user)
        token.usedAt = Instant.now()
        tokens.save(token)
    }

    /**
     * Trigger a resend for the given email.
     * Silently no-ops for unknown or already-verified emails (no account enumeration).
     */
    @Transactional
    fun resendVerification(email: String) {
        val user = users.findByEmailIgnoreCase(email).orElse(null) ?: return
        if (user.emailVerified) return
        sendVerification(user)
    }

    private fun generateRawToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
