package com.grappim.wallosmobile.feature.settings.ui.customheaders

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeader
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeaderError
import com.grappim.wallosmobile.core.storage.customheaders.CustomHeaderValidator
import com.grappim.wallosmobile.testing.FakeCustomHeadersStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomHeadersViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel(storage: FakeCustomHeadersStorage) =
        CustomHeadersViewModel(customHeadersStorage = storage, customHeaderValidator = CustomHeaderValidator())

    @Test
    fun `stored headers are offered back as rows`() = runTest {
        val viewModel = viewModel(FakeCustomHeadersStorage(listOf(AUTH)))

        assertEquals(listOf(CustomHeaderRow(AUTH.name, AUTH.value)), viewModel.uiState.value.rows)
    }

    @Test
    fun `nothing changed means nothing to save`() = runTest {
        val viewModel = viewModel(FakeCustomHeadersStorage(listOf(AUTH)))

        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `a new header is saved trimmed and blank rows are dropped`() = runTest {
        val storage = FakeCustomHeadersStorage()
        val viewModel = viewModel(storage)
        val state = { viewModel.uiState.value }

        state().onAdd()
        state().onNameChange(0, " Authorization ")
        state().onValueChange(0, " Basic cGFuZ29saW46c2VjcmV0 ")
        state().onAdd()
        assertTrue(state().isSaveEnabled)
        state().onSaveClick()

        assertEquals(listOf(listOf(AUTH)), storage.setHeadersCalledWith)
        assertEquals(listOf(CustomHeaderRow(AUTH.name, AUTH.value)), state().rows)
        assertFalse(state().isSaveEnabled)
    }

    @Test
    fun `removing every header saves an empty list`() = runTest {
        val storage = FakeCustomHeadersStorage(listOf(AUTH))
        val viewModel = viewModel(storage)

        viewModel.uiState.value.onRemove(0)
        viewModel.uiState.value.onSaveClick()

        assertEquals(listOf(emptyList()), storage.setHeadersCalledWith)
        assertTrue(viewModel.uiState.value.rows.isEmpty())
    }

    @Test
    fun `an invalid row blocks the save`() = runTest {
        val storage = FakeCustomHeadersStorage()
        val viewModel = viewModel(storage)
        val state = { viewModel.uiState.value }

        state().onAdd()
        state().onNameChange(0, "Host")
        state().onValueChange(0, "wallos.lan")

        assertEquals(CustomHeaderError.ReservedName, state().rows.single().error)
        assertFalse(state().isSaveEnabled)
        state().onSaveClick()
        assertTrue(storage.setHeadersCalledWith.isEmpty())
    }

    @Test
    fun `a duplicate name marks the second row`() = runTest {
        val viewModel = viewModel(FakeCustomHeadersStorage(listOf(AUTH)))
        val state = { viewModel.uiState.value }

        state().onAdd()
        state().onNameChange(1, "authorization")

        assertEquals(listOf(null, CustomHeaderError.DuplicateName), state().rows.map { it.error })
    }

    @Test
    fun `an edit in progress survives a storage emission`() = runTest {
        val storage = FakeCustomHeadersStorage(listOf(AUTH))
        val viewModel = viewModel(storage)

        viewModel.uiState.value.onValueChange(0, "Basic d3Jvbmc=")
        storage.setHeaders(listOf(OTHER))

        assertEquals("Basic d3Jvbmc=", viewModel.uiState.value.rows.single().value)
        assertEquals(listOf(OTHER), viewModel.uiState.value.saved)
    }

    private companion object {
        val AUTH = CustomHeader(name = "Authorization", value = "Basic cGFuZ29saW46c2VjcmV0")
        val OTHER = CustomHeader(name = "X-Token", value = "abc")
    }
}
