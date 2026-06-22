package io.riskily.sme.market

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

data class MarketItem(
    val current: Double,
    val prev: Double,
    val delta: Double,
    /** "UP" | "DOWN" | "STABLE" — abs(delta/prev) < 0.1 % is considered stable. */
    val direction: String,
)

data class MarketDataResponse(
    val snapshotDate: LocalDate,
    val usdkes: MarketItem,
    val fuel: MarketItem,
    val cbkRate: MarketItem,
    val unga: MarketItem,
    val kraDeadlineDays: Int,
    val refreshedAt: Instant,
)

/**
 * GET /api/market-data/latest — authenticated (default security rule).
 * Returns today's market snapshot enriched with directional signals for the dashboard strip.
 * Returns null (204) when no snapshot exists yet (first boot before the job has run).
 */
@RestController
@RequestMapping("/api/market-data")
class MarketDataController(private val repo: MarketDataRepository) {

    @GetMapping("/latest")
    fun latest(): MarketDataResponse? =
        repo.findFirstByOrderBySnapshotDateDesc()?.toResponse()

    private fun MarketData.toResponse() = MarketDataResponse(
        snapshotDate    = snapshotDate,
        usdkes          = item(usdkesSpot, usdkesPrev),
        fuel            = item(fuelPrice, fuelPrev),
        cbkRate         = item(cbkRate, cbkPrev),
        unga            = item(ungaPrice, ungaPrev),
        kraDeadlineDays = kraDeadlineDays,
        refreshedAt     = refreshedAt,
    )

    private fun item(current: Double, prev: Double): MarketItem {
        val delta = current - prev
        val pct = if (prev != 0.0) abs(delta) / prev * 100.0 else 0.0
        val direction = when {
            pct < 0.1  -> "STABLE"
            delta > 0  -> "UP"
            else       -> "DOWN"
        }
        return MarketItem(current = current, prev = prev, delta = delta, direction = direction)
    }
}
