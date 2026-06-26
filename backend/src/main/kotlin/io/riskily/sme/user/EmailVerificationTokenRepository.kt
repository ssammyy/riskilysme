package io.riskily.sme.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, Long> {
    fun findByTokenHash(hash: String): Optional<EmailVerificationToken>
}
