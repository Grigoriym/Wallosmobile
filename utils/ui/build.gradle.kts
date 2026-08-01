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
        }
    }
}
