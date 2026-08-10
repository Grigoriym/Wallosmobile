package com.grappim.wallosmobile.core.crashreportingapi

/**
 * `feature:settings:ui` (a lower Gradle module) cannot depend upward on `androidApp`, so this
 * interface lives here — the two flavor implementations live in `androidApp/src/gplay` and
 * `androidApp/src/fdroid`, mirroring `core:appinfo-api`'s shape.
 */
interface CrashReporter {

    val isAvailable: Boolean

    fun setCollectionEnabled(enabled: Boolean)

    fun recordException(throwable: Throwable)

    fun log(message: String)
}
