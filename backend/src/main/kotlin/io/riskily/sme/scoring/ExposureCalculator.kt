package io.riskily.sme.scoring

import io.riskily.sme.user.User
import org.springframework.stereotype.Component

/**
 * AMD-006 v3.0 §4 — Dynamic PRISM Weight Engine.
 *
 * Returns per-module exposure (0..1) that represents how much that module
 * is expected to be under pressure for this user. The values are used as
 * weights in the final health score formula:
 *   health = 100 − Σ(exposure[m] × pressure[m]) × 100
 *
 * Algorithm:
 * 1. Start from AMD-006 base weights.
 * 2. Add cumulative deltas for each Yes answer (Q1–Q8).
 * 3. Apply Q2b credit-rate boost directly to CREDIT (if Q2=Yes).
 * 4. Floor each weight at 0.03, ceiling at 0.40.
 * 5. Normalise so all weights sum to 1.00.
 *
 * Null answers (user not yet onboarded, or migrated legacy user) are treated
 * as No, giving baseline weights.
 *
 * Module mapping:
 *   AMD-006 "FX Risk"              → ModuleCode.FX
 *   AMD-006 "Credit Risk"          → ModuleCode.CREDIT
 *   AMD-006 "Counterparty Risk"    → ModuleCode.COUNTERPARTY
 *   AMD-006 "Running Costs Risk"   → ModuleCode.COMMODITY
 *   AMD-006 "Liquidity Risk"       → ModuleCode.LIQUIDITY
 *   AMD-006 "Regulatory Risk"      → ModuleCode.REGULATORY
 *   AMD-006 "Political & Sovereign"→ ModuleCode.MACRO
 */
@Component
class ExposureCalculator {

    fun compute(user: User): Map<ModuleCode, Double> {
        // Step 1: AMD-006 base weights (sum = 1.00)
        val w = mutableMapOf(
            ModuleCode.FX           to 0.05,
            ModuleCode.CREDIT       to 0.10,
            ModuleCode.COUNTERPARTY to 0.20,
            ModuleCode.COMMODITY    to 0.20,
            ModuleCode.LIQUIDITY    to 0.25,
            ModuleCode.REGULATORY   to 0.05,
            ModuleCode.MACRO        to 0.15,
        )

        // Step 2: cumulative deltas from Yes answers
        // Q1 — FX exposure: foreign currency payments / sales
        if (user.q1FxYes == true) {
            w[ModuleCode.FX] = w[ModuleCode.FX]!! + 0.15
            w[ModuleCode.MACRO] = w[ModuleCode.MACRO]!! + 0.05
        }

        // Q2 — Loans / overdrafts
        if (user.q2LoansYes == true) {
            w[ModuleCode.CREDIT] = w[ModuleCode.CREDIT]!! + 0.15
            w[ModuleCode.LIQUIDITY] = w[ModuleCode.LIQUIDITY]!! + 0.05
        }

        // Q3 — Credit sales / debtors
        if (user.q3CreditSalesYes == true) {
            w[ModuleCode.COUNTERPARTY] = w[ModuleCode.COUNTERPARTY]!! + 0.10
            w[ModuleCode.LIQUIDITY] = w[ModuleCode.LIQUIDITY]!! + 0.05
        }

        // Q4 — High fixed costs (fuel / electricity / rent / water)
        if (user.q4FixedCostsYes == true) {
            w[ModuleCode.COMMODITY] = w[ModuleCode.COMMODITY]!! + 0.10
        }

        // Q5 — Concentration: one entity ≥ 40% of business survival
        if (user.q5ConcentrationYes == true) {
            w[ModuleCode.COUNTERPARTY] = w[ModuleCode.COUNTERPARTY]!! + 0.10
        }

        // Q6 — Cash timing gap (owed money but can't pay bills now)
        if (user.q6CashTimingYes == true) {
            w[ModuleCode.LIQUIDITY] = w[ModuleCode.LIQUIDITY]!! + 0.10
        }

        // Q7 — Supplier dependency (relies on 1–2 key suppliers)
        if (user.q7SupplierDepYes == true) {
            w[ModuleCode.COUNTERPARTY] = w[ModuleCode.COUNTERPARTY]!! + 0.05
            w[ModuleCode.COMMODITY] = w[ModuleCode.COMMODITY]!! + 0.05
        }

        // Q8 — Informal / mobile credit (chamas / M-Shwari / digital loans)
        if (user.q8InformalCreditYes == true) {
            w[ModuleCode.CREDIT] = w[ModuleCode.CREDIT]!! + 0.10
        }

        // Step 3: Q2b — interest rate band boosts CREDIT exposure directly
        // This is a direct score input per AMD-006, not a weight delta.
        // Applied only when Q2 = Yes (which is when q2bInterestRate is meaningful).
        if (user.q2LoansYes == true) {
            val creditBoost = when (user.q2bInterestRate) {
                "above_20" -> 0.10
                "15_to_20" -> 0.05
                "below_15" -> 0.00
                else       -> 0.05   // "not_sure" → conservative moderate boost
            }
            w[ModuleCode.CREDIT] = w[ModuleCode.CREDIT]!! + creditBoost
        }

        // Step 4: floor 0.03, ceiling 0.40
        val capped = w.mapValues { (_, v) -> v.coerceIn(FLOOR, CEILING) }.toMutableMap()

        // Step 5: normalise to 1.00
        val total = capped.values.sum()
        return capped.mapValues { (_, v) -> v / total }
    }

    companion object {
        private const val FLOOR   = 0.03
        private const val CEILING = 0.40
    }
}
