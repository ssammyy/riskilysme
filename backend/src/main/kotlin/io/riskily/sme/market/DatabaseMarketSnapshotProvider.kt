package io.riskily.sme.market

import io.riskily.sme.scoring.MarketSnapshot
import io.riskily.sme.scoring.MarketSnapshotProvider
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Live implementation of [MarketSnapshotProvider] backed by the [market_data] table.
 * Marked [@Primary] so Spring prefers it over [NeutralMarketSnapshotProvider] in production.
 * Returns null only when no snapshot has been written yet (e.g. first boot before the job runs).
 */
@Component
@Primary
class DatabaseMarketSnapshotProvider(
    private val repo: MarketDataRepository,
) : MarketSnapshotProvider {

    override fun current(): MarketSnapshot? =
        repo.findFirstByOrderBySnapshotDateDesc()?.toSnapshot()

    private fun MarketData.toSnapshot() = MarketSnapshot(
        usdkesSpot           = usdkesSpot,
        usdkesPrev           = usdkesPrev,
        usdkes7dAgo          = usdkes7dAgo,
        fuelPrice            = fuelPrice,
        fuelPrev             = fuelPrev,
        ungaPrice            = ungaPrice,
        ungaPrev             = ungaPrev,
        cbkRate              = cbkRate,
        cbkPrev              = cbkPrev,
        nextKraDeadlineDays  = kraDeadlineDays,
        activeCircularsCount = activeCircularsCount,
    )
}
