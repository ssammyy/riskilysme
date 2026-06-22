package io.riskily.sme.onboarding

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * AMD-006 v3.0 onboarding: 8 Yes/No questions + conditional Q2b interest rate.
 * businessName is a profile field, not scored. First name is captured at sign-up, not here.
 */
data class OnboardingRequest(
    @field:NotBlank @field:Size(max = 200) val businessName: String,

    // Q1 — FX Risk
    val q1FxYes: Boolean,

    // Q2 — Credit Risk (loans / overdrafts)
    val q2LoansYes: Boolean,

    // Q2b — only present when q2LoansYes = true; null otherwise
    val q2bInterestRate: String? = null,

    // Q3 — Counterparty Risk (credit sales / debtors)
    val q3CreditSalesYes: Boolean,

    // Q4 — Running Costs Risk (fuel / electricity / rent / water)
    val q4FixedCostsYes: Boolean,

    // Q5 — Counterparty concentration (one entity = 40%+ of survival)
    val q5ConcentrationYes: Boolean,

    // Q6 — Liquidity timing gap
    val q6CashTimingYes: Boolean,

    // Q7 — Counterparty supply concentration
    val q7SupplierDepYes: Boolean,

    // Q8 — Informal / mobile credit
    val q8InformalCreditYes: Boolean,
)
