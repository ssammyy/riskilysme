package io.riskily.sme.cbk

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * A point-in-time Central Bank of Kenya reference rate.
 * The latest row per [rateType] (by [effectiveDate]) is the current live rate.
 * rate_type examples: CBR, USD_KES, T_BILL_91, INFLATION
 */
@Entity
@Table(name = "cbk_rates")
class CbkRate(

    @Column(name = "rate_type", nullable = false, length = 40)
    var rateType: String,

    @Column(name = "rate_value", nullable = false, precision = 12, scale = 6)
    var rateValue: BigDecimal,

    @Column(name = "effective_date", nullable = false)
    var effectiveDate: LocalDate = LocalDate.now(),

    @Column(name = "set_by", nullable = false)
    var setBy: String = "system",
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
}
