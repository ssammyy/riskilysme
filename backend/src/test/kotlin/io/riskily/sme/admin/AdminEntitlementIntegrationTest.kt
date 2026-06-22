package io.riskily.sme.admin

import com.fasterxml.jackson.databind.ObjectMapper
import io.riskily.sme.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@AutoConfigureMockMvc
class AdminEntitlementIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun token(email: String, password: String): String {
        val res = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
        }.andReturn()
        return objectMapper.readTree(res.response.contentAsString).get("accessToken").asText()
    }

    @Test
    fun `admin can change a feature's required tier and the resolver reflects it`() {
        val admin = token("admin@riskily.africa", "changeme-admin-pass")

        // Make "academy" free (basic)
        mockMvc.put("/api/admin/entitlements/academy") {
            header("Authorization", "Bearer $admin")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("minTier" to "basic", "enabled" to true))
        }.andExpect {
            status { isOk() }
            jsonPath("$.minTier") { value("basic") }
        }

        // The public entitlements matrix now reports academy as basic
        mockMvc.get("/api/entitlements") {
            header("Authorization", "Bearer $admin")
        }.andExpect {
            status { isOk() }
        }.andReturn().let { result ->
            val rows = objectMapper.readTree(result.response.contentAsString)
            val academy = rows.first { it.get("feature").asText() == "academy" }
            assert(academy.get("requiredTier").asText() == "basic")
        }
    }
}
