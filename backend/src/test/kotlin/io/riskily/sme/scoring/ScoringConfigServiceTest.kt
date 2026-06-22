package io.riskily.sme.scoring

import io.riskily.sme.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ScoringConfigServiceTest : AbstractIntegrationTest() {

    @Autowired lateinit var service: ScoringConfigService

    @Test
    fun `v1 constants are seeded and resolvable`() {
        // Band thresholds present
        assertThat(service.double("bands", "allGood")).isEqualTo(75.0)
        assertThat(service.double("bands", "actNow")).isEqualTo(25.0)

        // FX exposure for a regular importer
        assertThat(service.double("exposure.fx", "yes_regularly")).isEqualTo(1.0)

        // FX pressure coefficient
        assertThat(service.double("pressure.fx", "depr7dCoef")).isEqualTo(6.0)

        // All expected keys loaded
        assertThat(service.keys()).contains(
            "bands", "weights", "pressure.fx", "pressure.compliance",
            "exposure.cost", "exposure.macro",
        )
    }
}
