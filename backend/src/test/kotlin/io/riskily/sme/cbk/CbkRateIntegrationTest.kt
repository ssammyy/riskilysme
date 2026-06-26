package io.riskily.sme.cbk

import com.fasterxml.jackson.databind.ObjectMapper
import io.riskily.sme.AbstractIntegrationTest
import io.riskily.sme.audit.AuditLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@AutoConfigureMockMvc
class CbkRateIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var auditLogRepo: AuditLogRepository

    // Seeded by AdminSeeder on every test-context boot.
    private fun adminToken(): String {
        val res = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "admin@riskily.africa", "password" to "changeme-admin-pass")
            )
        }.andReturn()
        return objectMapper.readTree(res.response.contentAsString).get("accessToken").asText()
    }

    private fun userToken(): String {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "cbkuser@test.co.ke", "password" to "password123")
            )
        }
        val res = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "cbkuser@test.co.ke", "password" to "password123")
            )
        }.andReturn()
        return objectMapper.readTree(res.response.contentAsString).get("accessToken").asText()
    }

    @Test
    fun `GET cbk-rates is public and returns seeded rates`() {
        mockMvc.get("/api/cbk/rates").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(org.hamcrest.Matchers.greaterThan(0)) }
            jsonPath("$[0].rateType") { exists() }
            jsonPath("$[0].rateValue") { exists() }
        }
    }

    @Test
    fun `PUT cbk-rates as admin creates an audit log entry with correct affectedModules`() {
        val token = adminToken()
        val countBefore = auditLogRepo.count()

        mockMvc.put("/api/cbk/rates/CBR") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("rateValue" to "9.00"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.rateType") { value("CBR") }
            jsonPath("$.rateValue") { value(9.0) }
        }

        val entries = auditLogRepo.findAll()
        assertThat(entries).hasSizeGreaterThan(countBefore.toInt())
        val entry = entries.maxByOrNull { it.timestamp }!!
        assertThat(entry.action).isEqualTo("CBK_RATE_CHANGE")
        assertThat(entry.affectedModules).containsExactlyInAnyOrder("CREDIT", "LIQUIDITY", "MACRO")
    }

    @Test
    fun `PUT cbk-rates as a normal user is forbidden`() {
        val token = userToken()
        mockMvc.put("/api/cbk/rates/CBR") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("rateValue" to "9.00"))
        }.andExpect { status { isForbidden() } }
    }
}
