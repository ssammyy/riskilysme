package io.riskily.sme.entitlement

import io.riskily.sme.admin.TierPreviewService
import io.riskily.sme.auth.AppUserDetails
import io.riskily.sme.user.UserRole
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Enforces [RequiresFeature] on controller methods: reads the authenticated principal's
 * effective tier and consults the [FeatureEntitlementResolver].
 *
 * For ADMIN users, the effective tier is resolved via [TierPreviewService]: if the admin
 * has activated a "preview as tier" override, that tier is used instead of their real one.
 * This lets QA verify feature gating without touching production data.
 *
 * Throws [FeatureAccessDeniedException] (403) when the tier is insufficient.
 * Authentication itself is enforced upstream by Spring Security.
 */
@Aspect
@Component
class FeatureGuardAspect(
    private val resolver: FeatureEntitlementResolver,
    private val tierPreviewService: TierPreviewService,
) {

    @Before("@annotation(requiresFeature)")
    fun enforce(requiresFeature: RequiresFeature) {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? AppUserDetails
            ?: throw FeatureAccessDeniedException(requiresFeature.value)

        val effectiveTier = if (principal.user.role == UserRole.ADMIN) {
            tierPreviewService.effectiveTier(principal.id, principal.user.subscriptionTier)
        } else {
            principal.user.subscriptionTier
        }

        if (!resolver.isAllowed(effectiveTier, requiresFeature.value)) {
            throw FeatureAccessDeniedException(requiresFeature.value)
        }
    }
}
