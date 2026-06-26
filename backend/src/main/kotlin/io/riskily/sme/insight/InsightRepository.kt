package io.riskily.sme.insight

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface InsightRepository : JpaRepository<Insight, Long> {
    fun findTop10ByUserIdOrderByGeneratedAtDesc(userId: Long): List<Insight>
    fun existsByUserIdAndGeneratedAtBetween(userId: Long, from: Instant, to: Instant): Boolean
}
