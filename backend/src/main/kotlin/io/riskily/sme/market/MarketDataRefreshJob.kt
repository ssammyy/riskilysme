package io.riskily.sme.market

import io.riskily.sme.cbk.CbkRateRepository
import io.riskily.sme.regulatory.RegulatoryNotice
import io.riskily.sme.regulatory.RegulatoryNoticeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Runs at 05:50 EAT daily — 10 minutes before DailyScoreJob — so the scoring engine always
 * reads a fresh [MarketData] snapshot when it recalculates scores.
 *
 * Data sources:
 *  - USD_KES, CBR, FUEL_KES, UNGA_KES: latest rows from cbk_rates (admin-managed).
 *  - usdkesPrev / fuelPrev / ungaPrev / cbkPrev: yesterday's market_data row.
 *  - usdkes7dAgo: market_data row from 7 days ago.
 *  - kraDeadlineDays: nearest upcoming KRA deadline computed from regulatory_notices.
 *  - activeCircularsCount: count of all active regulatory_notices.
 *
 * If a source value is missing the job falls back to today's existing value (or a sane default)
 * so one missing rate never aborts the whole run.
 */
@Component
class MarketDataRefreshJob(
    private val marketRepo: MarketDataRepository,
    private val cbkRates: CbkRateRepository,
    private val notices: RegulatoryNoticeRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 50 5 * * *", zone = "Africa/Nairobi")
    fun refresh() {
        val today = LocalDate.now()
        log.info("MarketDataRefreshJob: starting for {}", today)
        runCatching { doRefresh(today) }
            .onSuccess { log.info("MarketDataRefreshJob: done for {}", today) }
            .onFailure { ex -> log.error("MarketDataRefreshJob: failed for {}", today, ex) }
    }

    internal fun doRefresh(today: LocalDate) {
        val usdkes  = cbkRate("USD_KES")  ?: 130.5
        val cbk     = cbkRate("CBR")      ?: 12.5
        val fuel    = cbkRate("FUEL_KES") ?: 195.0
        val unga    = cbkRate("UNGA_KES") ?: 180.0

        val yesterday  = marketRepo.findFirstBySnapshotDateBeforeOrderBySnapshotDateDesc(today)
        val sevenDaysAgo = marketRepo.findFirstBySnapshotDateBeforeOrderBySnapshotDateDesc(today.minusDays(6))

        val activeNotices = notices.findByIsActiveTrue()
        val nextKraDays = activeNotices
            .filter { it.authority == "KRA" }
            .mapNotNull { notice -> runCatching { nextDueDate(notice, today) }.getOrNull() }
            .map { ChronoUnit.DAYS.between(today, it).toInt() }
            .filter { it >= 0 }
            .minOrNull() ?: 30

        val snapshot = marketRepo.findBySnapshotDate(today) ?: MarketData(snapshotDate = today)
        snapshot.apply {
            snapshotDate         = today
            usdkesSpot           = usdkes
            usdkesPrev           = yesterday?.usdkesSpot    ?: usdkes
            usdkes7dAgo          = sevenDaysAgo?.usdkesSpot ?: usdkes
            fuelPrice            = fuel
            fuelPrev             = yesterday?.fuelPrice     ?: fuel
            ungaPrice            = unga
            ungaPrev             = yesterday?.ungaPrice     ?: unga
            cbkRate              = cbk
            cbkPrev              = yesterday?.cbkRate       ?: cbk
            kraDeadlineDays      = nextKraDays
            activeCircularsCount = activeNotices.size
            refreshedAt          = Instant.now()
        }
        marketRepo.save(snapshot)
    }

    private fun cbkRate(type: String): Double? =
        cbkRates.findFirstByRateTypeOrderByEffectiveDateDesc(type)?.rateValue?.toDouble()

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
