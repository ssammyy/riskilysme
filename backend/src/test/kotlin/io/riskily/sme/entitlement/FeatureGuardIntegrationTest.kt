package io.riskily.sme.entitlement

import com.fasterxml.jackson.databind.ObjectMapper
import io.riskily.sme.AbstractIntegrationTest
import io.riskily.sme.user.SubscriptionTier
import io.riskily.sme.user.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
class GuardedTestController {
    @GetMapping("/whatif")
    @RequiresFeature(Feature.WHATIF_CALCULATOR)
    fun whatif(): Map<String, String> = mapOf("ok" to "true")
}

@AutoConfigureMockMvc
class FeatureGuardIntegrationTest : AbstractIntegrationTest() {

    @TestConfiguration
    class Config {
        @Bean
        fun guardedTestController() = GuardedTestController()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var users: UserRepository

    private fun registerAndToken(email: String): String {
        val res = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to "password123"))
        }.andReturn()
        return objectMapper.readTree(res.response.contentAsString).get("accessToken").asText()
    }

    @Test
    fun `basic tier is blocked, standard tier is allowed`() {
        val email = "tier@duka.co.ke"
        val token = registerAndToken(email)

        // Basic user -> 403
        mockMvc.get("/api/test/whatif") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isForbidden() } }

        // Upgrade the user to STANDARD; guard reads the current tier per request
        val user = users.findByEmailIgnoreCase(email).orElseThrow()
        user.subscriptionTier = SubscriptionTier.STANDARD
        users.save(user)

        mockMvc.get("/api/test/whatif") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `entitlements endpoint lists the matrix`() {
        val token = registerAndToken("ent@duka.co.ke")
        mockMvc.get("/api/entitlements") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].feature") { exists() }
            jsonPath("$[0].requiredTier") { exists() }
        }
    }
}
