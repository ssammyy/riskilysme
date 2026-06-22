package io.riskily.sme.alert

import io.riskily.sme.auth.AppUserDetails
import io.riskily.sme.user.SubscriptionTier
import java.time.Instant
import java.time.YearMonth
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

data class AlertResponse(
    val id: Long,
    val severity: String,
    val moduleCode: String?,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: Instant,
)

data class AlertsPageResponse(
    val alerts: List<AlertResponse>,
    val monthlyUsed: Long,
    val monthlyCap: Int,          // 3 for BASIC, Int.MAX_VALUE (sentinel) for STANDARD
)

@RestController
@RequestMapping("/api/alerts")
class AlertController(
    private val alertRepo: AlertRepository,
) {

    /** GET /api/alerts/me — all alerts for the authenticated user, most-critical first. */
    @GetMapping("/me")
    fun myAlerts(@AuthenticationPrincipal principal: AppUserDetails): AlertsPageResponse {
        val userId = principal.id
        val monthKey = YearMonth.now().toString()
        val alerts = alertRepo.findByUserIdSeverityOrdered(userId)
        val monthlyUsed = alertRepo.countByUserIdAndMonthKey(userId, monthKey)
        val isStandard = principal.user.subscriptionTier == SubscriptionTier.STANDARD
        return AlertsPageResponse(
            alerts      = alerts.map { it.toResponse() },
            monthlyUsed = monthlyUsed,
            monthlyCap  = if (isStandard) Int.MAX_VALUE else 3,
        )
    }

    /** POST /api/alerts/{id}/read — marks a single alert as read. */
    @PostMapping("/{id}/read")
    fun markRead(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: AppUserDetails,
    ): AlertResponse {
        val alert = alertRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (alert.userId != principal.id)
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        alert.isRead = true
        alertRepo.save(alert)
        return alert.toResponse()
    }

    private fun Alert.toResponse() = AlertResponse(
        id         = id!!,
        severity   = severity.name,
        moduleCode = moduleCode?.name,
        title      = title,
        body       = body,
        isRead     = isRead,
        createdAt  = createdAt,
    )
}
