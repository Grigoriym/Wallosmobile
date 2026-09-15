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

            // `FakeCrashReporter`/`FakeNetworkMonitor`/`FakeTrustedCertStorage`/`MainDispatcherRule`
            // come from here now — `grappim-kit-testing` re-exports `grappim-kit-crash`/
            // `grappim-kit-storage` (and, through it, `grappim-kit-domain`) as `api`, so
            // `CrashReporter`/`NetworkMonitor`/`TrustedCertStorage`/`PendingCertTrust` stay reachable
            // through this one line the same way they were through the three project/library lines
            // this replaced.
            api(libs.grappim.kit.testing)
        }
    }
}
