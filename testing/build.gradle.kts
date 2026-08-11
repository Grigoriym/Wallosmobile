plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.serialization)
    alias(libs.plugins.wallosmobile.kmp.network)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `configureTests()` puts this module on every other module's `commonTest`, so
            // `runTest` reaches them through here rather than being declared per module.
            api(libs.kotlinx.coroutines.test)

            // Ktor's MockEngine, likewise: every module that talks HTTP needs it in `commonTest`
            // and none of them need it at runtime.
            api(libs.ktor.client.mock)

            // `FakeNetworkMonitor` implements a `core:storage` interface, so consumers resolve it
            // through here — the same shape TaigaMobileNova's `:testing` uses.
            api(projects.core.storage)

            // `FakeTrustedCertStorage`'s public surface (`pins`, `getAllFlow`) is `PendingCertTrust`
            // — `core:storage`'s own dependency on this is `implementation`, not transitive.
            api(projects.core.domain)

            // `FakeCrashReporter` implements this interface, same reasoning as `core:storage` above.
            api(projects.core.crashreportingApi)
        }
    }
}
