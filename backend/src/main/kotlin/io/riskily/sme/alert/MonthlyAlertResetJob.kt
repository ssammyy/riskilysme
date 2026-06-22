package io.riskily.sme.alert

import io.riskily.sme.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Resets [io.riskily.sme.user.User.monthlyAlertCount] to 0 for every user on the 1st of each
 * month at 00:05 EAT. This is the authoritative reset — the alert cap window is calendar-month.
 */
@Component
class MonthlyAlertResetJob(private val users: UserRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 5 0 1 * *", zone = "Africa/Nairobi")
    @Transactional
    fun resetAllCounts() {
        val all = users.findAll()
        all.forEach { it.monthlyAlertCount = 0 }
        users.saveAll(all)
        log.info("MonthlyAlertResetJob: reset monthly_alert_count to 0 for {} user(s)", all.size)
    }
}
