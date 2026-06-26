package io.riskily.sme.regulatory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant

enum class DeadlineStatus {
    DONE,
    IN_PROGRESS,
    REMIND_ME,
}

@Entity
@Table(name = "user_deadline_statuses")
class UserDeadlineStatus(

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "regulatory_notice_id", nullable = false)
    var regulatoryNoticeId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DeadlineStatus = DeadlineStatus.REMIND_ME,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
