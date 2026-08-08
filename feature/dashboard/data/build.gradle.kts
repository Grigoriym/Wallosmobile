plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
    alias(libs.plugins.wallosmobile.kmp.network)
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.api)
            implementation(projects.core.domain)
            implementation(projects.core.asyncKmp)
            implementation(projects.utils.formatter.decimal)
            implementation(projects.utils.formatter.datetime)

            implementation(projects.feature.dashboard.domain)
            implementation(projects.feature.dashboard.dto)
        }

        commonTest.dependencies {
            // `ApiKeyStorage` for the fake `WallosApiClient` needs — core:api only exposes
            // `core:storage` as `implementation`, so the API test needs its own line.
            implementation(projects.core.storage)
        }
    }
}
