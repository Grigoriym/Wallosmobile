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

            // `DateFormatter` — the wire's `YYYY-MM-DD`, and its blank-means-absent handling.
            implementation(projects.utils.formatter.datetime)
        }
    }
}
