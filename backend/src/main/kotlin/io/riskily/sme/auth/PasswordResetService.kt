package io.riskily.sme.auth

import io.riskily.sme.user.PasswordResetToken
import io.riskily.sme.user.PasswordResetTokenRepository
import io.riskily.sme.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class PasswordResetService(
    private val users: UserRepository,
    private val tokens: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailer: PasswordResetMailer,
) {
    private val random = SecureRandom()
    private val tokenTtlHours = 1L

    /**
     * Start a reset. Silently no-ops for unknown emails (no account enumeration).
     * Always behaves the same from the caller's perspective.
     */
    @Transactional
    fun requestReset(email: String) {
        val user = users.findByEmailIgnoreCase(email).orElse(null) ?: return
        val rawToken = generateRawToken()
        tokens.save(
            PasswordResetToken(
                userId = user.id!!,
                tokenHash = sha256(rawToken),
                expiresAt = Instant.now().plus(tokenTtlHours, ChronoUnit.HOURS),
            ),
        )
        mailer.sendPasswordReset(user, rawToken)
    }

    /** Complete a reset with the raw token from the email link. */
    @Transactional
    fun resetPassword(rawToken: String, newPassword: String) {
        val token = tokens.findByTokenHash(sha256(rawToken))
            .filter { it.isUsable() }
            .orElseThrow { InvalidTokenException("Reset link is invalid or has expired") }
        val user = users.findById(token.userId)
            .orElseThrow { InvalidTokenException("User no longer exists") }
        user.passwordHash = passwordEncoder.encode(newPassword)
        user.emailVerified = true  // clicking a reset link proves email ownership
        users.save(user)
        token.usedAt = Instant.now()
        tokens.save(token)
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
