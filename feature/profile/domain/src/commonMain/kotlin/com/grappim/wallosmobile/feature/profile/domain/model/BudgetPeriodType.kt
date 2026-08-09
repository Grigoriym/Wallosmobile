package com.grappim.wallosmobile.feature.profile.domain.model

/**
 * `budget_period_type` (`docs/WALLOS_API.md` §3.8) — a closed, server-defined set, same shape as
 * `BillingCycle`'s wire mapping. [fromWireValue] defaults to [MONTHLY] for anything unrecognized,
 * mirroring `set_budget.php`'s own `sanitizeBudgetPeriodType` fallback rather than crashing or
 * going nullable on a value this field can never actually be absent for.
 */
enum class BudgetPeriodType(val wireValue: String) {
    WEEKLY("weekly"),
    FORTNIGHTLY("fortnightly"),
    MONTHLY("monthly");

    companion object {
        fun fromWireValue(value: String): BudgetPeriodType = entries.firstOrNull { it.wireValue == value } ?: MONTHLY
    }
}
