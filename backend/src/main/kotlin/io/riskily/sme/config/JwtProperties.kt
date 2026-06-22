package io.riskily.sme.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Bound from `riskily.auth.*` in application.yml. */
@ConfigurationProperties(prefix = "riskily.auth")
data class JwtProperties(
    /** HMAC secret; must be at least 32 bytes (256 bits). */
    val jwtSecret: String,
    val accessTtlMinutes: Long = 15,
    val refreshTtlDays: Long = 14,
)
