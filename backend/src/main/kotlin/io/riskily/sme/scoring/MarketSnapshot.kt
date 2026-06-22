package io.riskily.sme.scoring

import org.springframework.stereotype.Component

/**
 * Daily market inputs the pressure model needs. Nullable so the engine can run before any
 * feed exists (Sprint 2) — a null snapshot yields neutral baseline pressures.
 */
data class MarketSnapshot(
    val usdkesSpot: Double,
    val usdkesPrev: Double,
    val usdkes7dAgo: Double,
    val fuelPrice: Double,
    val fuelPrev: Double,
    val ungaPrice: Double,
    val ungaPrev: Double,
    val cbkRate: Double,
    val cbkPrev: Double,
    val nextKraDeadlineDays: Int,
    val activeCircularsCount: Int,
)

/** Supplies the current market snapshot. */
interface MarketSnapshotProvider {
    fun current(): MarketSnapshot?
}

/**
 * Sprint 2 default: no market feed yet, so return null → neutral baseline pressures.
 * TODO(Sprint 3): replace with a `market_data`-backed provider fed by the 06:00 EAT job.
 */
@Component
class NeutralMarketSnapshotProvider : MarketSnapshotProvider {
    override fun current(): MarketSnapshot? = null
}
