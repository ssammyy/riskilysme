package io.riskily.sme.onboarding

import io.riskily.sme.scoring.ScoreResponse
import io.riskily.sme.scoring.ScoreService
import io.riskily.sme.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OnboardingService(
    private val users: UserRepository,
    private val scoreService: ScoreService,
) {

    /**
     * Persist AMD-006 v3.0 Yes/No answers, stamp completion, and compute the starting score.
     * businessName + firstName are profile labels, not scored.
     */
    @Transactional
    fun complete(userId: Long, request: OnboardingRequest): ScoreResponse {
        validate(request)
        val user = users.findById(userId).orElseThrow { IllegalArgumentException("User not found") }

        user.businessName = request.businessName.trim()
        // First name comes from sign-up; onboarding no longer collects it.

        // AMD-006 Q1–Q8
        user.q1FxYes = request.q1FxYes
        user.q2LoansYes = request.q2LoansYes
        user.q2bInterestRate = if (request.q2LoansYes) request.q2bInterestRate else null
        user.q3CreditSalesYes = request.q3CreditSalesYes
        user.q4FixedCostsYes = request.q4FixedCostsYes
        user.q5ConcentrationYes = request.q5ConcentrationYes
        user.q6CashTimingYes = request.q6CashTimingYes
        user.q7SupplierDepYes = request.q7SupplierDepYes
        user.q8InformalCreditYes = request.q8InformalCreditYes

        user.onboardingCompletedAt = Instant.now()
        users.save(user)

        return scoreService.recalculateAndPersist(user)
    }

    private fun validate(r: OnboardingRequest) {
        if (r.q2LoansYes && r.q2bInterestRate != null) {
            require(r.q2bInterestRate in OnboardingOptions.INTEREST_RATE_BANDS) {
                "Invalid q2bInterestRate: ${r.q2bInterestRate}"
            }
        }
    }
}
