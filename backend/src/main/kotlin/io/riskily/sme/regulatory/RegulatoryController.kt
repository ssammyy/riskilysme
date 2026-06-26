package io.riskily.sme.regulatory

import io.riskily.sme.auth.AppUserDetails
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

data class DeadlineResponse(
    val id: Long,
    val title: String,
    val titleSw: String,
    val description: String?,
    val descriptionSw: String?,
    val authority: String,
    val nextDueDate: LocalDate,
    val daysRemaining: Long,
    /** Null when the caller is unauthenticated. Defaults to REMIND_ME when authenticated but no row exists. */
    val userStatus: String?,
)

data class UpdateDeadlineStatusRequest(val status: String)

@RestController
@RequestMapping("/api/regulatory")
class RegulatoryController(
    private val notices: RegulatoryNoticeRepository,
    private val statusRepo: UserDeadlineStatusRepository,
) {

    /**
     * GET /api/regulatory/deadlines
     * Public — no auth required. When a JWT is present, populates userStatus per deadline.
     */
    @GetMapping("/deadlines")
    fun deadlines(
        @AuthenticationPrincipal principal: AppUserDetails?,
    ): List<DeadlineResponse> {
        val today = LocalDate.now()

        val statusByNoticeId: Map<Long, DeadlineStatus> = if (principal != null) {
            statusRepo.findByUserId(principal.id)
                .associateBy({ it.regulatoryNoticeId }, { it.status })
        } else {
            emptyMap()
        }

        return notices.findByIsActiveTrue()
            .map { notice ->
                val nextDue = nextDueDate(notice, today)
                val userStatus = if (principal != null) {
                    (statusByNoticeId[notice.id!!] ?: DeadlineStatus.REMIND_ME).name
                } else {
                    null
                }
                DeadlineResponse(
                    id            = notice.id!!,
                    title         = notice.title,
                    titleSw       = notice.titleSw,
                    description   = notice.description,
                    descriptionSw = notice.descriptionSw,
                    authority     = notice.authority,
                    nextDueDate   = nextDue,
                    daysRemaining = today.until(nextDue, java.time.temporal.ChronoUnit.DAYS),
                    userStatus    = userStatus,
                )
            }
            .sortedBy { it.daysRemaining }
    }

    /**
     * PUT /api/regulatory/deadlines/{id}/status
     * Requires authentication. Upserts the user's action state for one deadline.
     */
    @PutMapping("/deadlines/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateDeadlineStatusRequest,
        @AuthenticationPrincipal principal: AppUserDetails,
    ) {
        val newStatus = runCatching { DeadlineStatus.valueOf(request.status) }
            .getOrElse { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: ${request.status}") }

        if (!notices.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Deadline not found")
        }

        val row = statusRepo.findByUserIdAndRegulatoryNoticeId(principal.id, id)
            .orElseGet { UserDeadlineStatus(userId = principal.id, regulatoryNoticeId = id) }

        row.status = newStatus
        statusRepo.save(row)
    }

    // -------------------------------------------------------------------------

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
        val clampedDay = day.coerceAtMost(adjusted.lengthOfMonth())
        return adjusted.withDayOfMonth(clampedDay)
    }
}
