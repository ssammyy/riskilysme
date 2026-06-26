package io.riskily.sme.regulatory

import io.riskily.sme.user.User
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.time.LocalDate

data class ReminderDeadline(
    val title: String,
    val authority: String,
    val nextDueDate: LocalDate,
    val daysRemaining: Long,
)

interface DeadlineReminderMailer {
    fun sendReminder(user: User, deadlines: List<ReminderDeadline>)
}

@Component
class SmtpDeadlineReminderMailer(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.from:hello@riskily.africa}") private val from: String,
    @Value("\${spring.mail.password:}") private val mailPassword: String,
    @Value("\${riskily.app.base-url:http://localhost:5173}") private val baseUrl: String,
) : DeadlineReminderMailer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendReminder(user: User, deadlines: List<ReminderDeadline>) {
        if (mailPassword.isBlank()) {
            log.info(
                "[deadline-reminder-skipped] mail not configured — to={} count={}",
                user.email, deadlines.size,
            )
            return
        }
        val name = user.businessName ?: user.firstName ?: "there"
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            MimeMessageHelper(message, true, "UTF-8").apply {
                setFrom(from, "Riskily SME")
                setTo(user.email)
                setSubject(buildSubject(deadlines))
                setText(buildHtml(name, deadlines), true)
            }
            mailSender.send(message)
            log.info("[deadline-reminder-sent] to={} count={}", user.email, deadlines.size)
        } catch (ex: Exception) {
            log.error("[deadline-reminder-failed] to={} error={}", user.email, ex.message, ex)
        }
    }

    private fun buildSubject(deadlines: List<ReminderDeadline>): String {
        val urgentDays = deadlines.minOf { it.daysRemaining }
        return when {
            urgentDays == 0L -> "Action today — regulatory deadline due"
            urgentDays == 1L -> "Reminder — regulatory deadline due tomorrow"
            urgentDays <= 7L -> "Reminder — regulatory deadline in $urgentDays days"
            else             -> "Upcoming regulatory deadlines — Riskily SME"
        }
    }

    private fun buildHtml(name: String, deadlines: List<ReminderDeadline>): String {
        val cards = deadlines.joinToString("") { d ->
            val urgencyColor = when {
                d.daysRemaining <= 3  -> "#dc2626"   // red
                d.daysRemaining <= 7  -> "#d97706"   // amber
                else                  -> "#ff4f00"   // brand orange
            }
            val daysLabel = when (d.daysRemaining) {
                0L   -> "Due today"
                1L   -> "Due tomorrow"
                else -> "Due in ${d.daysRemaining} days"
            }
            val dateStr = d.nextDueDate.toString() // ISO, e.g. 2026-07-09
            """
            <div style="background:#fffefb;border:1px solid #e8e0df;border-radius:12px;
                        padding:20px 24px;margin-bottom:14px;">
              <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;">
                <div>
                  <p style="margin:0 0 4px;font-size:11px;font-weight:700;color:#ff4f00;
                             letter-spacing:1px;text-transform:uppercase;">${d.authority}</p>
                  <p style="margin:0 0 6px;font-size:15px;font-weight:700;color:#201515;
                             line-height:1.3;">${d.title}</p>
                  <p style="margin:0;font-size:12px;color:#939084;">Due $dateStr</p>
                </div>
                <span style="display:inline-block;background:${urgencyColor}1a;color:$urgencyColor;
                             border:1px solid ${urgencyColor}40;border-radius:9999px;
                             padding:4px 12px;font-size:11px;font-weight:700;
                             white-space:nowrap;flex-shrink:0;">$daysLabel</span>
              </div>
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;padding:0;background:#f8f4f0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
          <table width="100%" cellpadding="0" cellspacing="0" style="padding:32px 16px;">
            <tr><td align="center">
              <table width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%;">

                <!-- Header -->
                <tr><td style="background:#201515;border-radius:12px 12px 0 0;padding:24px 28px;">
                  <p style="margin:0;font-size:13px;font-weight:700;color:#ff4f00;letter-spacing:1px;
                             text-transform:uppercase;">Riskily SME</p>
                  <p style="margin:6px 0 0;font-size:20px;font-weight:700;color:#fffefb;line-height:1.3;">
                    Upcoming deadlines, $name</p>
                  <p style="margin:6px 0 0;font-size:14px;color:#c5c0b1;line-height:1.5;">
                    You have ${deadlines.size} regulatory deadline${if (deadlines.size == 1) "" else "s"} coming up.
                    Mark them done when you're sorted.</p>
                </td></tr>

                <!-- Deadline cards -->
                <tr><td style="background:#f8f4f0;padding:24px 28px 8px;">
                  $cards
                </td></tr>

                <!-- CTA -->
                <tr><td style="background:#f8f4f0;padding:0 28px 28px;text-align:center;">
                  <table cellpadding="0" cellspacing="0" style="margin:0 auto;">
                    <tr><td style="background:#ff4f00;border-radius:12px;text-align:center;">
                      <a href="$baseUrl/dashboard"
                         style="display:inline-block;padding:13px 28px;font-size:15px;font-weight:700;
                                color:#fffefb;text-decoration:none;">
                        Go to my dashboard
                      </a>
                    </td></tr>
                  </table>
                </td></tr>

                <!-- Footer -->
                <tr><td style="background:#fffefb;border-radius:0 0 12px 12px;border-top:1px solid #e8e0df;
                               padding:16px 28px;text-align:center;">
                  <p style="margin:0;font-size:11px;color:#939084;line-height:1.6;">
                    You receive these reminders because you have deadlines set to <strong>Remind me</strong>
                    in Riskily SME.&nbsp;·&nbsp;
                    <a href="$baseUrl/dashboard" style="color:#939084;">Manage on dashboard</a>
                    &nbsp;·&nbsp;
                    <a href="mailto:hello@riskily.africa" style="color:#939084;">hello@riskily.africa</a>
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
