package io.riskily.sme.scoring

import io.riskily.sme.AbstractIntegrationTest
import io.riskily.sme.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Pins the engine to the canonical methodology's worked example ("Mama Mboga"): a stock-heavy
 * local shop during input-cost inflation. Cost & Price should be the worst module (ACT NOW)
 * and the overall lands in the WATCH OUT band.
 */
class SmeScoreCalculatorTest : AbstractIntegrationTest() {

    @Autowired lateinit var calculator: SmeScoreCalculator

    private fun mamaMboga() = User(
        email = "mama@duka.co.ke",
        passwordHash = "x",
    ).apply {
        importBehaviour = "no_local"
        paymentMethods = mutableListOf("mpesa", "cash")
        biggestCost = "stock_inventory"
        employeeRange = "2_5"
        businessType = "shop_retail"
    }

    // USD/KES flat, fuel +4%, unga +5%, CBK 10% (flat), KRA deadline 20 days away, 1 circular.
    private val snapshot = MarketSnapshot(
        usdkesSpot = 150.0, usdkesPrev = 150.0, usdkes7dAgo = 150.0,
        fuelPrice = 104.0, fuelPrev = 100.0,
        ungaPrice = 105.0, ungaPrev = 100.0,
        cbkRate = 10.0, cbkPrev = 10.0,
        nextKraDeadlineDays = 20, activeCircularsCount = 1,
    )

    @Test
    fun `worked example matches the methodology`() {
        val result = calculator.calculate(mamaMboga(), snapshot)
        val byCode = result.modules.associateBy { it.code }

        // Cost & Price: exposure 0.80 × pressure 87 → health ~30 (ACT NOW)
        assertThat(byCode.getValue(ModuleCode.COMMODITY).health).isEqualTo(30)
        assertThat(byCode.getValue(ModuleCode.COMMODITY).band).isEqualTo(Band.ACT_NOW)

        // FX: no import exposure → very healthy
        assertThat(byCode.getValue(ModuleCode.FX).health).isGreaterThanOrEqualTo(95)

        // Regulatory uses the provisional floor (1 circular) → flagged provisional
        assertThat(byCode.getValue(ModuleCode.REGULATORY).isProvisional).isTrue()

        // Worst module is Cost & Price
        val worst = result.modules.minByOrNull { it.health }!!
        assertThat(worst.code).isEqualTo(ModuleCode.COMMODITY)

        // Overall lands in WATCH OUT (~73)
        assertThat(result.overallHealth).isBetween(70, 75)
        assertThat(result.overallBand).isEqualTo(Band.WATCH_OUT)
    }

    @Test
    fun `neutral snapshot yields healthy baseline scores`() {
        val result = calculator.calculate(mamaMboga(), null)
        // No market stress → all modules comfortably healthy
        assertThat(result.modules).allSatisfy { assertThat(it.health).isGreaterThan(60) }
    }
}
