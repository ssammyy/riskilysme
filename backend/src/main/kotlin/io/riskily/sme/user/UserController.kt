package io.riskily.sme.user

import io.riskily.sme.auth.AppUserDetails
import io.riskily.sme.insight.InsightRepository
import io.riskily.sme.insight.InsightResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/me")
class UserController(
    private val accountService: AccountService,
    private val insightRepo: InsightRepository,
) {

    /** Current authenticated user. */
    @GetMapping
    fun me(@AuthenticationPrincipal principal: AppUserDetails): UserSummaryResponse =
        principal.user.toSummary()

    /** Update editable profile fields (first name, business name, language). */
    @PatchMapping
    fun updateProfile(
        @AuthenticationPrincipal principal: AppUserDetails,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): UserSummaryResponse = accountService.updateProfile(principal.id, request)

    /** Change password for the authenticated user. */
    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal principal: AppUserDetails,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<Void> {
        accountService.changePassword(principal.id, request.currentPassword, request.newPassword)
        return ResponseEntity.noContent().build()
    }

    /** Latest 10 AI insights. Standard tier only — returns empty list for Basic. */
    @GetMapping("/insights")
    fun myInsights(@AuthenticationPrincipal principal: AppUserDetails): List<InsightResponse> {
        println("it enters here !!")
        if (principal.user.subscriptionTier != SubscriptionTier.STANDARD) return emptyList()
        return insightRepo.findTop10ByUserIdOrderByGeneratedAtDesc(principal.id)
            .map { it.toInsightResponse() }
    }

    /** Mark a single insight as read. */
    @PostMapping("/insights/{id}/read")
    fun markInsightRead(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Any>,
        @AuthenticationPrincipal principal: AppUserDetails,
    ): InsightResponse {
        val insight = insightRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (insight.userId != principal.id)
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        insight.isRead = true
        insightRepo.save(insight)
        return insight.toInsightResponse()
    }

    private fun io.riskily.sme.insight.Insight.toInsightResponse() = InsightResponse(
        id          = id!!,
        title       = title,
        body        = body,
        actionText  = actionText,
        moduleCode  = moduleCode,
        isRead      = isRead,
        generatedAt = generatedAt,
    )
}
