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

    // --- Onboarding profile ---
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
