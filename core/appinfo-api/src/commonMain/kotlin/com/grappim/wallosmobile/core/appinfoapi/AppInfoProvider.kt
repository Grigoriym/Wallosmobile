package com.grappim.wallosmobile.core.appinfoapi

/**
 * Build-time facts the shared code can't read for itself. The implementation is platform glue
 * and arrives with the Koin startup wiring in `androidApp`.
 */
interface AppInfoProvider {
    fun isDebug(): Boolean

    fun versionName(): String

    fun versionCode(): Int
}
