plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
    alias(libs.plugins.wallosmobile.kmp.network)
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.storage)
            implementation(libs.grappim.kit.appinfo)

            // `api`: `Json` is in `WallosEnvelopeParser`'s and the `NetworkModule` providers' public
            // signatures, and every feature's `data` module injects a qualified one.
            api(libs.kotlinx.serialization.json)

            // Not in `kmp.network`: this is the only module that installs the plugin, and it has
            // to, because the API key travels in the body where Ktor's own sanitizer can't reach.
            implementation(libs.ktor.logging)
        }
        androidMain.dependencies {
            // CompositeTrustManager is androidMain-only in grappim-kit too (X509ExtendedTrustManager
            // doesn't exist off Android/JVM); nothing outside this module constructs it, so
            // `implementation` is enough.
            implementation(libs.grappim.kit.trustmanager)
        }
    }
}
