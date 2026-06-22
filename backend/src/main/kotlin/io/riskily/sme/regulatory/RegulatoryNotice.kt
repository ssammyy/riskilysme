package io.riskily.sme.regulatory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "regulatory_notices")
class RegulatoryNotice(

    @Column(nullable = false)
    var title: String,

    @Column(name = "title_sw", nullable = false)
    var titleSw: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "description_sw", columnDefinition = "TEXT")
    var descriptionSw: String? = null,

    @Column(nullable = false, length = 20)
    var authority: String,

    @Column(name = "recurring_day_of_month", nullable = false)
    var recurringDayOfMonth: Int,

    /** NULL = monthly obligation; 1-12 = annual (fires only in that specific month). */
    @Column(name = "recurring_month")
    var recurringMonth: Int? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
