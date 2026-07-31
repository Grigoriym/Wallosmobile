plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
    alias(libs.plugins.wallosmobile.kmp.network)
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.storage)
            implementation(projects.core.appinfoApi)

            // Not in `kmp.network`: this is the only module that installs the plugin, and it has
            // to, because the API key travels in the body where Ktor's own sanitizer can't reach.
            implementation(libs.ktor.logging)
        }
    }
}
