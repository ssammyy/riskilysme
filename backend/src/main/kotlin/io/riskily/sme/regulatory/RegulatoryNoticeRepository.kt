package io.riskily.sme.regulatory

import org.springframework.data.jpa.repository.JpaRepository

interface RegulatoryNoticeRepository : JpaRepository<RegulatoryNotice, Long> {
    fun findByIsActiveTrue(): List<RegulatoryNotice>
}
