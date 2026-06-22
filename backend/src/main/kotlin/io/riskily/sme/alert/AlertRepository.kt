package io.riskily.sme.alert

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AlertRepository : JpaRepository<Alert, Long> {

    /** All alerts for a user, most-critical (lowest ordinal) and most-recent first. */
    @Query("""
        SELECT a FROM Alert a
        WHERE a.userId = :userId
        ORDER BY
            CASE a.severity
                WHEN 'URGENT'    THEN 0
                WHEN 'ACT_NOW'   THEN 1
                WHEN 'WATCH_OUT' THEN 2
                ELSE 3
            END ASC,
            a.createdAt DESC
    """)
    fun findByUserIdSeverityOrdered(@Param("userId") userId: Long): List<Alert>

    /** Module codes already alerted this month — prevents duplicate alerts per module. */
    fun findByUserIdAndMonthKey(userId: Long, monthKey: String): List<Alert>

    /** Count of alerts routed this month — drives the Basic 3-cap display. */
    fun countByUserIdAndMonthKey(userId: Long, monthKey: String): Long
}
