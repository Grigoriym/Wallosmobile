plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
    alias(libs.plugins.wallosmobile.kmp.network)
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.api)
            implementation(projects.core.domain)
            implementation(projects.core.asyncKmp)

            // The cache (3.4): this is the module that talks to the DAOs. The entity types come
            // with them, which is also what makes the mapper's `implementation` line enough.
            implementation(projects.core.storage)

            // `dto` and `domain` arrive through the mapper, which exposes both as `api`.
            implementation(projects.feature.subscriptions.mapper)
        }

        commonTest.dependencies {
            // The mappers are constructed here, not injected, since they are pure classes with no
            // seam to fake (plan §6.1).
            implementation(projects.utils.formatter.datetime)
        }
    }
}
