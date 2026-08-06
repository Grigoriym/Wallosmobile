package com.grappim.wallosmobile.feature.household.domain.repo

import com.grappim.wallosmobile.feature.household.domain.model.HouseholdMember

/**
 * Household members, straight off `get_household.php` / `set_household.php`
 * (`docs/WALLOS_API.md` §3.10) — reference data, so unlike
 * [com.grappim.wallosmobile.feature.subscriptions.domain.repo] there is no cache to read instead:
 * every call is a round trip.
 *
 * A [Result] failure is always a [com.grappim.wallosmobile.core.domain.WallosError], and a
 * [deleteMember] on a member still referenced by a subscription is
 * [com.grappim.wallosmobile.core.domain.WallosError.InUse].
 */
interface HouseholdRepository {

    suspend fun getMembers(): Result<List<HouseholdMember>>

    /** @return the id the server assigned the new member. */
    suspend fun addMember(name: String, email: String): Result<Int>

    suspend fun editMember(id: Int, name: String, email: String): Result<Unit>

    suspend fun deleteMember(id: Int): Result<Unit>
}
