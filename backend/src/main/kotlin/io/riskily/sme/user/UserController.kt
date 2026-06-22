package io.riskily.sme.user

import io.riskily.sme.auth.AppUserDetails
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me")
class UserController(private val accountService: AccountService) {

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
}
