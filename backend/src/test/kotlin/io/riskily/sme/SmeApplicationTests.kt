package io.riskily.sme

import io.riskily.sme.user.UserRepository
import io.riskily.sme.user.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Verifies the Spring context loads against a real (Testcontainers) Postgres, that Flyway
 * migrations produced a usable schema, and that the initial admin was seeded.
 */
class SmeApplicationTests : AbstractIntegrationTest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `context loads, users table is queryable, and an admin is seeded`() {
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(0)
        assertThat(userRepository.existsByRole(UserRole.ADMIN)).isTrue()
    }
}
