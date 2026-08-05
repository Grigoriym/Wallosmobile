package com.grappim.wallosmobile.feature.settings.ui.about

import com.grappim.wallosmobile.core.appinfoapi.AppInfoProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AboutViewModelTest {

    /*
     * No `MainDispatcherRule` here, unlike every other ViewModel test: this one never touches
     * `viewModelScope`, because the values are read straight in the constructor.
     */
    @Test
    fun `the build facts reach the state unchanged`() {
        val viewModel = AboutViewModel(FakeAppInfoProvider(name = "1.2.3", code = 42, debug = true))

        val uiState = viewModel.uiState.value
        assertEquals("1.2.3", uiState.versionName)
        assertEquals(42, uiState.versionCode)
    }

    /*
     * The build type is what the screen renders as Debug or Release, so it must be the provider's
     * answer rather than the state class's default — which is `false` and would look right by
     * accident on a release build.
     */
    @Test
    fun `a release build is reported as one`() {
        val viewModel = AboutViewModel(FakeAppInfoProvider(name = "1.2.3", code = 42, debug = false))

        assertFalse(viewModel.uiState.value.isDebug)
    }

    // Private to this file, as in `SettingsViewModelTest` and `InterfaceViewModelTest`: `:testing`
    // is for doubles more than one module needs.
    private class FakeAppInfoProvider(private val name: String, private val code: Int, private val debug: Boolean) :
        AppInfoProvider {

        // The fields are not named after their methods: a `val isDebug` would compile to an
        // `isDebug()` getter and clash with the override (CLAUDE.md).
        override fun isDebug(): Boolean = debug

        override fun versionName(): String = name

        override fun versionCode(): Int = code
    }
}
