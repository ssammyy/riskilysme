package io.riskily.sme.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank val currentPassword: String,
    @field:NotBlank @field:Size(min = 8, max = 100) val newPassword: String,
)

/** All fields optional; only provided fields are updated. */
data class UpdateProfileRequest(
    @field:Size(max = 120) val firstName: String? = null,
    @field:Size(max = 200) val businessName: String? = null,
    /** Language code: "en" or "sw". */
    val language: String? = null,
)
