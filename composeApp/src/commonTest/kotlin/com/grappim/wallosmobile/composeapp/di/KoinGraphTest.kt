package com.grappim.wallosmobile.composeapp.di

import android.content.Context
import com.grappim.wallosmobile.core.appinfoapi.AppInfoProvider
import com.grappim.wallosmobile.core.crashreportingapi.CrashReporter
import io.ktor.client.engine.HttpClientEngine
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * A missing Koin definition is otherwise a launch-time crash that no gate catches: a
 * `@ComponentScan` that misses a class, or an `AppModule` that forgets an `includes` line, both
 * compile perfectly.
 *
 * `verify()` walks every definition's constructor by reflection and fails on a parameter type the
 * module set can't supply — without instantiating anything, which matters because half this graph
 * wants a DataStore file and an HTTP engine.
 */
class KoinGraphTest {

    @Test
    fun `every definition in the app graph can be resolved`() {
        AppModule().module().verify(extraTypes = EXTERNALLY_SUPPLIED)
    }

    private companion object {
        /**
         * Types nothing in `AppModule` defines, on purpose:
         * - `Context` comes from `androidContext()` in `WallosApp`.
         * - `AppInfoProvider`'s only implementation needs `BuildConfig`, so it lives in
         *   `:androidApp` — which sits above this module and cannot be included from here.
         * - `CrashReporter` is the same shape as `AppInfoProvider` (16.4): both flavor impls live
         *   in `androidApp/src/<flavor>/kotlin/.../di/CrashReporterImpl.kt`, invisible from here
         *   for the same reason. `AboutViewModel`/`InterfaceViewModel` are real runtime consumers,
         *   verified only by installing the app, not by this test — same caveat 16.3 already
         *   documented for `WallosApp.kt`'s own use of it.
         * - `HttpClientEngine` is a false positive rather than a real gap. `verify()` reads a
         *   definition through its *bound type's* constructor, so for the `@Single fun
         *   provideHttpClient(…)` factories it inspects `HttpClient(engine)` instead of the
         *   function's own parameters. The engine comes from `createPlatformHttpClientEngine`
         *   (3.7), which the factory calls itself, so Koin is never asked for one.
         */
        val EXTERNALLY_SUPPLIED = listOf(
            Context::class,
            AppInfoProvider::class,
            CrashReporter::class,
            HttpClientEngine::class
        )
    }
}
