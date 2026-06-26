package io.riskily.sme.regulatory

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserDeadlineStatusRepository : JpaRepository<UserDeadlineStatus, Long> {

    fun findByUserId(userId: Long): List<UserDeadlineStatus>

    fun findByUserIdAndRegulatoryNoticeId(
        userId: Long,
        regulatoryNoticeId: Long,
    ): Optional<UserDeadlineStatus>
}
