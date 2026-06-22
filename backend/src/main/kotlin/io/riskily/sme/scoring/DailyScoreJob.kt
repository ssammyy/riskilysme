package io.riskily.sme.scoring

import io.riskily.sme.alert.AlertService
import io.riskily.sme.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Runs every day at 06:00 EAT (Africa/Nairobi) — after market data refresh windows.
 * Recalculates the Business Health Score for every user who has completed onboarding,
 * appends a row to score_history, then generates alerts from the new scores.
 * Per-user failures are swallowed and logged so one bad profile never aborts the whole run.
 */
@Component
class DailyScoreJob(
    private val users: UserRepository,
    private val scoreService: ScoreService,
    private val alertService: AlertService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 6 * * *", zone = "Africa/Nairobi")
    fun recalculateAll() {
        val candidates = users.findAllByOnboardingCompletedAtIsNotNull()
        log.info("DailyScoreJob: starting recalculation for {} users", candidates.size)
        var succeeded = 0
        var failed = 0
        candidates.forEach { user ->
            runCatching {
                val score = scoreService.recalculateAndPersist(user)
                alertService.generateAlertsForUser(user, score)
            }
                .onSuccess { succeeded++ }
                .onFailure { ex ->
                    failed++
                    log.error("DailyScoreJob: recalc/alert failed for user {} ({})", user.id, user.email, ex)
                }
        }
        log.info("DailyScoreJob: done — {} succeeded, {} failed", succeeded, failed)
    }
}
