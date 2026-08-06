package com.grappim.wallosmobile.feature.household.data

import com.grappim.wallosmobile.core.api.FormParams
import com.grappim.wallosmobile.core.domain.WallosError
import com.grappim.wallosmobile.feature.household.dto.HouseholdMemberDTO
import com.grappim.wallosmobile.feature.household.mapper.HouseholdMemberMapper
import com.grappim.wallosmobile.feature.subscriptions.mapper.HtmlUnescaper
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HouseholdRepositoryImplTest {

    @Test
    fun `getMembers maps every row through the mapper`() = runTest {
        val api = FakeHouseholdApi(
            all = listOf(HouseholdMemberDTO(id = 1, name = "Mom &amp; Dad", email = "m@e.com"))
        )

        val result = repository(api).getMembers().getOrThrow()

        assertEquals(listOf("Mom & Dad"), result.map { it.name })
    }

    @Test
    fun `a failed getMembers surfaces the WallosError`() = runTest {
        val api = FakeHouseholdApi(allFailure = WallosError.Server("502 Bad Gateway"))

        assertFailsWith<WallosError.Server> { repository(api).getMembers().getOrThrow() }
    }

    @Test
    fun `addMember sends the name and email and returns the new id`() = runTest {
        val api = FakeHouseholdApi(addedId = 9)

        val result = repository(api).addMember("Jane Doe", "jane@example.com").getOrThrow()

        assertEquals(9, result)
        assertEquals("Jane Doe", api.addedFields?.asMap()?.get("name"))
        assertEquals("jane@example.com", api.addedFields?.asMap()?.get("email"))
    }

    @Test
    fun `editMember sends the id and the new name and email`() = runTest {
        val api = FakeHouseholdApi()

        repository(api).editMember(7, "Renamed", "renamed@example.com").getOrThrow()

        val (id, fields) = api.editCalls.single()
        assertEquals(7, id)
        assertEquals("Renamed", fields.asMap()["name"])
        assertEquals("renamed@example.com", fields.asMap()["email"])
    }

    @Test
    fun `deleteMember forwards the id`() = runTest {
        val api = FakeHouseholdApi()

        repository(api).deleteMember(4).getOrThrow()

        assertEquals(listOf(4), api.deleteCalls)
    }

    @Test
    fun `a delete on a referenced member surfaces as InUse`() = runTest {
        val api = FakeHouseholdApi(deleteFailure = WallosError.InUse("Household member in use"))

        assertFailsWith<WallosError.InUse> { repository(api).deleteMember(4).getOrThrow() }
    }

    private fun repository(api: HouseholdApi = FakeHouseholdApi()): HouseholdRepositoryImpl = HouseholdRepositoryImpl(
        api = api,
        mapper = HouseholdMemberMapper(HtmlUnescaper()),
        dispatcher = UnconfinedTestDispatcher()
    )

    private class FakeHouseholdApi(
        private val all: List<HouseholdMemberDTO>? = null,
        private val addedId: Int = 0,
        private val allFailure: Throwable? = null,
        private val addFailure: Throwable? = null,
        private val editFailure: Throwable? = null,
        private val deleteFailure: Throwable? = null
    ) : HouseholdApi {

        var addedFields: FormParams? = null
            private set
        val editCalls = mutableListOf<Pair<Int, FormParams>>()
        val deleteCalls = mutableListOf<Int>()

        override suspend fun getAll(): List<HouseholdMemberDTO> {
            allFailure?.let { throw it }
            return all ?: error("household not set")
        }

        override suspend fun add(fields: FormParams): Int {
            addFailure?.let { throw it }
            addedFields = fields
            return addedId
        }

        override suspend fun edit(id: Int, fields: FormParams) {
            editFailure?.let { throw it }
            editCalls += id to fields
        }

        override suspend fun delete(id: Int) {
            deleteFailure?.let { throw it }
            deleteCalls += id
        }
    }
}
