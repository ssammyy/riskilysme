package io.riskily.sme.admin

import org.springframework.boot.context.properties.ConfigurationProperties

/** Bound from `riskily.admin.*`. Used to seed the initial admin account. */
@ConfigurationProperties(prefix = "riskily.admin")
data class AdminProperties(
    val email: String,
    val password: String,
)
