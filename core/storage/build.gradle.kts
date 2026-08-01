plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.datastore.android)
        }
        commonMain.dependencies {
            implementation(libs.androidx.datastore.core)
        }
    }
}
