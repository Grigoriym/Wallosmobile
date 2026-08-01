package com.grappim.wallosmobile.di

import com.grappim.wallosmobile.BuildConfig
import com.grappim.wallosmobile.core.appinfoapi.AppInfoProvider
import org.koin.core.annotation.Single

/**
 * `BuildConfig` only exists in the application module, which is why the interface (`core:appinfo-api`)
 * and its one implementation live apart. Today it gates the Ktor log level (plan §4.1).
 */
@Single(binds = [AppInfoProvider::class])
class AppInfoProviderImpl : AppInfoProvider {
    override fun isDebug(): Boolean = BuildConfig.DEBUG
}
