package com.grappim.wallosmobile.feature.profile.data

import com.grappim.wallosmobile.core.api.WallosApiClient
import com.grappim.wallosmobile.core.api.post
import com.grappim.wallosmobile.feature.profile.dto.UserDTO
import com.grappim.wallosmobile.feature.profile.dto.UserResponse
import org.koin.core.annotation.Single

/** The one read endpoint M10 needs (API doc §3.9), unwrapping the envelope. */
interface ProfileApi {

    suspend fun getUser(): UserDTO
}

@Single(binds = [ProfileApi::class])
internal class ProfileApiImpl(private val apiClient: WallosApiClient) : ProfileApi {

    override suspend fun getUser(): UserDTO = apiClient.post<UserResponse>(GET_USER_PATH).user

    private companion object {
        // Relative, no leading slash — a leading one discards the subpath of an install that
        // lives under e.g. `/wallos` (plan §4.1).
        const val GET_USER_PATH = "api/users/get_user.php"
    }
}
