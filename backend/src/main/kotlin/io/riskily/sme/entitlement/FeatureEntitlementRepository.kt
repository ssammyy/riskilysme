package io.riskily.sme.entitlement

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface FeatureEntitlementRepository : JpaRepository<FeatureEntitlement, Long> {
    fun findByFeatureKey(featureKey: String): Optional<FeatureEntitlement>
}
