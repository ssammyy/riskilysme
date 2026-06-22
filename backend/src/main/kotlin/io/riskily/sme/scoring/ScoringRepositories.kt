package io.riskily.sme.scoring

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RiskModuleRepository : JpaRepository<RiskModule, String> {
    fun findAllByOrderByDisplayOrder(): List<RiskModule>
}

interface ModuleScoreRepository : JpaRepository<ModuleScore, Long> {
    fun findByUserId(userId: Long): List<ModuleScore>
    fun findByUserIdAndModuleCode(userId: Long, moduleCode: ModuleCode): Optional<ModuleScore>
}

interface ScoreHistoryRepository : JpaRepository<ScoreHistory, Long> {
    fun findFirstByUserIdOrderByCalculatedAtDesc(userId: Long): Optional<ScoreHistory>
}
