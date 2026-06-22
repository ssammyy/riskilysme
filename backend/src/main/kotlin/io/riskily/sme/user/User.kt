package io.riskily.sme.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * Riskily SME user account: authentication + role + the onboarding profile.
 * Profile fields are nullable until onboarding is completed.
 */
@Entity
@Table(name = "users")
class User(

    @Column(nullable = false)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    // --- Onboarding profile (retained for legacy data; not used by ExposureCalculator) ---
    @Column(name = "first_name")
    var firstName: String? = null,

    @Column(name = "business_name")
    var businessName: String? = null,

    @Column(name = "business_type")
    var businessType: String? = null,

    @Column(name = "employee_range")
    var employeeRange: String? = null,

    @Column(name = "import_behaviour")
    var importBehaviour: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_methods", nullable = false)
    var paymentMethods: MutableList<String> = mutableListOf(),

    @Column(name = "biggest_cost")
    var biggestCost: String? = null,

    // --- AMD-006 v3.0 onboarding answers (8 Yes/No + conditional Q2b) ---
    // All nullable: NULL means not yet answered; ExposureCalculator treats null as No (baseline).

    @Column(name = "q1_fx_yes")
    var q1FxYes: Boolean? = null,

    @Column(name = "q2_loans_yes")
    var q2LoansYes: Boolean? = null,

    /** Q2b: interest rate band. Only populated when q2LoansYes = true. */
    @Column(name = "q2b_interest_rate", length = 20)
    var q2bInterestRate: String? = null,

    @Column(name = "q3_credit_sales_yes")
    var q3CreditSalesYes: Boolean? = null,

    @Column(name = "q4_fixed_costs_yes")
    var q4FixedCostsYes: Boolean? = null,

    @Column(name = "q5_concentration_yes")
    var q5ConcentrationYes: Boolean? = null,

    @Column(name = "q6_cash_timing_yes")
    var q6CashTimingYes: Boolean? = null,

    @Column(name = "q7_supplier_dep_yes")
    var q7SupplierDepYes: Boolean? = null,

    @Column(name = "q8_informal_credit_yes")
    var q8InformalCreditYes: Boolean? = null,

    // --- Preferences & subscription ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    var language: Language = Language.EN,

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 20)
    var subscriptionTier: SubscriptionTier = SubscriptionTier.BASIC,

    @Column(name = "monthly_alert_count", nullable = false)
    var monthlyAlertCount: Int = 0,

    @Column(name = "onboarding_completed_at")
    var onboardingCompletedAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
