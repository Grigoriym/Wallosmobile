package com.grappim.wallosmobile.di

import com.grappim.wallosmobile.core.crashreportingapi.CrashReporter
import org.koin.core.annotation.Single

@Single(binds = [CrashReporter::class])
class CrashReporterImpl : CrashReporter {
    override val isAvailable: Boolean = false

    override fun setCollectionEnabled(enabled: Boolean) = Unit

    override fun recordException(throwable: Throwable) = Unit

    override fun log(message: String) = Unit
}
