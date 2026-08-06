package com.grappim.wallosmobile.feature.household.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.asynckmp.IoDispatcher
import com.grappim.wallosmobile.core.domain.resultOf
import com.grappim.wallosmobile.feature.household.domain.model.HouseholdMember
import com.grappim.wallosmobile.feature.household.domain.repo.HouseholdRepository
import com.grappim.wallosmobile.feature.household.mapper.HouseholdMemberMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * No cache behind this one (unlike `SubscriptionsRepository`, 3.4) — household members are
 * reference data with no offline requirement in this milestone, so every call is a round trip to
 * [HouseholdApi].
 */
@Single(binds = [HouseholdRepository::class])
internal class HouseholdRepositoryImpl(
    private val api: HouseholdApi,
    private val mapper: HouseholdMemberMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : HouseholdRepository {

    override suspend fun getMembers(): Result<List<HouseholdMember>> = resultOf {
        withContext(dispatcher) { api.getAll().map(mapper::toDomain) }
    }

    override suspend fun addMember(name: String, email: String): Result<Int> = resultOf {
        withContext(dispatcher) {
            api.add(FormParams().put(PARAM_NAME, name).put(PARAM_EMAIL, email))
        }
    }

    override suspend fun editMember(id: Int, name: String, email: String): Result<Unit> = resultOf {
        withContext(dispatcher) {
            api.edit(id, FormParams().put(PARAM_NAME, name).put(PARAM_EMAIL, email))
        }
    }

    override suspend fun deleteMember(id: Int): Result<Unit> = resultOf {
        withContext(dispatcher) { api.delete(id) }
    }

    private companion object {
        const val PARAM_NAME = "name"
        const val PARAM_EMAIL = "email"
    }
}
