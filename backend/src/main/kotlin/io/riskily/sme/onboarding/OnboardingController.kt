package io.riskily.sme.onboarding

import io.riskily.sme.auth.AppUserDetails
import io.riskily.sme.scoring.ScoreResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/onboarding")
class OnboardingController(private val onboardingService: OnboardingService) {

    /** Complete onboarding and return the starting Business Health Score. */
    @PostMapping
    fun complete(
        @AuthenticationPrincipal principal: AppUserDetails,
        @Valid @RequestBody request: OnboardingRequest,
    ): ScoreResponse = onboardingService.complete(principal.id, request)
}
