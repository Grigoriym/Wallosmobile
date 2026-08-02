plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Both `api`: they are the two ends of every mapper's public signature, so a consumer
            // that can call one can name the other.
            api(projects.feature.subscriptions.dto)
            api(projects.feature.subscriptions.domain)

            // The cache's entities are the third end of a mapper signature (3.4): the wire, the
            // model and the row. `implementation`, not `api` — `core:storage` also holds the key
            // and URL seams, and only the module that talks to the DAOs should see those.
            implementation(projects.core.storage)

            // `DateFormatter` — the wire's `YYYY-MM-DD`, and its blank-means-absent handling.
            implementation(projects.utils.formatter.datetime)
        }
    }
}
