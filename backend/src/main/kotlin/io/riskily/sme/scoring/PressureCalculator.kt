package io.riskily.sme.scoring

import org.springframework.stereotype.Component

/**
 * Per-module pressure (0..100) from the daily market snapshot, per methodology §6.
 * A null snapshot yields neutral baseline pressures (each module's `base`). All constants
 * come from `scoring_config` (pressure.*).
 */
@Component
class PressureCalculator(private val config: ScoringConfigService) {

    fun compute(s: MarketSnapshot?): Map<ModuleCode, Double> {
        val fxN = config.node("pressure.fx")
        val costN = config.node("pressure.cost")
        val credN = config.node("pressure.credit")
        val cashN = config.node("pressure.cashflow")
        val compN = config.node("pressure.compliance")
        val csN = config.node("pressure.customerSupplier")
        val macroN = config.node("pressure.macro")

        val csBaseline = csN.get("baseline").asDouble()
        val compBase = compN.get("base").asDouble()

        val fx: Double
        val cost: Double
        val credit: Double
        val cashflow: Double
        val compliance: Double

        if (s == null) {
            fx = fxN.get("base").asDouble()
            cost = costN.get("base").asDouble()
            credit = credN.get("base").asDouble()
            cashflow = cashN.get("base").asDouble()
            compliance = (compBase + compN.get("deadline").get("other").asDouble()).clampPressure()
        } else {
            val depr7d = pctChange(s.usdkesSpot, s.usdkes7dAgo)
            val depr1d = pctChange(s.usdkesSpot, s.usdkesPrev)
            fx = (fxN.get("base").asDouble() +
                fxN.get("depr7dCoef").asDouble() * pos(depr7d) +
                fxN.get("depr1dCoef").asDouble() * pos(depr1d) -
                fxN.get("apprEaseCoef").asDouble() * pos(-depr7d)).clampPressure()

            val fuelChg = pctChange(s.fuelPrice, s.fuelPrev)
            val ungaChg = pctChange(s.ungaPrice, s.ungaPrev)
            cost = (costN.get("base").asDouble() +
                costN.get("fuelCoef").asDouble() * pos(fuelChg) +
                costN.get("ungaCoef").asDouble() * pos(ungaChg)).clampPressure()

            val neutralRate = credN.get("neutralCbkRate").asDouble()
            val rateLevel = ((s.cbkRate - neutralRate) * credN.get("rateLevelMult").asDouble())
                .coerceIn(0.0, credN.get("rateLevelCap").asDouble())
            val rateRise = pos(s.cbkRate - s.cbkPrev)
            credit = (credN.get("base").asDouble() + rateLevel +
                credN.get("rateRiseCoef").asDouble() * rateRise).clampPressure()
            cashflow = (cashN.get("base").asDouble() +
                cashN.get("rateLevelFactorMult").asDouble() * rateLevel).clampPressure()

            val d = compN.get("deadline")
            val deadlineFactor = when {
                s.nextKraDeadlineDays <= 3 -> d.get("d3").asDouble()
                s.nextKraDeadlineDays <= 7 -> d.get("d7").asDouble()
                s.nextKraDeadlineDays <= 14 -> d.get("d14").asDouble()
                else -> d.get("other").asDouble()
            }
            val pf = compN.get("provisionalFloor")
            val floor = when {
                s.activeCircularsCount <= 0 -> 0.0
                s.activeCircularsCount == 1 -> pf.get("one").asDouble()
                s.activeCircularsCount == 2 -> pf.get("two").asDouble()
                else -> pf.get("threePlus").asDouble()
            }
            compliance = maxOf(compBase + deadlineFactor, floor).clampPressure()
        }

        val macro = (macroN.get("fxWeight").asDouble() * fx +
            macroN.get("costWeight").asDouble() * cost +
            macroN.get("creditWeight").asDouble() * credit).clampPressure()

        return mapOf(
            ModuleCode.FX to fx,
            ModuleCode.COMMODITY to cost,
            ModuleCode.CREDIT to credit,
            ModuleCode.LIQUIDITY to cashflow,
            ModuleCode.REGULATORY to compliance,
            ModuleCode.COUNTERPARTY to csBaseline,
            ModuleCode.MACRO to macro,
        )
    }

    private fun pctChange(now: Double, then: Double): Double =
        if (then == 0.0) 0.0 else (now - then) / then * 100.0

    private fun pos(v: Double): Double = maxOf(v, 0.0)

    private fun Double.clampPressure(): Double = coerceIn(0.0, 100.0)
}
