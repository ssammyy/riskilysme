package io.riskily.sme.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

/** An immutable record of a privileged admin action. */
@Entity
@Table(name = "audit_log")
class AuditLog(

    @Column(nullable = false, length = 80)
    var action: String,

    @Column(length = 80)
    var entity: String? = null,

    @Column(name = "old_value")
    var oldValue: String? = null,

    @Column(name = "new_value")
    var newValue: String? = null,

    @Column(name = "delta_pct")
    var deltaPct: BigDecimal? = null,

    @Column(name = "performed_by", nullable = false)
    var performedBy: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_modules", nullable = false)
    var affectedModules: MutableList<String> = mutableListOf(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "timestamp", nullable = false, updatable = false)
    var timestamp: Instant = Instant.now()
}
