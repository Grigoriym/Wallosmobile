package com.grappim.wallosmobile.testing

import com.grappim.wallosmobile.core.crashreportingapi.CrashReporter

class FakeCrashReporter : CrashReporter {
    override var isAvailable: Boolean = false

    val setCollectionEnabledCalls = mutableListOf<Boolean>()
    val recordExceptionCalls = mutableListOf<Throwable>()
    val logCalls = mutableListOf<String>()

    override fun setCollectionEnabled(enabled: Boolean) {
        setCollectionEnabledCalls.add(enabled)
    }

    override fun recordException(throwable: Throwable) {
        recordExceptionCalls.add(throwable)
    }

    override fun log(message: String) {
        logCalls.add(message)
    }
}
