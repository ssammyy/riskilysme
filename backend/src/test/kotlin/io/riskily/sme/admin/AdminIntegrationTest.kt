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

@AutoConfigureMockMvc
class AdminIntegrationTest : AbstractIntegrationTest() {

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
    fun `seeded admin can reach the admin overview`() {
        // AdminSeeder creates this on startup (application.yml defaults).
        val adminToken = token("admin@riskily.africa", "changeme-admin-pass")
        mockMvc.get("/api/admin/overview") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalUsers") { exists() }
        }
    }

    @Test
    fun `normal user is forbidden from admin routes`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "notadmin@duka.co.ke", "password" to "password123"),
            )
        }.andExpect { status { isCreated() } }

        val userToken = token("notadmin@duka.co.ke", "password123")
        mockMvc.get("/api/admin/overview") {
            header("Authorization", "Bearer $userToken")
        }.andExpect { status { isForbidden() } }
    }
}
