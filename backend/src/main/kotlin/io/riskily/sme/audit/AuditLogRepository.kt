package io.riskily.sme.audit

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findAllByOrderByTimestampDesc(pageable: Pageable): Page<AuditLog>
}
