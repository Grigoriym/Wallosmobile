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

            // `FakeNetworkMonitor` implements `grappim-kit-storage`'s `NetworkMonitor`, reachable
            // through `core:storage`'s own `api(libs.grappim.kit.storage)` — the same shape
            // TaigaMobileNova's `:testing` uses.
            api(projects.core.storage)

            // `FakeTrustedCertStorage`'s public surface (`pins`, `getAllFlow`) is `PendingCertTrust`
            // — already reachable via `core:storage`'s own `api(libs.grappim.kit.storage)` (which
            // itself re-exports `grappim-kit-domain`), but kept as its own line for the
            // `com.grappim.wallosmobile.core.domain` types other fakes here still need.
            api(projects.core.domain)

            // `FakeCrashReporter` implements this interface, same reasoning as `core:storage` above.
            api(libs.grappim.kit.crash)
        }
    }
}
