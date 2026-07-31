plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.serialization)
    alias(libs.plugins.wallosmobile.kmp.network)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `configureTests()` puts this module on every other module's `commonTest`, so
            // `runTest` reaches them through here rather than being declared per module.
            api(libs.kotlinx.coroutines.test)
        }
    }
}
