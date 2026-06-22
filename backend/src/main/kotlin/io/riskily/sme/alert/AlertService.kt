package io.riskily.sme.alert

import io.riskily.sme.scoring.ModuleCode
import io.riskily.sme.scoring.ScoreResponse
import io.riskily.sme.user.SubscriptionTier
import io.riskily.sme.user.User
import io.riskily.sme.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AlertService(
    private val alertRepo: AlertRepository,
    private val userRepo: UserRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        /** Basic users: cap at 3 URGENT/ACT_NOW alerts per calendar month. */
        private const val BASIC_MONTHLY_CAP = 3

        // Thresholds (module health score) for each severity level.
        private const val URGENT_THRESHOLD = 30
        private const val ACT_NOW_THRESHOLD = 55
        private const val WATCH_OUT_THRESHOLD = 75
    }

    /**
     * Derives alerts from [score], applies tier-based routing, and persists new alerts.
     * Already-alerted modules in the same calendar month are skipped to avoid re-spamming.
     * Called by [io.riskily.sme.scoring.DailyScoreJob] after each successful recalculation.
     */
    @Transactional
    fun generateAlertsForUser(user: User, score: ScoreResponse) {
        val userId = user.id ?: return
        val monthKey = YearMonth.now().toString()   // e.g. "2026-06"

        // Modules already alerted this month — skip to avoid duplicates.
        val alreadyAlerted: Set<String> = alertRepo
            .findByUserIdAndMonthKey(userId, monthKey)
            .mapNotNull { it.moduleCode?.name }
            .toSet()

        // Build candidate alerts from module scores.
        val candidates: List<CandidateAlert> = score.modules
            .mapNotNull { m ->
                val severity = when {
                    m.health < URGENT_THRESHOLD    -> AlertSeverity.URGENT
                    m.health < ACT_NOW_THRESHOLD   -> AlertSeverity.ACT_NOW
                    m.health < WATCH_OUT_THRESHOLD -> AlertSeverity.WATCH_OUT
                    else                           -> null
                } ?: return@mapNotNull null

                if (m.code.uppercase() in alreadyAlerted) return@mapNotNull null

                CandidateAlert(
                    severity   = severity,
                    moduleCode = runCatching { ModuleCode.valueOf(m.code.uppercase()) }.getOrNull(),
                    title      = buildTitle(severity, m.code),
                    body       = buildBody(severity, m.code),
                )
            }
            .sortedBy { it.severity.ordinal }   // URGENT first

        // Apply tier routing.
        val toRoute: List<CandidateAlert> = when (user.subscriptionTier) {
            SubscriptionTier.STANDARD -> candidates

            SubscriptionTier.BASIC -> {
                val usedThisMonth = alertRepo.countByUserIdAndMonthKey(userId, monthKey).toInt()
                val remaining = (BASIC_MONTHLY_CAP - usedThisMonth).coerceAtLeast(0)
                candidates
                    .filter { it.severity in listOf(AlertSeverity.URGENT, AlertSeverity.ACT_NOW) }
                    .take(remaining)
            }
        }

        if (toRoute.isEmpty()) return

        toRoute.forEach { c ->
            alertRepo.save(
                Alert(
                    userId     = userId,
                    severity   = c.severity,
                    moduleCode = c.moduleCode,
                    title      = c.title,
                    body       = c.body,
                    monthKey   = monthKey,
                )
            )
        }

        // Keep users.monthly_alert_count in sync (used for "X of 3 used" UI).
        user.monthlyAlertCount = alertRepo.countByUserIdAndMonthKey(userId, monthKey).toInt()
        userRepo.save(user)

        log.info(
            "AlertService: generated {} alert(s) for user {} (tier={}, month={})",
            toRoute.size, userId, user.subscriptionTier, monthKey,
        )
    }

    // -------------------------------------------------------------------------
    // Text generation — English only for Phase 1; SW localisation via frontend.
    // -------------------------------------------------------------------------

    private fun buildTitle(severity: AlertSeverity, code: String): String {
        val module = moduleDisplayName(code)
        return when (severity) {
            AlertSeverity.URGENT    -> "⚠️ Critical risk: $module"
            AlertSeverity.ACT_NOW   -> "Action needed: $module"
            AlertSeverity.WATCH_OUT -> "Watch out: $module"
        }
    }

    private fun buildBody(severity: AlertSeverity, code: String): String = when (code.uppercase()) {
        "FX"           -> "Exchange rate movements are impacting your cost base. Review dollar-denominated payments."
        "LIQUIDITY"    -> "Your cash flow position needs attention. Ensure your float covers fixed costs for the next 30 days."
        "COUNTERPARTY" -> "Customer or supplier risk is elevated. Chase outstanding invoices and confirm supplier reliability."
        "COMMODITY"    -> "Commodity price pressure is high. Consider locking in stock purchases before the next price rise."
        "CREDIT"       -> "Loan and credit exposure is elevated. Confirm repayment dates and protect working capital."
        "REGULATORY"   -> "A compliance deadline is approaching. Review your KRA iTax calendar and upcoming filing obligations."
        "MACRO"        -> "Broader economic conditions are unfavourable. Monitor CBK rate announcements and refinancing options."
        else           -> "Review this risk area and take appropriate action."
    }.let { base ->
        if (severity == AlertSeverity.URGENT) "Immediate action required. $base" else base
    }

    private fun moduleDisplayName(code: String): String = when (code.uppercase()) {
        "FX"           -> "Foreign Exchange"
        "LIQUIDITY"    -> "Cash Flow"
        "COUNTERPARTY" -> "Customers & Suppliers"
        "COMMODITY"    -> "Commodity Prices"
        "CREDIT"       -> "Credit & Loans"
        "REGULATORY"   -> "Compliance"
        "MACRO"        -> "Economic Conditions"
        else           -> code
    }
}

/** Internal transfer object — not persisted directly. */
private data class CandidateAlert(
    val severity: AlertSeverity,
    val moduleCode: ModuleCode?,
    val title: String,
    val body: String,
)
