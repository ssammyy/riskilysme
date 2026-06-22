package io.riskily.sme.scoring

import io.riskily.sme.auth.AppUserDetails
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/score")
class ScoreController(private val scoreService: ScoreService) {

    /** Current Business Health Score for the authenticated user (404 until onboarding is done). */
    @GetMapping("/me")
    fun myScore(@AuthenticationPrincipal principal: AppUserDetails): ScoreResponse =
        scoreService.currentScoreOrNull(principal.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No score yet — complete onboarding")
}
