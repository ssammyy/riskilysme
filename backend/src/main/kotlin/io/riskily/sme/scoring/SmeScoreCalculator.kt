package io.riskily.sme.scoring

import io.riskily.sme.user.User
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

/** Per-module result of a scoring run. */
data class ModuleScoreResult(
    val code: ModuleCode,
    val exposure: Double,
    val pressure: Double,
    val health: Int,
    val band: Band,
    val dataConfidence: DataConfidence,
    val isProvisional: Boolean,
)

/** Full scoring result: overall + the 7 modules. */
data class ScoreResult(
    val overallHealth: Int,
    val overallBand: Band,
    val modules: List<ModuleScoreResult>,
)

/**
 * Combines exposure × pressure into module health and the exposure-tilted overall score,
 * per methodology §4 and §7. Pure/deterministic — persistence lives in [ScoreService].
 */
@Component
class SmeScoreCalculator(
    private val exposureCalculator: ExposureCalculator,
    private val pressureCalculator: PressureCalculator,
    private val config: ScoringConfigService,
) {

    fun calculate(user: User, snapshot: MarketSnapshot?): ScoreResult {
        val exposures = exposureCalculator.compute(user)
        val pressures = pressureCalculator.compute(snapshot)
        val circulars = snapshot?.activeCircularsCount ?: 0

        val modules = ModuleCode.entries.map { code ->
            val exposure = exposures.getValue(code)
            val pressure = pressures.getValue(code)
            val rawHealth = (100.0 - exposure * pressure).coerceIn(0.0, 100.0).roundToInt()
            val isProvisional = code == ModuleCode.REGULATORY && circulars > 0
            // §6.4 provisional floor: cap health when active circulars are present,
            // because REGULATORY exposure (~5%) makes a pressure floor ineffective.
            val health = if (isProvisional) minOf(rawHealth, provisionalHealthCap(circulars)) else rawHealth
            ModuleScoreResult(
                code = code,
                exposure = exposure,
                pressure = pressure,
                health = health,
                band = band(health),
                dataConfidence = DataConfidence.PROFILE,
                isProvisional = isProvisional,
            )
        }

        val overall = weightedOverall(modules)
        return ScoreResult(overall, band(overall), modules)
    }

    /** weight_i = equalBase + tilt × (exposure_i / Σ exposure); Σ weight = 1. */
    private fun weightedOverall(modules: List<ModuleScoreResult>): Int {
        val weights = config.node("weights")
        val equalBase = weights.get("equalBaseTotal").asDouble() / modules.size
        val tiltTotal = weights.get("exposureTiltTotal").asDouble()
        val sumExposure = modules.sumOf { it.exposure }

        val score = modules.sumOf { m ->
            val tilt = if (sumExposure > 0) tiltTotal * (m.exposure / sumExposure) else tiltTotal / modules.size
            (equalBase + tilt) * m.health
        }
        return score.roundToInt()
    }

    private fun provisionalHealthCap(circulars: Int): Int {
        val caps = config.node("pressure.compliance").get("provisionalHealthCap")
        return when {
            circulars >= 3 -> caps.get("threePlus").asInt()
            circulars == 2 -> caps.get("two").asInt()
            else           -> caps.get("one").asInt()
        }
    }

    fun band(health: Int): Band {
        val bands = config.node("bands")
        return when {
            health >= bands.get("allGood").asInt() -> Band.ALL_GOOD
            health >= bands.get("watchOut").asInt() -> Band.WATCH_OUT
            health >= bands.get("actNow").asInt() -> Band.ACT_NOW
            else -> Band.URGENT
        }
    }
}
