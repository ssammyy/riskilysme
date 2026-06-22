package io.riskily.sme.market

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

/**
 * Daily market snapshot used by [DatabaseMarketSnapshotProvider] to feed the scoring engine.
 * One row per [snapshotDate]; the 05:50 EAT job upserts today's row before DailyScoreJob runs.
 */
@Entity
@Table(name = "market_data")
class MarketData(

    @Column(name = "snapshot_date", nullable = false, unique = true)
    var snapshotDate: LocalDate,

    @Column(name = "usdkes_spot", nullable = false)
    var usdkesSpot: Double = 0.0,

    @Column(name = "usdkes_prev", nullable = false)
    var usdkesPrev: Double = 0.0,

    @Column(name = "usdkes_7d_ago", nullable = false)
    var usdkes7dAgo: Double = 0.0,

    @Column(name = "fuel_price", nullable = false)
    var fuelPrice: Double = 0.0,

    @Column(name = "fuel_prev", nullable = false)
    var fuelPrev: Double = 0.0,

    @Column(name = "unga_price", nullable = false)
    var ungaPrice: Double = 0.0,

    @Column(name = "unga_prev", nullable = false)
    var ungaPrev: Double = 0.0,

    @Column(name = "cbk_rate", nullable = false)
    var cbkRate: Double = 0.0,

    @Column(name = "cbk_prev", nullable = false)
    var cbkPrev: Double = 0.0,

    @Column(name = "kra_deadline_days", nullable = false)
    var kraDeadlineDays: Int = 30,

    @Column(name = "active_circulars_count", nullable = false)
    var activeCircularsCount: Int = 0,

    @Column(name = "refreshed_at", nullable = false)
    var refreshedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
