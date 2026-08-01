plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: `NativeText.Resource` exposes `StringResource` in its public signature.
            api(libs.jetbrains.compose.components.resources)

            // `getErrorMessage` is the one place a `WallosError` becomes a user-facing string,
            // so this module needs both the error type and the catalogue of messages.
            implementation(projects.core.domain)
            implementation(projects.strings)
        }
    }
}
