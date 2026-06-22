package io.riskily.sme.onboarding

import com.fasterxml.jackson.databind.ObjectMapper
import io.riskily.sme.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class OnboardingIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun registerAndToken(email: String): String {
        val res = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to "password123"))
        }.andReturn()
        return objectMapper.readTree(res.response.contentAsString).get("accessToken").asText()
    }

    @Test
    fun `completing onboarding produces a starting score available at score me`() {
        val token = registerAndToken("onboard@duka.co.ke")

        // No score before onboarding
        mockMvc.get("/api/score/me") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isNotFound() } }

        val payload = mapOf(
            "businessName" to "Mama Mboga Stores",
            "firstName" to "Amina",
            "businessType" to "shop_retail",
            "employeeRange" to "2_5",
            "importBehaviour" to "no_local",
            "paymentMethods" to listOf("mpesa", "cash"),
            "biggestCost" to "stock_inventory",
        )
        mockMvc.post("/api/onboarding") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
            jsonPath("$.overallHealth") { exists() }
            jsonPath("$.modules.length()") { value(7) }
        }

        // Score now available; /me reflects onboardingCompleted
        mockMvc.get("/api/score/me") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.onboardingCompleted") { value(true) }
        }
    }

    @Test
    fun `invalid option codes are rejected`() {
        val token = registerAndToken("badonboard@duka.co.ke")
        val payload = mapOf(
            "businessName" to "X",
            "firstName" to "Y",
            "businessType" to "not_a_type",
            "employeeRange" to "2_5",
            "importBehaviour" to "no_local",
            "paymentMethods" to listOf("mpesa"),
            "biggestCost" to "stock_inventory",
        )
        mockMvc.post("/api/onboarding") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect { status { isBadRequest() } }
    }
}
