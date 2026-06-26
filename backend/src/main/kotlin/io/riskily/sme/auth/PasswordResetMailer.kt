package io.riskily.sme.auth

import io.riskily.sme.user.User
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/** Sends password-reset emails. */
interface PasswordResetMailer {
    fun sendPasswordReset(user: User, rawToken: String)
}

/**
 * Sends a password-reset email via Resend SMTP when RESEND_API_KEY is set.
 * Falls back to console logging when the key is absent (dev / CI).
 */
@Component
class SmtpPasswordResetMailer(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.from:hello@riskily.africa}") private val from: String,
    @Value("\${spring.mail.password:}") private val mailPassword: String,
    @Value("\${riskily.app.base-url:http://localhost:5173}") private val baseUrl: String,
) : PasswordResetMailer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendPasswordReset(user: User, rawToken: String) {
        val link = "$baseUrl/reset?token=$rawToken"
        if (mailPassword.isBlank()) {
            log.info("[password-reset-skipped] lang={} to={} link={}", user.language.code, user.email, link)
            return
        }
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            MimeMessageHelper(message, true, "UTF-8").apply {
                setFrom(from, "Riskily SME")
                setTo(user.email)
                setSubject("Reset your Riskily password")
                setText(buildHtml(link), true)
            }
            mailSender.send(message)
            log.info("[password-reset-sent] to={}", user.email)
        } catch (ex: Exception) {
            log.error("[password-reset-failed] to={} error={}", user.email, ex.message, ex)
        }
    }

    private fun buildHtml(link: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;padding:0;background:#f7f3f2;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
          <table width="100%" cellpadding="0" cellspacing="0" style="padding:32px 16px;">
            <tr><td align="center">
              <table width="480" cellpadding="0" cellspacing="0" style="max-width:480px;width:100%;">

                <!-- Header -->
                <tr><td style="background:#201515;border-radius:12px 12px 0 0;padding:24px 28px;">
                  <p style="margin:0;font-size:13px;font-weight:700;color:#ff4f00;letter-spacing:1px;
                             text-transform:uppercase;">Riskily SME</p>
                  <p style="margin:6px 0 0;font-size:20px;font-weight:700;color:#fff;">
                    Reset your password</p>
                </td></tr>

                <!-- Body -->
                <tr><td style="background:#fff;padding:28px;">
                  <p style="margin:0 0 20px;font-size:14px;color:#4a3f3f;line-height:1.6;">
                    We received a request to reset the password for your Riskily account.
                    Click the button below to choose a new password.
                  </p>
                  <table cellpadding="0" cellspacing="0" style="margin:0 auto 24px;">
                    <tr><td style="background:#ff4f00;border-radius:8px;text-align:center;">
                      <a href="$link"
                         style="display:inline-block;padding:13px 28px;font-size:14px;font-weight:700;
                                color:#fff;text-decoration:none;">
                        Reset password
                      </a>
                    </td></tr>
                  </table>
                  <p style="margin:0;font-size:12px;color:#9b8f8f;line-height:1.6;">
                    This link expires in 1 hour. If you didn't request a password reset,
                    you can safely ignore this email.
                  </p>
                </td></tr>

                <!-- Footer -->
                <tr><td style="background:#f7f3f2;border-radius:0 0 12px 12px;border-top:1px solid #e8e0df;
                               padding:16px 28px;text-align:center;">
                  <p style="margin:0;font-size:11px;color:#9b8f8f;">
                    <a href="mailto:hello@riskily.africa" style="color:#9b8f8f;">hello@riskily.africa</a>
                  </p>
                </td></tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
