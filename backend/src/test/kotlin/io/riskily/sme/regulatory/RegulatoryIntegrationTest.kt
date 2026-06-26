package io.riskily.sme.regulatory

import io.riskily.sme.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class RegulatoryIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun `GET regulatory-deadlines is public and returns seeded notices sorted by daysRemaining`() {
        val result = mockMvc.get("/api/regulatory/deadlines")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(org.hamcrest.Matchers.greaterThan(0)) }
                jsonPath("$[0].id") { exists() }
                jsonPath("$[0].title") { exists() }
                jsonPath("$[0].authority") { exists() }
                jsonPath("$[0].daysRemaining") { exists() }
                jsonPath("$[0].nextDueDate") { exists() }
            }.andReturn()

        // Verify sorted ascending by daysRemaining
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        val nodes = mapper.readTree(result.response.contentAsString)
        val days = nodes.map { it.get("daysRemaining").asLong() }
        assert(days == days.sorted()) { "Deadlines must be sorted ascending by daysRemaining; got $days" }
    }
}
