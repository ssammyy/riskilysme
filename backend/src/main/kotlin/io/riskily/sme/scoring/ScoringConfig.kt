package io.riskily.sme.scoring

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** A single versioned calibration constant for the scoring engine. value is JSON text. */
@Entity
@Table(name = "scoring_config")
class ScoringConfig(

    @Column(name = "config_version", nullable = false)
    var configVersion: String,

    @Column(name = "config_key", nullable = false)
    var configKey: String,

    @Column(name = "value_json", nullable = false)
    var valueJson: String,

    @Column(nullable = false)
    var active: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
}
