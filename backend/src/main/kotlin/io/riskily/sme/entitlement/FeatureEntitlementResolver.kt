package io.riskily.sme.entitlement

import io.riskily.sme.user.SubscriptionTier
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

/**
 * Resolves which tier a feature requires. The guard layer depends on this interface, never
 * on hardcoded tier checks, so the backing source can change without touching call sites.
 */
interface FeatureEntitlementResolver {
    fun requiredTier(feature: Feature): SubscriptionTier
    fun isAllowed(tier: SubscriptionTier, feature: Feature): Boolean
    fun all(): Map<Feature, SubscriptionTier>
}

/**
 * DB-backed resolver (Sprint 2): reads the admin-managed `feature_entitlements` table.
 * A row that is `enabled = false` reverts to the feature's in-code default tier. Cached in
 * memory; call [reload] after an admin edit. Keys stay stable with the [Feature] catalogue.
 */
@Component
class DbFeatureEntitlementResolver(
    private val repository: FeatureEntitlementRepository,
) : FeatureEntitlementResolver {

    // featureKey -> active min tier
    private val cache = AtomicReference<Map<String, SubscriptionTier>>(emptyMap())

    init {
        reload()
    }

    fun reload() {
        val active = repository.findAll()
            .filter { it.enabled }
            .associate { it.featureKey to it.minTier }
        cache.set(active)
    }

    override fun requiredTier(feature: Feature): SubscriptionTier =
        cache.get()[feature.key] ?: feature.defaultMinTier

    override fun isAllowed(tier: SubscriptionTier, feature: Feature): Boolean =
        tier.ordinal >= requiredTier(feature).ordinal // BASIC(0) < STANDARD(1)

    override fun all(): Map<Feature, SubscriptionTier> =
        Feature.entries.associateWith { requiredTier(it) }
}
