plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    // `ProfileRoute` is a `@Serializable` `NavKey` — the shell serializes it into the back stack,
    // same as `feature:settings:ui`'s `InterfaceRoute`/`AboutRoute`.
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.profile.domain)

            implementation(projects.uikit)
            implementation(projects.strings)
        }

        commonTest.dependencies {
            // Test-only: production code never names a `WallosError`, it hands whatever it caught
            // to `getErrorMessage`.
            implementation(projects.core.domain)
        }
    }
}
