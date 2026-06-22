package io.riskily.sme.audit

import io.riskily.sme.auth.AppUserDetails
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.math.BigDecimal

/** Central place to record privileged admin actions. Used by admin features from step 8 on. */
@Service
class AuditService(private val repository: AuditLogRepository) {

    fun record(
        action: String,
        entity: String? = null,
        oldValue: String? = null,
        newValue: String? = null,
        deltaPct: BigDecimal? = null,
        affectedModules: List<String> = emptyList(),
        performedByOverride: String? = null,
    ): AuditLog {
        val performedBy = performedByOverride ?: currentPrincipalEmail() ?: "system"
        return repository.save(
            AuditLog(
                action = action,
                entity = entity,
                oldValue = oldValue,
                newValue = newValue,
                deltaPct = deltaPct,
                performedBy = performedBy,
                affectedModules = affectedModules.toMutableList(),
            ),
        )
    }

    private fun currentPrincipalEmail(): String? =
        (SecurityContextHolder.getContext().authentication?.principal as? AppUserDetails)?.username
}
