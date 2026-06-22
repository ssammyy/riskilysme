package io.riskily.sme.alert

import io.riskily.sme.scoring.ModuleCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Alert severity — mirrors Band but ALL_GOOD never creates an alert. */
enum class AlertSeverity {
    URGENT,   // ordinal 0 — sorts first (most critical)
    ACT_NOW,  // ordinal 1
    WATCH_OUT // ordinal 2
}

/**
 * A per-user risk alert derived from the daily scoring run.
 * month_key ('YYYY-MM') pairs with users.monthly_alert_count for the Basic 3-cap.
 */
@Entity
@Table(name = "user_alerts")
class Alert(

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var severity: AlertSeverity,

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", length = 20)
    var moduleCode: ModuleCode? = null,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var body: String,

    /** 'YYYY-MM' — used to scope the Basic monthly cap. */
    @Column(name = "month_key", nullable = false, length = 7)
    var monthKey: String,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
}
