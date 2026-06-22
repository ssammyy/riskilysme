package io.riskily.sme.config

import io.riskily.sme.auth.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
//
///*
// * Stateless JWT security (Sprint 1 step 4).
// *   - public: /api/health, /api/auth/**
// *   - /api/admin/** requires ADMIN (admin portal lands in step 8)
// *   - everything else requires authentication
// *
// * TODO-confirm: align filter-chain structure, CORS origins, and encoder choice with parent Riskily.
// */
@Configuration
class SecurityConfig(private val jwtAuthenticationFilter: JwtAuthenticationFilter) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
            authorizeHttpRequests {
                authorize(HttpMethod.GET, "/api/health", permitAll)
                authorize("/api/auth/**", permitAll)
                // CBK rates: public read, admin write
                authorize(HttpMethod.GET, "/api/cbk/rates", permitAll)
                authorize(HttpMethod.PUT, "/api/cbk/rates/**", hasRole("ADMIN"))
                // Regulatory deadlines: public read
                authorize(HttpMethod.GET, "/api/regulatory/deadlines", permitAll)
                authorize("/api/admin/**", hasRole("ADMIN"))
                authorize(anyRequest, authenticated)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            // Dev origins; production origins added via config. TODO-confirm.
            allowedOrigins = listOf(
                "http://localhost:5173",
                "https://riskily.africa",
                "https://www.riskily.africa",
                "http://178.128.145.102"
            )
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    /** Argon2id password encoder (approved default). */
    @Bean
    fun passwordEncoder(): PasswordEncoder =
        Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
}
