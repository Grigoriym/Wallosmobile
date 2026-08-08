package com.grappim.wallosmobile.feature.profile.data

import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.profile.data.mapper.UserMapper
import com.grappim.wallosmobile.feature.profile.domain.model.User
import com.grappim.wallosmobile.feature.profile.domain.repo.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/** No cache behind the call (like `DashboardRepository`) — a single-row snapshot, not a list. */
@Single(binds = [ProfileRepository::class])
internal class ProfileRepositoryImpl(
    private val api: ProfileApi,
    private val userMapper: UserMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : ProfileRepository {

    override suspend fun getUser(): Result<User> = resultOf {
        withContext(dispatcher) { userMapper.toDomain(api.getUser()) }
    }
}
