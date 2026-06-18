package io.riskily.sme.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

/**
 * Sprint 1 step 1: permissive baseline so the app boots and /api/health is reachable.
 * Step 4 (Authentication) tightens this: stateless JWT, public auth endpoints only,
 * everything else authenticated, and /admin/** restricted to ADMIN.
 *
 * TODO-confirm: align filter-chain structure and encoder choice with parent Riskily.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            authorizeHttpRequests {
                authorize("/api/health", permitAll)
                authorize(anyRequest, permitAll) // tightened in step 4
            }
        }
        return http.build()
    }

    /** Argon2id password encoder (approved default). Used from step 4. */
    @Bean
    fun passwordEncoder(): PasswordEncoder =
        Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
}
