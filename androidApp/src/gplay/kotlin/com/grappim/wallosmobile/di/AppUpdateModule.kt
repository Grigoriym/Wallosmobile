package com.grappim.wallosmobile.di

import android.content.Context
import com.grappim.kit.appupdate.AppUpdateChecker
import com.grappim.kit.appupdate.AppUpdateCheckerImpl
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * `grappim-kit-appupdate-gplay`'s `AppUpdateCheckerImpl` carries no `@Single` of its own (same
 * convention as `grappim-kit-storage`/`grappim-kit-trustmanager`), so the binding is explicit here.
 */
@Module
@Configuration
class AppUpdateModule {
    @Single
    fun provideAppUpdateChecker(context: Context): AppUpdateChecker = AppUpdateCheckerImpl(context)
}
