plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
    alias(libs.plugins.wallosmobile.kmp.network)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.api)
            implementation(projects.core.domain)
            implementation(projects.core.storage)
            implementation(projects.core.asyncKmp)
            implementation(projects.feature.setup.domain)
            implementation(projects.feature.setup.dto)
        }
    }
}
