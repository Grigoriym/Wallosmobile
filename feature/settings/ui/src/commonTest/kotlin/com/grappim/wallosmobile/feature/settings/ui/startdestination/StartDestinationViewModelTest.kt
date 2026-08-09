package com.grappim.wallosmobile.feature.settings.ui.startdestination

import app.cash.turbine.test
import com.grappim.wallosmobile.core.storage.startdestination.StartDestination
import com.grappim.wallosmobile.core.storage.startdestination.StartDestinationStorage
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StartDestinationViewModelTest {

    private val startDestinationStorage = FakeStartDestinationStorage()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel() = StartDestinationViewModel(startDestinationStorage = startDestinationStorage)

    @Test
    fun `the stored destination is what the radio group shows`() = runTest {
        startDestinationStorage.destination.value = StartDestination.Subscriptions

        assertEquals(StartDestination.Subscriptions, viewModel().uiState.value.selected)
    }

    @Test
    fun `selecting a destination stores it`() = runTest {
        viewModel().uiState.value.onSelect(StartDestination.Categories)

        assertEquals(listOf(StartDestination.Categories), startDestinationStorage.stored)
    }

    /*
     * The selection is not held locally: it is read back off the flow, which is the same one the
     * shell collects. A ViewModel that echoed the tap instead could show Household while the app
     * actually started on Dashboard.
     */
    @Test
    fun `the selection follows storage, not the tap`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(StartDestination.Dashboard, awaitItem().selected)

            viewModel.uiState.value.onSelect(StartDestination.Household)
            assertEquals(StartDestination.Household, awaitItem().selected)

            // Storage changed by something else — a second screen, or a restore — and the group
            // moves with it.
            startDestinationStorage.destination.value = StartDestination.Currencies
            assertEquals(StartDestination.Currencies, awaitItem().selected)
        }
    }

    @Test
    fun `a failed write leaves the previous destination selected`() = runTest {
        startDestinationStorage.destination.value = StartDestination.Subscriptions
        startDestinationStorage.failOnSet = true
        val viewModel = viewModel()

        viewModel.uiState.value.onSelect(StartDestination.Settings)

        assertEquals(StartDestination.Subscriptions, viewModel.uiState.value.selected)
    }

    // Private to this file, as in `InterfaceViewModelTest`: `:testing` is for doubles more than
    // one module needs, and this one has a single consumer.
    private class FakeStartDestinationStorage : StartDestinationStorage {

        val destination = MutableStateFlow(StartDestination.default())
        val stored = mutableListOf<StartDestination>()
        var failOnSet = false

        override val startDestination: Flow<StartDestination> = destination

        override suspend fun setStartDestination(value: StartDestination) {
            if (failOnSet) error("the store is unwritable")
            stored += value
            destination.value = value
        }
    }
}
