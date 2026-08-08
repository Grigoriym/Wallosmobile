package com.grappim.wallosmobile.feature.dashboard.domain.repo

import com.grappim.wallosmobile.feature.dashboard.domain.model.MonthlyCost
import com.grappim.wallosmobile.feature.dashboard.domain.model.PeriodBudget
import kotlinx.datetime.LocalDate

/**
 * `get_monthly_cost.php` / `get_period_budget.php` (`docs/WALLOS_API.md` §3.5–3.6) — period
 * snapshots, not a list to page through offline, so unlike
 * [com.grappim.wallosmobile.feature.subscriptions.domain.repo] there is no cache behind either
 * call: every call is a round trip.
 *
 * A [Result] failure is always a [com.grappim.wallosmobile.core.domain.WallosError].
 * [getPeriodBudget] fails with `WallosError.UnsupportedEndpoint` on an instance old enough not to
 * have the endpoint at all — no stored version is checked ahead of the call (M8's own preamble in
 * `docs/CHECKLIST.md`).
 */
interface DashboardRepository {

    suspend fun getMonthlyCost(month: Int, year: Int): Result<MonthlyCost>

    /** [referenceDate] `null` lets the server default to its own idea of today. */
    suspend fun getPeriodBudget(referenceDate: LocalDate? = null): Result<PeriodBudget>
}
