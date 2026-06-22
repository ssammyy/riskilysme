package io.riskily.sme.auth

import com.fasterxml.jackson.databind.ObjectMapper
import io.riskily.sme.AbstractIntegrationTest
import io.riskily.sme.user.User
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

/** Captures the raw reset token instead of "emailing" it, so the flow can be tested. */
class CapturingMailer : PasswordResetMailer {
    var lastToken: String? = null
    override fun sendPasswordReset(user: User, rawToken: String) {
        lastToken = rawToken
    }
}

@AutoConfigureMockMvc
class PasswordResetIntegrationTest : AbstractIntegrationTest() {

    @TestConfiguration
    class Mailers {
        @Bean
        @Primary
        fun capturingMailer() = CapturingMailer()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var mailer: CapturingMailer

    private fun body(vararg pairs: Pair<String, String>) =
        objectMapper.writeValueAsString(pairs.toMap())

    @Test
    fun `forgot then reset lets the user log in with the new password`() {
        val email = "reset@duka.co.ke"
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = body("email" to email, "password" to "oldpassword1")
        }.andExpect { status { isCreated() } }

        mockMvc.post("/api/auth/password/forgot") {
            contentType = MediaType.APPLICATION_JSON
            content = body("email" to email)
        }.andExpect { status { isNoContent() } }

        val token = requireNotNull(mailer.lastToken) { "Reset token should have been issued" }

        mockMvc.post("/api/auth/password/reset") {
            contentType = MediaType.APPLICATION_JSON
            content = body("token" to token, "newPassword" to "newpassword1")
        }.andExpect { status { isNoContent() } }

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = body("email" to email, "password" to "newpassword1")
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = body("email" to email, "password" to "oldpassword1")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `forgot for unknown email still returns 204`() {
        mockMvc.post("/api/auth/password/forgot") {
            contentType = MediaType.APPLICATION_JSON
            content = body("email" to "ghost@nowhere.co.ke")
        }.andExpect { status { isNoContent() } }
    }
}
