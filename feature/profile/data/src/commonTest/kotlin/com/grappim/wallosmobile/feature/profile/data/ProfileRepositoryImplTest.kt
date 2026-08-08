package com.grappim.wallosmobile.feature.profile.data

import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.profile.data.mapper.UserMapper
import com.grappim.wallosmobile.feature.profile.dto.UserDTO
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProfileRepositoryImplTest {

    @Test
    fun `getUser maps the DTO through the mapper`() = runTest {
        val api = FakeProfileApi(user = UserDTO(id = 1, budget = 150.5, periodBudget = 25.0, mainCurrency = 1))

        val result = repository(api).getUser().getOrThrow()

        assertEquals(1, result.id)
        assertEquals(150.5, result.budget)
        assertEquals(25.0, result.periodBudget)
        assertEquals(1, result.mainCurrencyId)
    }

    @Test
    fun `a failed getUser surfaces the WallosError`() = runTest {
        val api = FakeProfileApi(failure = WallosError.Server("502 Bad Gateway"))

        assertFailsWith<WallosError.Server> { repository(api).getUser().getOrThrow() }
    }

    private fun repository(api: ProfileApi): ProfileRepositoryImpl = ProfileRepositoryImpl(
        api = api,
        userMapper = UserMapper(),
        dispatcher = UnconfinedTestDispatcher()
    )

    private class FakeProfileApi(private val user: UserDTO? = null, private val failure: Throwable? = null) :
        ProfileApi {

        override suspend fun getUser(): UserDTO {
            failure?.let { throw it }
            return user ?: error("user not set")
        }
    }
}
