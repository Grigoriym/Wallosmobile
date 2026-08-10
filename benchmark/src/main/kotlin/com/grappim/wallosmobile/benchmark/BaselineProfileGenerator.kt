package com.grappim.wallosmobile.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

private const val TARGET_PACKAGE = "com.grappim.wallosmobile"
private const val UI_TIMEOUT_MS = 5_000L
private const val SWIPE_STEPS = 150

/**
 * The list/editor journeys below need a logged-in process — cold-start-only 13.1 never did,
 * since `LoginScreen` is a valid cold-start target itself. `BaselineProfileRule` never clears app
 * data between iterations or `@Test` methods (only kills the process), so logging in once, by
 * hand, before running the generator is enough to seed every journey below — see this project's
 * `docs/EMULATOR_TESTING.md` for the concrete on-device recipe.
 */
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun coldStart() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun subscriptionsListFling() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
        openSubscriptionsList()
        repeat(3) {
            device.swipe(540, 2000, 540, 300, SWIPE_STEPS)
        }
    }

    @Test
    fun addSubscriptionEditorOpen() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
        openSubscriptionsList()
        device.findObject(By.desc("Add subscription")).click()
        device.wait(Until.hasObject(By.text("New subscription")), UI_TIMEOUT_MS)
    }
}

/**
 * Dashboard is the app's start destination, so every journey below reaches the subscriptions
 * list the same way a user would: open the drawer, tap "Subscriptions".
 */
private fun MacrobenchmarkScope.openSubscriptionsList() {
    // `startActivityAndWait()` only waits for the window to be idle, not for
    // `WallosAppContent`'s `isConnected` DataStore flow to resolve past its first-frame blank
    // `Box` (plan §7.1) — on a slow cold-JIT frame the Menu icon isn't drawn yet, and skipping
    // this wait made `addSubscriptionEditorOpen` flaky (`findObject` returning null) while
    // `subscriptionsListFling`, calling the same function, happened to land on a fast frame.
    device.wait(Until.hasObject(By.desc("Menu")), UI_TIMEOUT_MS)
    device.findObject(By.desc("Menu")).click()
    device.wait(Until.hasObject(By.text("Subscriptions")), UI_TIMEOUT_MS)
    device.findObject(By.text("Subscriptions")).click()
    device.wait(Until.hasObject(By.desc("Add subscription")), UI_TIMEOUT_MS)
}
