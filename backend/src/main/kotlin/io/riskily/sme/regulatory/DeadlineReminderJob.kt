package io.riskily.sme.regulatory

import io.riskily.sme.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Runs daily at 07:00 EAT — after DailyScoreJob (06:00).
 * For each active regulatory notice with daysRemaining <= 15, finds every onboarded user
 * whose status is REMIND_ME (explicit or by default — no row = REMIND_ME).
 * Groups all qualifying deadlines into a single digest email per user.
 */
@Component
class DeadlineReminderJob(
    private val noticeRepo: RegulatoryNoticeRepository,
    private val statusRepo: UserDeadlineStatusRepository,
    private val userRepo: UserRepository,
    private val mailer: DeadlineReminderMailer,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val REMINDER_WINDOW_DAYS = 15L
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Africa/Nairobi")
    fun sendReminders() {
        val today = LocalDate.now()

        val upcomingNotices = noticeRepo.findByIsActiveTrue()
            .map { notice -> notice to daysUntil(notice, today) }
            .filter { (_, days) -> days in 0..REMINDER_WINDOW_DAYS }

        if (upcomingNotices.isEmpty()) {
            log.info("DeadlineReminderJob: no deadlines within {} days — skipping", REMINDER_WINDOW_DAYS)
            return
        }

        val noticeIds = upcomingNotices.map { (n, _) -> n.id!! }.toSet()
        val onboardedUsers = userRepo.findAllByOnboardingCompletedAtIsNotNull()

        log.info(
            "DeadlineReminderJob: {} deadline(s) in window, {} candidate user(s)",
            upcomingNotices.size, onboardedUsers.size,
        )

        var sent = 0
        var skipped = 0

        onboardedUsers.forEach { user ->
            val userId = user.id ?: return@forEach

            // Load explicit statuses for this user, keyed by notice id.
            val explicitStatuses: Map<Long, DeadlineStatus> = statusRepo
                .findByUserId(userId)
                .filter { it.regulatoryNoticeId in noticeIds }
                .associateBy({ it.regulatoryNoticeId }, { it.status })

            // Collect deadlines where the effective status is REMIND_ME.
            val toRemind: List<ReminderDeadline> = upcomingNotices
                .filter { (notice, _) ->
                    // Missing row → default REMIND_ME; explicit DONE/IN_PROGRESS → skip.
                    val status = explicitStatuses[notice.id!!] ?: DeadlineStatus.REMIND_ME
                    status == DeadlineStatus.REMIND_ME
                }
                .map { (notice, days) ->
                    ReminderDeadline(
                        title         = notice.title,
                        authority     = notice.authority,
                        nextDueDate   = nextDueDate(notice, today),
                        daysRemaining = days,
                    )
                }
                .sortedBy { it.daysRemaining }

            if (toRemind.isEmpty()) {
                skipped++
                return@forEach
            }

            runCatching { mailer.sendReminder(user, toRemind) }
                .onSuccess { sent++ }
                .onFailure { ex ->
                    log.error(
                        "DeadlineReminderJob: failed to send to user {} ({}): {}",
                        userId, user.email, ex.message, ex,
                    )
                }
        }

        log.info("DeadlineReminderJob: done — {} sent, {} skipped (no REMIND_ME deadlines)", sent, skipped)
    }

    private fun daysUntil(notice: RegulatoryNotice, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, nextDueDate(notice, today))

    private fun nextDueDate(notice: RegulatoryNotice, today: LocalDate): LocalDate {
        val day = notice.recurringDayOfMonth
        val month = notice.recurringMonth

        return if (month == null) {
            val thisMonth = safeDate(today.year, today.monthValue, day)
            if (!thisMonth.isBefore(today)) thisMonth
            else safeDate(today.year, today.monthValue + 1, day)
        } else {
            val thisYear = safeDate(today.year, month, day)
            if (!thisYear.isBefore(today)) thisYear
            else safeDate(today.year + 1, month, day)
        }
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate {
        val adjusted = LocalDate.of(year, 1, 1).plusMonths(month.toLong() - 1)
        return adjusted.withDayOfMonth(day.coerceAtMost(adjusted.lengthOfMonth()))
    }
}
