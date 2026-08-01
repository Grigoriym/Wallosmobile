package com.grappim.wallosmobile.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * `viewModelScope` dispatches on `Dispatchers.Main`, which does not exist in a host test — a
 * ViewModel test without this fails on the first `launch`. Not a JUnit `@Rule`: `commonTest` is
 * `kotlin.test`, so the test class calls [setup]/[tearDown] from `@BeforeTest`/`@AfterTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) {

    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    fun tearDown() {
        Dispatchers.resetMain()
    }
}
