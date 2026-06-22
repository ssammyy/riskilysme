package io.riskily.sme.common

import java.time.Instant

/** Consistent error envelope returned for all handled exceptions. */
data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: Map<String, String>? = null,
    val timestamp: Instant = Instant.now(),
)
