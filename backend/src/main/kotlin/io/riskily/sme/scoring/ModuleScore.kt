package io.riskily.sme.scoring

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/** Latest score for one user/module (upserted on each recalculation). */
@Entity
@Table(name = "module_scores")
class ModuleScore(

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 20)
    var moduleCode: ModuleCode,

    @Column(nullable = false, precision = 5, scale = 4)
    var exposure: BigDecimal,

    @Column(nullable = false, precision = 6, scale = 2)
    var pressure: BigDecimal,

    @Column(nullable = false)
    var health: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var band: Band,

    @Enumerated(EnumType.STRING)
    @Column(name = "data_confidence", nullable = false, length = 20)
    var dataConfidence: DataConfidence = DataConfidence.PROFILE,

    @Column(name = "is_provisional", nullable = false)
    var isProvisional: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "calculated_at", nullable = false)
    var calculatedAt: Instant = Instant.now()
}
