plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: consumers need to resolve `StringResource` themselves.
            api(libs.jetbrains.compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.grappim.wallosmobile.strings.generated.resources"
    generateResClass = always
    publicResClass = true
}
