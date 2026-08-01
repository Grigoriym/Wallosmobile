plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    // `@Serializable` routes, and the polymorphic `SerializersModule` in `NavKeySerializers.kt`.
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation)
            implementation(projects.uikit)
            implementation(projects.strings)
        }
    }
}
