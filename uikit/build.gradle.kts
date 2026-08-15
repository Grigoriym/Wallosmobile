plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Required by the generated `Res` class itself, and `api` so that consumers of
            // `RDrawable` can resolve `DrawableResource`.
            api(libs.jetbrains.compose.components.resources)

            // `api`: `TopBarConfig` exposes `NativeText` in its public signature.
            api(projects.utils.ui)

            implementation(projects.strings)
        }
    }
}

// `publicResClass` is what makes the generated `Res` — and so the `RDrawable` alias — visible
// to the modules that depend on this one.
compose.resources {
    packageOfResClass = "com.grappim.wallosmobile.uikit.generated.resources"
    generateResClass = always
    publicResClass = true
}
