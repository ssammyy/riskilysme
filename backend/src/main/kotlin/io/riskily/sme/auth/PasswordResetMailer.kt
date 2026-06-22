package io.riskily.sme.auth

import io.riskily.sme.user.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** Sends password-reset emails. Templates/copy are selected by the user's language. */
interface PasswordResetMailer {
    fun sendPasswordReset(user: User, rawToken: String)
}

/**
 * Dev mailer: logs the reset link instead of sending email.
 * TODO(provider): replace with a real transactional email integration (endpoint TBD),
 * using bilingual templates keyed off user.language per the Glossary.
 */
@Component
class LoggingPasswordResetMailer : PasswordResetMailer {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendPasswordReset(user: User, rawToken: String) {
        // Frontend reset route; origin is configurable in real environments. TODO-confirm.
        val link = "http://localhost:5173/reset?token=$rawToken"
        log.info(
            "[password-reset] lang={} to={} link={}",
            user.language.code, user.email, link,
        )
    }
}
