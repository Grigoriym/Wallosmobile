package com.grappim.wallosmobile.feature.dashboard.domain.model

/**
 * The monthly budget sub-rows the web's "Monthly Budget" card shows alongside [MonthlyCost]
 * (`stats_calculations.php:299–304`) — not its own endpoint, just `feature:profile`'s
 * `User.budget` compared against this month's already-fetched cost. [from] mirrors
 * `isset($userData['budget']) && $userData['budget'] > 0` exactly: absent (`null`), not a
 * zeroed-out instance, when there's no budget set to compare against.
 */
data class MonthlyBudget(
    val amount: Double,
    val used: Double,
    val remaining: Double,
    val overBudget: Double,
    val isOverBudget: Boolean
) {
    companion object {
        fun from(budget: Double, monthlyCost: Double): MonthlyBudget? {
            if (budget <= 0) return null

            val isOverBudget = monthlyCost > budget
            return MonthlyBudget(
                amount = budget,
                used = minOf(100.0, (monthlyCost / budget) * 100),
                remaining = maxOf(0.0, budget - monthlyCost),
                overBudget = if (isOverBudget) monthlyCost - budget else 0.0,
                isOverBudget = isOverBudget
            )
        }
    }
}
