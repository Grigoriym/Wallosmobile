plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    // No `kmp.serialization`: onboarding has no route. The startup branch is state, not
    // navigation, so login never enters a back stack and needs nothing serialized.
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Carries `core:domain` as `api` — `PendingCertTrust` is in `SetupRepository`'s
            // signature and in this screen's state, so it needs no declaration here either.
            implementation(projects.feature.setup.domain)

            // `uikit` carries `utils:ui` as `api`, so `NativeText` needs no declaration here.
            implementation(projects.uikit)
            implementation(projects.strings)
        }
    }
}
