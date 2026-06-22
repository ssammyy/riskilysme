package io.riskily.sme.scoring

import io.riskily.sme.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit

data class ModuleScoreDto(
    val code: String,
    val exposure: Double,
    val pressure: Double,
    val health: Int,
    val band: String,
    val dataConfidence: String,
    val isProvisional: Boolean,
)

data class ScoreResponse(
    val overallHealth: Int,
    val overallBand: String,
    val modules: List<ModuleScoreDto>,
    val calculatedAt: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS),
)

@Service
class ScoreService(
    private val calculator: SmeScoreCalculator,
    private val marketProvider: MarketSnapshotProvider,
    private val moduleScores: ModuleScoreRepository,
    private val scoreHistory: ScoreHistoryRepository,
) {

    /** Recompute all modules + overall for a user, upsert module_scores, append score_history. */
    @Transactional
    fun recalculateAndPersist(user: User): ScoreResponse {
        val userId = user.id ?: error("User must be persisted")
        val result = calculator.calculate(user, marketProvider.current())

        result.modules.forEach { m ->
            val entity = moduleScores.findByUserIdAndModuleCode(userId, m.code).orElseGet {
                ModuleScore(
                    userId = userId,
                    moduleCode = m.code,
                    exposure = BigDecimal.ZERO,
                    pressure = BigDecimal.ZERO,
                    health = 0,
                    band = m.band,
                )
            }
            entity.exposure = BigDecimal(m.exposure).setScale(4, RoundingMode.HALF_UP)
            entity.pressure = BigDecimal(m.pressure).setScale(2, RoundingMode.HALF_UP)
            entity.health = m.health
            entity.band = m.band
            entity.dataConfidence = m.dataConfidence
            entity.isProvisional = m.isProvisional
            entity.calculatedAt = Instant.now()
            moduleScores.save(entity)
        }

        val saved = scoreHistory.save(
            ScoreHistory(userId = userId, overallHealth = result.overallHealth, band = result.overallBand),
        )
        return result.toResponse(saved.calculatedAt)
    }

    /** Current persisted score for a user, or null if they have none yet. */
    @Transactional(readOnly = true)
    fun currentScoreOrNull(userId: Long): ScoreResponse? {
        val latest = scoreHistory.findFirstByUserIdOrderByCalculatedAtDesc(userId).orElse(null)
            ?: return null
        val modules = moduleScores.findByUserId(userId).sortedBy { it.moduleCode.ordinal }.map {
            ModuleScoreDto(
                code = it.moduleCode.name,
                exposure = it.exposure.toDouble(),
                pressure = it.pressure.toDouble(),
                health = it.health,
                band = it.band.name.lowercase(),
                dataConfidence = it.dataConfidence.name.lowercase(),
                isProvisional = it.isProvisional,
            )
        }
        return ScoreResponse(latest.overallHealth, latest.band.name.lowercase(), modules, latest.calculatedAt)
    }

    private fun ScoreResult.toResponse(at: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)) = ScoreResponse(
        overallHealth = overallHealth,
        overallBand = overallBand.name.lowercase(),
        modules = modules.map {
            ModuleScoreDto(
                code = it.code.name,
                exposure = it.exposure,
                pressure = it.pressure,
                health = it.health,
                band = it.band.name.lowercase(),
                dataConfidence = it.dataConfidence.name.lowercase(),
                isProvisional = it.isProvisional,
            )
        },
        calculatedAt = at,
    )
}
