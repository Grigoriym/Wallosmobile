package com.grappim.wallosmobile.feature.profile.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.post
import com.grappim.wallosmobile.feature.profile.dto.UserDTO
import com.grappim.wallosmobile.feature.profile.dto.UserResponse
import kotlinx.serialization.json.JsonObject
import org.koin.core.annotation.Single

/** `get_user.php` / `set_budget.php` (API doc §3.8–3.9), unwrapping the envelope. */
interface ProfileApi {

    suspend fun getUser(): UserDTO

    /** [fields] is built by the repository — see `ProfileRepository.setBudget`'s own doc. */
    suspend fun setBudget(fields: FormParams)
}

@Single(binds = [ProfileApi::class])
internal class ProfileApiImpl(private val apiClient: WallosApiClient) : ProfileApi {

    override suspend fun getUser(): UserDTO = apiClient.post<UserResponse>(GET_USER_PATH).user

    override suspend fun setBudget(fields: FormParams) {
        apiClient.post<JsonObject>(SET_BUDGET_PATH, fields)
    }

    private companion object {
        // Relative, no leading slash — a leading one discards the subpath of an install that
        // lives under e.g. `/wallos` (plan §4.1).
        const val GET_USER_PATH = "api/users/get_user.php"
        const val SET_BUDGET_PATH = "api/users/set_budget.php"
    }
}
