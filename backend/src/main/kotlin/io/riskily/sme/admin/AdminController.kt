package io.riskily.sme.admin

import io.riskily.sme.audit.AuditLog
import io.riskily.sme.audit.AuditLogRepository
import io.riskily.sme.audit.AuditService
import io.riskily.sme.auth.AppUserDetails
import io.riskily.sme.user.SubscriptionTier
import io.riskily.sme.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

data class AdminOverviewResponse(
    val totalUsers: Long,
    val basicUsers: Long,
    val standardUsers: Long,
)

data class AuditLogResponse(
    val id: Long,
    val action: String,
    val entity: String?,
    val oldValue: String?,
    val newValue: String?,
    val performedBy: String,
    val affectedModules: List<String>,
    val timestamp: Instant,
)

data class AdminUserResponse(
    val id: Long,
    val email: String,
    val firstName: String?,
    val subscriptionTier: String,
    val onboardingCompleted: Boolean,
    val createdAt: Instant,
)

data class SetTierRequest(val tier: String)

data class SetPreviewTierRequest(val tier: String?)

data class PreviewTierResponse(val previewTier: String?)

///**
// * Admin portal API. Restricted to ADMIN role by SecurityConfig (/api/admin/**).
// * Sprint 3 adds: user listing, tier management, and "preview as tier" session override.
// */
@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val users: UserRepository,
    private val auditLog: AuditLogRepository,
    private val auditService: AuditService,
    private val tierPreviewService: TierPreviewService,
) {

    @GetMapping("/overview")
    fun overview(): AdminOverviewResponse {
        val total = users.count()
        val standard = users.countBySubscriptionTier(SubscriptionTier.STANDARD)
        val basic = users.countBySubscriptionTier(SubscriptionTier.BASIC)
        return AdminOverviewResponse(total, basic, standard)
    }

    @GetMapping("/audit-log")
    fun auditLog(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): List<AuditLogResponse> =
        auditLog.findAllByOrderByTimestampDesc(PageRequest.of(page, size))
            .content.map { it.toResponse() }

    /** List all users ordered by sign-up date (most recent first). */
    @GetMapping("/users")
    fun listUsers(): List<AdminUserResponse> =
        users.findAllByOrderByCreatedAtDesc().map { u ->
            AdminUserResponse(
                id = u.id ?: 0,
                email = u.email,
                firstName = u.firstName,
                subscriptionTier = u.subscriptionTier.name.lowercase(),
                onboardingCompleted = u.onboardingCompletedAt != null,
                createdAt = u.createdAt,
            )
        }

    /** Update a user's subscription tier. Writes an audit log entry. */
    @PutMapping("/users/{id}/tier")
    fun setUserTier(
        @PathVariable id: Long,
        @RequestBody body: SetTierRequest,
        @AuthenticationPrincipal admin: AppUserDetails,
    ): AdminUserResponse {
        val user = users.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User $id not found")
        }
        val newTier = runCatching { SubscriptionTier.valueOf(body.tier.uppercase()) }.getOrElse {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tier: ${body.tier}")
        }
        val oldTier = user.subscriptionTier
        user.subscriptionTier = newTier
        users.save(user)

        auditService.record(
            action = "TIER_CHANGE",
            entity = "users/${user.id}",
            oldValue = oldTier.name.lowercase(),
            newValue = newTier.name.lowercase(),
            performedByOverride = admin.username,
        )

        return AdminUserResponse(
            id = user.id ?: 0,
            email = user.email,
            firstName = user.firstName,
            subscriptionTier = user.subscriptionTier.name.lowercase(),
            onboardingCompleted = user.onboardingCompletedAt != null,
            createdAt = user.createdAt,
        )
    }

    /**
     * Set or clear the calling admin's "preview as tier" override.
     * Pass { "tier": "basic" | "standard" } to activate; { "tier": null } to clear.
     * The override lives in memory only — never written to the database.
     */
    @PostMapping("/preview-tier")
    fun setPreviewTier(
        @RequestBody body: SetPreviewTierRequest,
        @AuthenticationPrincipal admin: AppUserDetails,
    ): PreviewTierResponse {
        val tier = body.tier?.let {
            runCatching { SubscriptionTier.valueOf(it.uppercase()) }.getOrElse {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tier: $it")
            }
        }
        tierPreviewService.setPreview(admin.id, tier)
        return PreviewTierResponse(tier?.name?.lowercase())
    }

    /** Returns the calling admin's active preview tier, or null if none is set. */
    @GetMapping("/preview-tier")
    fun getPreviewTier(@AuthenticationPrincipal admin: AppUserDetails): PreviewTierResponse =
        PreviewTierResponse(tierPreviewService.getPreviewTier(admin.id)?.name?.lowercase())

    private fun AuditLog.toResponse() = AuditLogResponse(
        id = id ?: 0,
        action = action,
        entity = entity,
        oldValue = oldValue,
        newValue = newValue,
        performedBy = performedBy,
        affectedModules = affectedModules,
        timestamp = timestamp,
    )
}
