package io.riskily.sme.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationToken(

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

    fun isUsable(): Boolean = usedAt == null && Instant.now().isBefore(expiresAt)
}
