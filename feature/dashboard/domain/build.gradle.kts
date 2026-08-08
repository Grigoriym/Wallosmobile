plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Upcoming payments is `Subscription` rows filtered and re-sorted (M8 preamble) —
            // duplicating the type here would fork it in two places for no caller.
            implementation(projects.feature.subscriptions.domain)
        }
    }
}
