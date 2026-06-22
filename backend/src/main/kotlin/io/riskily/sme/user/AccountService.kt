package io.riskily.sme.user

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountService(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    /** Change the password for an authenticated user after verifying the current one. */
    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        val user = users.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        require(passwordEncoder.matches(currentPassword, user.passwordHash)) {
            "Current password is incorrect"
        }
        user.passwordHash = passwordEncoder.encode(newPassword)
        users.save(user)
    }

    /** Update editable profile fields; only non-null fields are applied. */
    @Transactional
    fun updateProfile(userId: Long, request: UpdateProfileRequest): UserSummaryResponse {
        val user = users.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        request.firstName?.let { user.firstName = it.trim() }
        request.businessName?.let { user.businessName = it.trim() }
        request.language?.let { user.language = Language.fromCode(it) }
        return users.save(user).toSummary()
    }
}
