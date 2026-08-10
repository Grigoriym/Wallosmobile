package com.grappim.wallosmobile.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

private const val TARGET_PACKAGE = "com.grappim.wallosmobile"

class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun coldStart() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
    }
}
