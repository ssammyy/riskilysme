package io.riskily.sme.admin

import io.riskily.sme.audit.AuditService
import io.riskily.sme.entitlement.DbFeatureEntitlementResolver
import io.riskily.sme.entitlement.FeatureEntitlementRepository
import io.riskily.sme.user.SubscriptionTier
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class EntitlementAdminResponse(
    val featureKey: String,
    val displayName: String,
    val minTier: String,
    val enabled: Boolean,
)

data class UpdateEntitlementRequest(
    @field:NotBlank val minTier: String, // "basic" | "standard"
    val enabled: Boolean,
)

/** Admin management of the free-vs-paid matrix. Restricted to ADMIN by SecurityConfig. */
@RestController
@RequestMapping("/api/admin/entitlements")
class AdminEntitlementController(
    private val repository: FeatureEntitlementRepository,
    private val resolver: DbFeatureEntitlementResolver,
    private val auditService: AuditService,
) {

    @GetMapping
    fun list(): List<EntitlementAdminResponse> =
        repository.findAll()
            .sortedBy { it.featureKey }
            .map {
                EntitlementAdminResponse(
                    it.featureKey, it.displayName, it.minTier.name.lowercase(), it.enabled,
                )
            }

    @PutMapping("/{featureKey}")
    @Transactional
    fun update(
        @PathVariable featureKey: String,
        @Valid @RequestBody request: UpdateEntitlementRequest,
    ): EntitlementAdminResponse {
        val entitlement = repository.findByFeatureKey(featureKey)
            .orElseThrow { IllegalArgumentException("Unknown feature: $featureKey") }
        val newTier = parseTier(request.minTier)
        val oldValue = "${entitlement.minTier.name.lowercase()}/${entitlement.enabled}"

        entitlement.minTier = newTier
        entitlement.enabled = request.enabled
        repository.save(entitlement)
        resolver.reload()

        auditService.record(
            action = "entitlement_updated",
            entity = "feature_entitlements:$featureKey",
            oldValue = oldValue,
            newValue = "${newTier.name.lowercase()}/${request.enabled}",
        )

        return EntitlementAdminResponse(
            entitlement.featureKey, entitlement.displayName,
            entitlement.minTier.name.lowercase(), entitlement.enabled,
        )
    }

    private fun parseTier(value: String): SubscriptionTier =
        when (value.lowercase()) {
            "basic" -> SubscriptionTier.BASIC
            "standard" -> SubscriptionTier.STANDARD
            else -> throw IllegalArgumentException("Invalid tier: $value")
        }
}
