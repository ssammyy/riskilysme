package io.riskily.sme.entitlement

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class EntitlementResponse(val feature: String, val requiredTier: String)

/**
 * Exposes the feature → required-tier matrix so the frontend gates UI from a single source
 * rather than duplicating the matrix. Sprint 2 serves the same shape from the admin-managed table.
 */
@RestController
@RequestMapping("/api/entitlements")
class EntitlementController(private val resolver: FeatureEntitlementResolver) {

    @GetMapping
    fun list(): List<EntitlementResponse> =
        resolver.all().map { (feature, tier) ->
            EntitlementResponse(feature.key, tier.name.lowercase())
        }
}
