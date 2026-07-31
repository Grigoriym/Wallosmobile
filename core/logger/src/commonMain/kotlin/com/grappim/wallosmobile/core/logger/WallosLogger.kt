package com.grappim.wallosmobile.core.logger

import com.grappim.wallosmobile.core.logger.WallosLogger.Companion.install
import com.grappim.wallosmobile.core.logger.WallosLogger.Companion.uninstall
import kotlin.concurrent.Volatile

/**
 * Logger that [logcat] delegates to. Call [install] to set a logger,
 * the default is a no-op logger. Call [uninstall] to revert to no-op.
 */
interface WallosLogger {

    fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String)

    companion object {
        @Volatile
        @PublishedApi
        internal var logger: WallosLogger = NoLog
            private set

        val isInstalled: Boolean
            get() = logger !== NoLog

        fun install(logger: WallosLogger) {
            this.logger = logger
        }

        fun uninstall() {
            logger = NoLog
        }
    }

    private object NoLog : WallosLogger {
        override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
            // no-op
        }
    }
}
