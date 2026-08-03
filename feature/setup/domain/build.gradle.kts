plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: `SetupRepository.trustCertificate` takes a `PendingCertTrust`, so every
            // consumer of this module needs the type — the `ui` module included.
            api(projects.core.domain)
        }
    }
}
