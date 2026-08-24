package com.grappim.wallosmobile.di

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/**
 * Nothing outside `androidApp` needs Play In-App Update, unlike [com.grappim.wallosmobile.core.crashreportingapi.CrashReporter]
 * — no KMP module, both flavor implementations live beside this interface in `androidApp/src/gplay`
 * and `androidApp/src/fdroid` (same `di` package `AndroidModule`'s `@ComponentScan` already covers).
 */
interface AppUpdateChecker {
    val updateState: Flow<UpdateState>

    fun checkAndRequestUpdate(activity: Activity)
    fun checkUpdateStateOnResume()
    fun registerUpdateListener()
    fun unregisterUpdateListener()
    fun completeUpdate()
}

sealed class UpdateState {
    data object UpdateDownloaded : UpdateState()
}
