package com.grappim.wallosmobile.feature.household.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.crud.CrudApi
import com.grappim.wallosmobile.core.crud.CrudEndpoint
import com.grappim.wallosmobile.core.crud.WallosCrudApi
import com.grappim.wallosmobile.feature.household.dto.HouseholdMemberDTO
import org.koin.core.annotation.Single

/** `get_household.php` / `set_household.php` (`docs/WALLOS_API.md` §3.10), nothing of its own. */
interface HouseholdApi : CrudApi<HouseholdMemberDTO>

@Single(binds = [HouseholdApi::class])
internal class HouseholdApiImpl(apiClient: WallosApiClient) :
    HouseholdApi,
    CrudApi<HouseholdMemberDTO> by WallosCrudApi(apiClient, HOUSEHOLD_ENDPOINT, HouseholdMemberDTO.serializer())

private val HOUSEHOLD_ENDPOINT = CrudEndpoint(
    getPath = "api/household/get_household.php",
    setPath = "api/household/set_household.php",
    listKey = "household",
    idParam = "memberId"
)
