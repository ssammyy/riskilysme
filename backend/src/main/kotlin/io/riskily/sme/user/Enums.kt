package io.riskily.sme.user

/** Application access role. Stored as the enum name (USER/ADMIN). */
enum class UserRole { USER, ADMIN }

/** Account lifecycle status. */
enum class UserStatus { ACTIVE, SUSPENDED }

/** Subscription tier. Drives feature entitlements (see Sprint 2 feature_entitlements). */
enum class SubscriptionTier { BASIC, STANDARD }

/** UI language. API/DTO layer exposes the lowercase code (en/sw); DB stores the enum name. */
enum class Language(val code: String) {
    EN("en"),
    SW("sw");

    companion object {
        fun fromCode(code: String): Language =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported language code: $code")
    }
}
