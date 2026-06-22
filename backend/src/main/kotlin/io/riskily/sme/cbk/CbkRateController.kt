package io.riskily.sme.cbk

import io.riskily.sme.audit.AuditService
import io.riskily.sme.auth.AppUserDetails
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

data class CbkRateResponse(
    val id: Long,
    val rateType: String,
    val rateValue: BigDecimal,
    val effectiveDate: LocalDate,
    val setBy: String,
    val createdAt: Instant,
)

data class UpdateCbkRateRequest(
    val rateValue: BigDecimal,
)

@RestController
@RequestMapping("/api/cbk")
class CbkRateController(
    private val rates: CbkRateRepository,
    private val auditService: AuditService,
) {

    /**
     * GET /api/cbk/rates — public endpoint.
     * Returns the latest value for each distinct rate type.
     */
    @GetMapping("/rates")
    fun latestRates(): List<CbkRateResponse> =
        rates.findLatestForAllTypes().map { it.toResponse() }

    /**
     * PUT /api/cbk/rates/{rateType} — admin only (enforced in SecurityConfig).
     * Inserts a new row for today's effective date.
     * Audits the change with old and new values.
     */
    @PutMapping("/rates/{rateType}")
    fun updateRate(
        @PathVariable rateType: String,
        @RequestBody body: UpdateCbkRateRequest,
        @AuthenticationPrincipal principal: AppUserDetails,
    ): CbkRateResponse {
        val safeType = rateType.uppercase().trim()
        if (safeType.isBlank() || safeType.length > 40)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rate type")

        val previous = rates.findFirstByRateTypeOrderByEffectiveDateDesc(safeType)
        val oldValue = previous?.rateValue?.toPlainString()

        val saved = rates.save(
            CbkRate(
                rateType      = safeType,
                rateValue     = body.rateValue,
                effectiveDate = LocalDate.now(),
                setBy         = principal.username,
            )
        )

        val deltaPct = if (previous != null && previous.rateValue.compareTo(BigDecimal.ZERO) != 0)
            (body.rateValue - previous.rateValue)
                .divide(previous.rateValue, 6, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
            else null

        auditService.record(
            action              = "CBK_RATE_CHANGE",
            entity              = "cbk_rates",
            oldValue            = oldValue,
            newValue            = body.rateValue.toPlainString(),
            deltaPct            = deltaPct,
            performedByOverride = principal.username,
        )

        return saved.toResponse()
    }

    private fun CbkRate.toResponse() = CbkRateResponse(
        id            = id!!,
        rateType      = rateType,
        rateValue     = rateValue,
        effectiveDate = effectiveDate,
        setBy         = setBy,
        createdAt     = createdAt,
    )
}
