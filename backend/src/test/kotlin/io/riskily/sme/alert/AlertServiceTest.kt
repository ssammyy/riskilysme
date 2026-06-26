package io.riskily.sme.alert

import io.riskily.sme.AbstractIntegrationTest
import io.riskily.sme.scoring.ModuleCode
import io.riskily.sme.scoring.ModuleScoreDto
import io.riskily.sme.scoring.ScoreResponse
import io.riskily.sme.user.SubscriptionTier
import io.riskily.sme.user.User
import io.riskily.sme.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.YearMonth

@Transactional
class AlertServiceTest : AbstractIntegrationTest() {

    @Autowired lateinit var alertService: AlertService
    @Autowired lateinit var alertRepo: AlertRepository
    @Autowired lateinit var userRepo: UserRepository

    private val monthKey = YearMonth.now().toString()

    private fun createUser(tier: SubscriptionTier, email: String): User =
        userRepo.save(User(email = email, passwordHash = "test-hash", subscriptionTier = tier))

    private fun scoreWith(vararg modules: Pair<String, Int>) = ScoreResponse(
        overallHealth = 50,
        overallBand = "watch_out",
        modules = modules.map { (code, health) ->
            ModuleScoreDto(
                code = code, exposure = 0.5, pressure = 0.5, health = health,
                band = "watch_out", dataConfidence = "profile", isProvisional = false,
            )
        },
    )

    @Test
    fun `standard tier routes all severities without cap`() {
        val user = createUser(SubscriptionTier.STANDARD, "std@alerts.test")
        val score = scoreWith(
            "FX" to 20,           // URGENT
            "LIQUIDITY" to 20,    // URGENT
            "COUNTERPARTY" to 45, // ACT_NOW
            "COMMODITY" to 45,    // ACT_NOW
            "CREDIT" to 65,       // WATCH_OUT
            "REGULATORY" to 65,   // WATCH_OUT
            "MACRO" to 80,        // ALL_GOOD — no alert
        )

        alertService.generateAlertsForUser(user, score)

        val alerts = alertRepo.findByUserIdAndMonthKey(user.id!!, monthKey)
        assertThat(alerts).hasSize(6)
        assertThat(alerts.map { it.severity })
            .contains(AlertSeverity.URGENT, AlertSeverity.ACT_NOW, AlertSeverity.WATCH_OUT)
    }

    @Test
    fun `basic tier cap enforced at 3 and counter synced`() {
        val user = createUser(SubscriptionTier.BASIC, "basic-cap@alerts.test")
        val score = scoreWith(
            "FX" to 20,
            "LIQUIDITY" to 20,
            "COUNTERPARTY" to 20,
            "COMMODITY" to 20, // 4 URGENT — only 3 should route
        )

        alertService.generateAlertsForUser(user, score)

        val alerts = alertRepo.findByUserIdAndMonthKey(user.id!!, monthKey)
        assertThat(alerts).hasSize(3)
        assertThat(user.monthlyAlertCount).isEqualTo(3)
    }

    @Test
    fun `basic tier excludes watch_out severity`() {
        val user = createUser(SubscriptionTier.BASIC, "basic-watch@alerts.test")
        val score = scoreWith(
            "FX" to 20,           // URGENT — routes
            "LIQUIDITY" to 65,    // WATCH_OUT — excluded for Basic
            "COUNTERPARTY" to 65, // WATCH_OUT — excluded for Basic
        )

        alertService.generateAlertsForUser(user, score)

        val alerts = alertRepo.findByUserIdAndMonthKey(user.id!!, monthKey)
        assertThat(alerts).hasSize(1)
        assertThat(alerts.single().severity).isEqualTo(AlertSeverity.URGENT)
    }

    @Test
    fun `basic tier routes remaining slot when 2 of 3 already used`() {
        val user = createUser(SubscriptionTier.BASIC, "basic-partial@alerts.test")
        alertRepo.save(Alert(userId = user.id!!, severity = AlertSeverity.URGENT,
            moduleCode = ModuleCode.FX, title = "t", body = "b", monthKey = monthKey))
        alertRepo.save(Alert(userId = user.id!!, severity = AlertSeverity.URGENT,
            moduleCode = ModuleCode.LIQUIDITY, title = "t", body = "b", monthKey = monthKey))

        val score = scoreWith(
            "COUNTERPARTY" to 20,
            "COMMODITY" to 20,
            "CREDIT" to 20,
        )

        alertService.generateAlertsForUser(user, score)

        val all = alertRepo.findByUserIdAndMonthKey(user.id!!, monthKey)
        assertThat(all).hasSize(3) // 2 seeded + 1 new
    }

    @Test
    fun `already-alerted module is skipped within the same month`() {
        val user = createUser(SubscriptionTier.BASIC, "basic-dedup@alerts.test")
        alertRepo.save(Alert(userId = user.id!!, severity = AlertSeverity.URGENT,
            moduleCode = ModuleCode.FX, title = "t", body = "b", monthKey = monthKey))

        val score = scoreWith(
            "FX" to 20,        // URGENT but already alerted this month — skip
            "LIQUIDITY" to 45, // ACT_NOW — routes
        )

        alertService.generateAlertsForUser(user, score)

        val alerts = alertRepo.findByUserIdAndMonthKey(user.id!!, monthKey)
        assertThat(alerts).hasSize(2) // 1 seeded FX + 1 new LIQUIDITY
        assertThat(alerts.count { it.moduleCode == ModuleCode.FX }).isEqualTo(1)
        assertThat(alerts.any { it.moduleCode == ModuleCode.LIQUIDITY }).isTrue()
    }
}
