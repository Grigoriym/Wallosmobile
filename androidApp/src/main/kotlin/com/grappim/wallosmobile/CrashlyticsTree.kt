package com.grappim.wallosmobile

import android.util.Log
import com.grappim.wallosmobile.core.crashreportingapi.CrashReporter
import timber.log.Timber

/** A no-op on fdroid via [CrashReporter]'s own implementation, so nothing to gate here. */
class CrashlyticsTree(private val crashReporter: CrashReporter) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority != Log.ERROR || t == null) return

        crashReporter.log(message)
        crashReporter.recordException(t)
    }
}
