package io.riskily.sme.scoring

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** Reference row for a risk module. Display name resolves from sme_name_key in the language files. */
@Entity
@Table(name = "risk_modules")
class RiskModule(
    @Id
    @Column(length = 20)
    var code: String,

    @Column(name = "sme_name_key", nullable = false)
    var smeNameKey: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,
)
