package io.riskily.sme

import io.riskily.sme.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Verifies the Spring context loads against a real (Testcontainers) Postgres and that
 * Flyway V1 produced a usable users table.
 */
class SmeApplicationTests : AbstractIntegrationTest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `context loads and users table is queryable`() {
        assertThat(userRepository.count()).isZero()
    }
}
