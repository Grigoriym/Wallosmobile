package com.grappim.wallosmobile.di

import com.grappim.kit.appinfo.AppInfoProvider
import com.grappim.wallosmobile.BuildConfig
import org.koin.core.annotation.Single

/**
 * `BuildConfig` only exists in the application module, which is why the interface
 * (`grappim-kit-appinfo`) and its one implementation live apart. It gates the Ktor log level
 * (plan §4.1) and, since 4.4, feeds the About screen — which takes the raw fields rather than a
 * rendered string, so the formatting stays in the `ui` module where a resource can carry it.
 */
@Single(binds = [AppInfoProvider::class])
class AppInfoProviderImpl : AppInfoProvider {
    override fun isDebug(): Boolean = BuildConfig.DEBUG

    override fun isFdroidBuild(): Boolean = BuildConfig.FLAVOR == "fdroid"

    override fun versionName(): String = BuildConfig.VERSION_NAME

    override fun versionCode(): Int = BuildConfig.VERSION_CODE

    override fun buildType(): String = BuildConfig.BUILD_TYPE
}
