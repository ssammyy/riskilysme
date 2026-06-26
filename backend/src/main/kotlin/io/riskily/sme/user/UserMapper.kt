package io.riskily.sme.user

/** API-facing summary of a user. Enums are exposed as lowercase codes for the frontend. */
data class UserSummaryResponse(
    val id: Long,
    val email: String,
    val firstName: String?,
    val role: String,
    val subscriptionTier: String,
    val language: String,
    val onboardingCompleted: Boolean,
    val emailVerified: Boolean,
)

fun User.toSummary(): UserSummaryResponse = UserSummaryResponse(
    id = id ?: error("User must be persisted"),
    email = email,
    firstName = firstName,
    role = role.name.lowercase(),
    subscriptionTier = subscriptionTier.name.lowercase(),
    language = language.code,
    onboardingCompleted = onboardingCompletedAt != null,
    emailVerified = emailVerified,
)
