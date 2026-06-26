package io.riskily.sme.entitlement

import io.riskily.sme.user.SubscriptionTier

/**
 * Catalogue of gated features and their default minimum tier (the SoW §3 matrix).
 * In Sprint 1 this in-code map is the source of truth; Sprint 2 replaces the resolver
 * with the admin-managed `feature_entitlements` table while keeping these keys stable.
 */
enum class Feature(val key: String, val defaultMinTier: SubscriptionTier) {
    FULL_MODULE_DETAIL("full_module_detail", SubscriptionTier.STANDARD),
    UNLIMITED_ALERTS("unlimited_alerts", SubscriptionTier.STANDARD),
    FULL_MARKET_STRIP("full_market_strip", SubscriptionTier.STANDARD),
    FULL_DEADLINES("full_deadlines", SubscriptionTier.STANDARD),
    WHATIF_CALCULATOR("whatif_calculator", SubscriptionTier.STANDARD),
    AI_BRIEF("ai_brief", SubscriptionTier.STANDARD),
    MONTHLY_REPORT("monthly_report", SubscriptionTier.STANDARD),
    CUSTOMER_TRACKER("customer_tracker", SubscriptionTier.STANDARD),
    LOAN_MONITOR("loan_monitor", SubscriptionTier.STANDARD),
    WHATSAPP_ALERTS("whatsapp_alerts", SubscriptionTier.STANDARD),
    ACADEMY("academy", SubscriptionTier.STANDARD),
    AI_INSIGHTS("ai_insights", SubscriptionTier.STANDARD);

    companion object {
        fun fromKey(key: String): Feature =
            entries.firstOrNull { it.key == key }
                ?: throw IllegalArgumentException("Unknown feature: $key")
    }
}
