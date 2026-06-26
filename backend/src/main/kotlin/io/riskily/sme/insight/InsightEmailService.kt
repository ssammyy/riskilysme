package io.riskily.sme.insight

import io.riskily.sme.user.User
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

interface InsightEmailService {
    fun sendDailyDigest(user: User, insights: List<Insight>)
}

@Component
class SmtpInsightEmailService(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.from:hello@riskily.africa}") private val from: String,
    @Value("\${spring.mail.password:}") private val mailPassword: String,
) : InsightEmailService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendDailyDigest(user: User, insights: List<Insight>) {
        if (mailPassword.isBlank()) {
            log.info("[insight-email-skipped] RESEND_API_KEY not set — to={} count={}", user.email, insights.size)
            return
        }
        val name = user.businessName ?: user.firstName ?: "there"
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            MimeMessageHelper(message, true, "UTF-8").apply {
                setFrom(from, "Riskily SME")
                setTo(user.email)
                setSubject("Your daily business insights — $name")
                setText(buildHtml(name, insights), true)
            }
            mailSender.send(message)
            log.info("[insight-email-sent] to={} count={}", user.email, insights.size)
        } catch (ex: Exception) {
            log.error("[insight-email-failed] to={} error={}", user.email, ex.message, ex)
        }
    }

    private fun buildHtml(name: String, insights: List<Insight>): String {
        val cards = insights.joinToString("") { insight ->
            val badge = insight.moduleCode?.let {
                """<span style="display:inline-block;background:#fff3ed;color:#ff4f00;border:1px solid #ffd5c2;
                   border-radius:4px;padding:2px 8px;font-size:11px;font-weight:700;letter-spacing:.5px;
                   margin-bottom:10px;">$it</span>"""
            } ?: ""
            val action = insight.actionText?.let {
                """<p style="margin:10px 0 0;font-size:13px;color:#ff4f00;">
                   <strong>Action:</strong> $it</p>"""
            } ?: ""
            """
            <div style="background:#fff;border:1px solid #e8e0df;border-radius:10px;
                        padding:18px 20px;margin-bottom:14px;">
              $badge
              <p style="margin:0 0 8px;font-size:15px;font-weight:700;color:#201515;
                        line-height:1.3;">${insight.title}</p>
              <p style="margin:0;font-size:13px;color:#6b5e5e;line-height:1.6;">${insight.body}</p>
              $action
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;padding:0;background:#f7f3f2;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
          <table width="100%" cellpadding="0" cellspacing="0" style="padding:32px 16px;">
            <tr><td align="center">
              <table width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%;">

                <!-- Header -->
                <tr><td style="background:#201515;border-radius:12px 12px 0 0;padding:24px 28px;">
                  <p style="margin:0;font-size:13px;font-weight:700;color:#ff4f00;letter-spacing:1px;
                             text-transform:uppercase;">Riskily SME</p>
                  <p style="margin:6px 0 0;font-size:20px;font-weight:700;color:#fff;">
                    Your daily insights, $name</p>
                </td></tr>

                <!-- Cards -->
                <tr><td style="background:#f7f3f2;padding:20px 28px;">
                  $cards
                </td></tr>

                <!-- Footer -->
                <tr><td style="background:#fff;border-radius:0 0 12px 12px;border-top:1px solid #e8e0df;
                               padding:16px 28px;text-align:center;">
                  <p style="margin:0;font-size:11px;color:#9b8f8f;line-height:1.6;">
                    Updated daily at 06:00 EAT &nbsp;·&nbsp;
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
}
