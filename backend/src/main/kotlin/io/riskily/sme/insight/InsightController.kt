package io.riskily.sme.insight

import java.time.Instant

data class InsightResponse(
    val id: Long,
    val title: String,
    val body: String,
    val actionText: String?,
    val moduleCode: String?,
    val isRead: Boolean,
    val generatedAt: Instant,
)
