package io.riskily.sme.regulatory

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
)

@RestController
@RequestMapping("/api/regulatory")
class RegulatoryController(
    private val notices: RegulatoryNoticeRepository,
) {

    /**
     * GET /api/regulatory/deadlines
     * Public endpoint — no auth required.
     * Returns active regulatory obligations with their computed next due date and days remaining.
     * Sorted nearest-first.
     */
    @GetMapping("/deadlines")
    fun deadlines(): List<DeadlineResponse> {
        val today = LocalDate.now()
        return notices.findByIsActiveTrue()
            .map { notice ->
                val nextDue = nextDueDate(notice, today)
                DeadlineResponse(
                    id             = notice.id!!,
                    title          = notice.title,
                    titleSw        = notice.titleSw,
                    description    = notice.description,
                    descriptionSw  = notice.descriptionSw,
                    authority      = notice.authority,
                    nextDueDate    = nextDue,
                    daysRemaining  = today.until(nextDue, java.time.temporal.ChronoUnit.DAYS),
                )
            }
            .sortedBy { it.daysRemaining }
    }

    /**
     * Computes the next calendar occurrence of a regulatory notice relative to [today].
     * - Monthly obligations: next occurrence of [recurringDayOfMonth] (this month if not yet passed,
     *   otherwise next month).
     * - Annual obligations: next occurrence of [recurringDayOfMonth] in [recurringMonth] (this year
     *   if still in the future, otherwise next year).
     */
    private fun nextDueDate(notice: RegulatoryNotice, today: LocalDate): LocalDate {
        val day = notice.recurringDayOfMonth
        val month = notice.recurringMonth

        return if (month == null) {
            // Monthly — try this month first
            val thisMonth = safeDate(today.year, today.monthValue, day)
            if (!thisMonth.isBefore(today)) thisMonth
            else safeDate(today.year, today.monthValue + 1, day)
        } else {
            // Annual — try this year first
            val thisYear = safeDate(today.year, month, day)
            if (!thisYear.isBefore(today)) thisYear
            else safeDate(today.year + 1, month, day)
        }
    }

    /**
     * Creates a [LocalDate] clamping the day to the last valid day in that month.
     * Handles month overflow (month=13 → January next year) via [LocalDate.of] with year/month
     * arithmetic.
     */
    private fun safeDate(year: Int, month: Int, day: Int): LocalDate {
        val adjusted = LocalDate.of(year, 1, 1)
            .plusMonths(month.toLong() - 1)
        val clampedDay = day.coerceAtMost(adjusted.lengthOfMonth())
        return adjusted.withDayOfMonth(clampedDay)
    }
}
