package com.grappim.wallosmobile.feature.profile.domain.repo

import com.grappim.wallosmobile.feature.profile.domain.model.User

/**
 * `get_user.php` (`docs/WALLOS_API.md` §3.9) — a single-row profile snapshot, hand-written
 * against `WallosApiClient` like `DashboardRepository`: neither `core:crud` (no add/edit/delete
 * here yet) nor a cache fits a single-row endpoint with no list to page through.
 *
 * A [Result] failure is always a [com.grappim.wallosmobile.core.domain.WallosError].
 */
interface ProfileRepository {

    suspend fun getUser(): Result<User>
}
