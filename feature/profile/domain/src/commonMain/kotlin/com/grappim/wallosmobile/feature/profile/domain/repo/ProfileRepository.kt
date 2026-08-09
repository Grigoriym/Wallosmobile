package com.grappim.wallosmobile.feature.profile.domain.repo

import com.grappim.wallosmobile.feature.profile.domain.model.BudgetPeriodType
import com.grappim.wallosmobile.feature.profile.domain.model.User
import kotlinx.datetime.LocalDate

/**
 * `get_user.php` / `set_budget.php` (`docs/WALLOS_API.md` §3.8–3.9) — a single-row profile
 * snapshot, hand-written against `WallosApiClient` like `DashboardRepository`: neither `core:crud`
 * (no add/edit/delete here) nor a cache fits a single-row endpoint with no list to page through.
 *
 * A [Result] failure is always a [com.grappim.wallosmobile.core.domain.WallosError].
 */
interface ProfileRepository {

    suspend fun getUser(): Result<User>

    /**
     * [periodType] and [anchorDate] always ride along, even when only [monthlyBudget] changed —
     * `set_budget.php` resets both to `monthly`/today the moment `period_budget` (or any period
     * field) is sent without them (API doc §3.8), so a partial call would silently overwrite a
     * caller's existing period settings.
     */
    suspend fun setBudget(
        monthlyBudget: Double,
        periodBudget: Double,
        periodType: BudgetPeriodType,
        anchorDate: LocalDate
    ): Result<Unit>
}
