plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.setup.domain)

            // `uikit` carries `utils:ui` as `api`, so `NativeText` needs no declaration here.
            implementation(projects.uikit)
            implementation(projects.strings)
        }

        commonTest.dependencies {
            // Test-only: production code never names a `WallosError`, it hands whatever it caught
            // to `getErrorMessage`. The tests have to construct them to pin the attribution down.
            implementation(projects.core.domain)
        }
    }
}
