package io.riskily.sme.alert

import com.fasterxml.jackson.databind.ObjectMapper
import io.riskily.sme.AbstractIntegrationTest
import io.riskily.sme.scoring.ModuleCode
import io.riskily.sme.user.UserRepository
import java.time.YearMonth
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class AlertIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var alertRepo: AlertRepository
    @Autowired lateinit var userRepo: UserRepository

    private fun registerAndToken(email: String): String {
        val res = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to email, "password" to "password123", "firstName" to "Tester")
            )
        }.andReturn()
        return objectMapper.readTree(res.response.contentAsString).get("accessToken").asText()
    }

    @Test
    fun `GET alerts-me without token is 401`() {
        mockMvc.get("/api/alerts/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET alerts-me returns alerts list with cap metadata for basic user`() {
        val email = "alertme@test.co.ke"
        val token = registerAndToken(email)
        val user = userRepo.findByEmailIgnoreCase(email).get()

        alertRepo.save(Alert(
            userId = user.id!!, severity = AlertSeverity.URGENT,
            moduleCode = ModuleCode.FX, title = "FX spike", body = "USD/KES up sharply",
            monthKey = YearMonth.now().toString(),
        ))

        mockMvc.get("/api/alerts/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.alerts.length()") { value(1) }
            jsonPath("$.alerts[0].severity") { value("URGENT") }
            jsonPath("$.monthlyUsed") { value(1) }
            jsonPath("$.monthlyCap") { value(3) }
        }
    }

    @Test
    fun `POST alerts-id-read marks alert as read`() {
        val email = "markread@test.co.ke"
        val token = registerAndToken(email)
        val user = userRepo.findByEmailIgnoreCase(email).get()

        val alert = alertRepo.save(Alert(
            userId = user.id!!, severity = AlertSeverity.ACT_NOW,
            moduleCode = ModuleCode.CREDIT, title = "Credit stress", body = "Credit pressure rising",
            monthKey = YearMonth.now().toString(),
        ))

        mockMvc.post("/api/alerts/${alert.id!!}/read") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.isRead") { value(true) }
        }

        assertThat(alertRepo.findById(alert.id!!).get().isRead).isTrue()
    }

    @Test
    fun `POST alerts-id-read is forbidden for a different user`() {
        val ownerEmail = "owner@test.co.ke"
        val otherEmail = "other@test.co.ke"
        registerAndToken(ownerEmail)
        val otherToken = registerAndToken(otherEmail)
        val owner = userRepo.findByEmailIgnoreCase(ownerEmail).get()

        val alert = alertRepo.save(Alert(
            userId = owner.id!!, severity = AlertSeverity.URGENT,
            moduleCode = ModuleCode.LIQUIDITY, title = "Liquidity warning", body = "Cash runway short",
            monthKey = YearMonth.now().toString(),
        ))

        mockMvc.post("/api/alerts/${alert.id!!}/read") {
            header("Authorization", "Bearer $otherToken")
        }.andExpect { status { isForbidden() } }
    }
}
