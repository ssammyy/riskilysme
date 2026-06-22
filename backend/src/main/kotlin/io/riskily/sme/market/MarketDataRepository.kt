package io.riskily.sme.market

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MarketDataRepository : JpaRepository<MarketData, Long> {

    /** Most recent snapshot — used by [DatabaseMarketSnapshotProvider] and the REST endpoint. */
    fun findFirstByOrderBySnapshotDateDesc(): MarketData?

    /** Today's snapshot if it exists — used by the refresh job for upsert. */
    fun findBySnapshotDate(date: LocalDate): MarketData?

    /** Most recent snapshot strictly before [date] — used to compute prev/7d-ago values. */
    fun findFirstBySnapshotDateBeforeOrderBySnapshotDateDesc(date: LocalDate): MarketData?
}
