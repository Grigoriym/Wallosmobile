plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
    alias(libs.plugins.wallosmobile.kmp.network)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.api)
            implementation(projects.core.domain)
            implementation(projects.core.asyncKmp)

            // `dto` and `domain` arrive through the mapper, which exposes both as `api`.
            implementation(projects.feature.subscriptions.mapper)
        }

        commonTest.dependencies {
            // Test-only: production code never names `ApiKeyStorage`, `WallosApiClient` owns it.
            // The api test has to fake one to build a client over `MockEngine`.
            implementation(projects.core.storage)

            // Likewise — the mappers are constructed here, not injected, since they are pure
            // classes with no seam to fake (plan §6.1).
            implementation(projects.utils.formatter.datetime)
        }
    }
}
