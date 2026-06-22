package io.riskily.sme.auth

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
class AuthIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun body(vararg pairs: Pair<String, String>) =
        objectMapper.writeValueAsString(pairs.toMap())

    @Test
    fun `register issues tokens, login works, and the token unlocks me`() {
        val email = "owner@duka.co.ke"

        val register = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = body("email" to email, "password" to "password123", "firstName" to "Amina")
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val node = objectMapper.readTree(register.response.contentAsString)
        val accessToken = node.get("accessToken").asText()
        assert(node.get("user").get("subscriptionTier").asText() == "basic")

        mockMvc.get("/api/me") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value(email) }
            jsonPath("$.role") { value("user") }
        }

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = body("email" to email, "password" to "password123")
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
        }
    }

    @Test
    fun `me without a token is rejected`() {
        mockMvc.get("/api/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `duplicate registration is a conflict`() {
        val email = "dupe@duka.co.ke"
        repeat(2) { i ->
            mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = body("email" to email, "password" to "password123")
            }.andExpect {
                status { if (i == 0) isCreated() else isConflict() }
            }
        }
    }
}
