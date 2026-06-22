package io.riskily.sme.entitlement

import io.riskily.sme.user.SubscriptionTier
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Admin-managed entitlement row: which tier a feature requires, and whether the override is active. */
@Entity
@Table(name = "feature_entitlements")
class FeatureEntitlement(

    @Column(name = "feature_key", nullable = false, unique = true)
    var featureKey: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "min_tier", nullable = false, length = 20)
    var minTier: SubscriptionTier,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "updated_by")
    var updatedBy: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
}
