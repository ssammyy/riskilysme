package io.riskily.sme.insight

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "insights")
class Insight(

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var body: String,

    @Column(name = "action_text", columnDefinition = "TEXT")
    var actionText: String? = null,

    @Column(name = "module_code", length = 20)
    var moduleCode: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "generated_at", nullable = false, updatable = false)
    var generatedAt: Instant = Instant.now()
}
