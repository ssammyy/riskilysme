package io.riskily.sme.scoring

import org.springframework.data.jpa.repository.JpaRepository

interface ScoringConfigRepository : JpaRepository<ScoringConfig, Long> {
    fun findAllByActiveTrue(): List<ScoringConfig>
}
