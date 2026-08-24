package com.grappim.wallosmobile.feature.settings.ui.appearance

import app.cash.turbine.test
import com.grappim.wallosmobile.core.storage.crashreporting.CrashReportingStorage
import com.grappim.wallosmobile.core.storage.theme.ThemeMode
import com.grappim.wallosmobile.core.storage.theme.ThemeStorage
import com.grappim.wallosmobile.testing.FakeCrashReporter
import com.grappim.wallosmobile.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterfaceViewModelTest {

    private val themeStorage = FakeThemeStorage()
    private val crashReportingStorage = FakeCrashReportingStorage()
    private val crashReporter = FakeCrashReporter()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun viewModel() = InterfaceViewModel(
        themeStorage = themeStorage,
        crashReportingStorage = crashReportingStorage,
        crashReporter = crashReporter
    )

    @Test
    fun `the stored mode is what the radio group shows`() = runTest {
        themeStorage.mode.value = ThemeMode.Dark

        assertEquals(ThemeMode.Dark, viewModel().uiState.value.selectedMode)
    }

    @Test
    fun `selecting a mode stores it`() = runTest {
        viewModel().uiState.value.onModeSelect(ThemeMode.Light)

        assertEquals(listOf(ThemeMode.Light), themeStorage.stored)
    }

    /*
     * The selection is not held locally: it is read back off the flow, which is the same one the
     * shell collects. A ViewModel that echoed the tap instead could show Dark while the app drew
     * light.
     */
    @Test
    fun `the selection follows storage, not the tap`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(ThemeMode.System, awaitItem().selectedMode)

            viewModel.uiState.value.onModeSelect(ThemeMode.Dark)
            assertEquals(ThemeMode.Dark, awaitItem().selectedMode)

            // Storage changed by something else — a second screen, or a restore — and the group
            // moves with it.
            themeStorage.mode.value = ThemeMode.Light
            assertEquals(ThemeMode.Light, awaitItem().selectedMode)
        }
    }

    @Test
    fun `a failed write leaves the previous mode selected`() = runTest {
        themeStorage.mode.value = ThemeMode.Dark
        themeStorage.failOnSet = true
        val viewModel = viewModel()

        viewModel.uiState.value.onModeSelect(ThemeMode.Light)

        assertEquals(ThemeMode.Dark, viewModel.uiState.value.selectedMode)
    }

    // `isCrashReportingAvailable` gates whether the toggle renders at all (fdroid vs. gplay,
    // 16.4) — it is a build/flavor fact read once, so no flow to await here.
    @Test
    fun `the toggle is available exactly when the crash reporter is`() {
        crashReporter.isAvailable = true

        assertTrue(viewModel().uiState.value.isCrashReportingAvailable)
    }

    @Test
    fun `the toggle is unavailable when the crash reporter is not`() {
        crashReporter.isAvailable = false

        assertFalse(viewModel().uiState.value.isCrashReportingAvailable)
    }

    @Test
    fun `the stored consent is what the toggle shows`() = runTest {
        crashReportingStorage.enabled.value = true

        assertTrue(viewModel().uiState.value.crashReportingEnabled)
    }

    @Test
    fun `toggling crash reporting stores the new consent`() = runTest {
        viewModel().uiState.value.onCrashReportingToggle(true)

        assertEquals(listOf(true), crashReportingStorage.stored)
    }

    // Private to this file, as in 1.10, 2.3 and `SettingsViewModelTest`: `:testing` is for doubles
    // more than one module needs, and this one has a single consumer.
    private class FakeThemeStorage : ThemeStorage {

        val mode = MutableStateFlow(ThemeMode.System)
        val stored = mutableListOf<ThemeMode>()
        var failOnSet = false

        override val themeMode: Flow<ThemeMode> = mode

        override suspend fun setThemeMode(mode: ThemeMode) {
            if (failOnSet) error("the store is unwritable")
            stored += mode
            this.mode.value = mode
        }
    }

    private class FakeCrashReportingStorage : CrashReportingStorage {

        val enabled = MutableStateFlow(false)
        val stored = mutableListOf<Boolean>()

        override val crashReportingEnabled: Flow<Boolean> = enabled

        override suspend fun setCrashReportingEnabled(enabled: Boolean) {
            stored += enabled
            this.enabled.value = enabled
        }
    }
}
