package io.riskily.sme.cbk

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CbkRateRepository : JpaRepository<CbkRate, Long> {

    /** Latest single rate for a given type. */
    fun findFirstByRateTypeOrderByEffectiveDateDesc(rateType: String): CbkRate?

    /** All distinct rate types with their most-recent value — used for the admin overview card. */
    @Query("""
        SELECT r FROM CbkRate r
        WHERE r.effectiveDate = (
            SELECT MAX(r2.effectiveDate) FROM CbkRate r2 WHERE r2.rateType = r.rateType
        )
        ORDER BY r.rateType ASC
    """)
    fun findLatestForAllTypes(): List<CbkRate>
}
