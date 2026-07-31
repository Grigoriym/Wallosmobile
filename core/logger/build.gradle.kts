plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.timber)
        }
    }
}
