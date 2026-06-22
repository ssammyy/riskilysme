package io.riskily.sme.scoring

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Append-only daily snapshot of the overall Business Health Score. */
@Entity
@Table(name = "score_history")
class ScoreHistory(

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "overall_health", nullable = false)
    var overallHealth: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var band: Band,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "calculated_at", nullable = false)
    var calculatedAt: Instant = Instant.now()
}
