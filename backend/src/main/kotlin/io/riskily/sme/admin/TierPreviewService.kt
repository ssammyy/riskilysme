package io.riskily.sme.admin

import io.riskily.sme.user.SubscriptionTier
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory store for admin "preview as tier" overrides.
 *
 * An admin may preview the app as a BASIC or STANDARD user to verify feature gating
 * end-to-end. The override lives only in this bean (JVM lifetime) and never touches
 * the user's real subscription_tier in the database. It is keyed by the admin's own
 * userId so that multiple concurrent admin sessions are isolated.
 *
 * The override is intentionally not persisted — a server restart clears all previews,
 * which is the safe default.
 */
@Service
class TierPreviewService {

    private val overrides = ConcurrentHashMap<Long, SubscriptionTier>()

    /** Set a preview override for [adminUserId]. Pass null to clear it. */
    fun setPreview(adminUserId: Long, tier: SubscriptionTier?) {
        if (tier == null) overrides.remove(adminUserId) else overrides[adminUserId] = tier
    }

    /** Returns the active preview tier for [adminUserId], or null if none is set. */
    fun getPreviewTier(adminUserId: Long): SubscriptionTier? = overrides[adminUserId]

    /**
     * Resolves the effective tier for the feature guard: if an admin has an active
     * preview override, that tier is used instead of their real one.
     */
    fun effectiveTier(userId: Long, realTier: SubscriptionTier): SubscriptionTier =
        overrides[userId] ?: realTier
}
