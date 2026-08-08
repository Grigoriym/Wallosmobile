plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Upcoming payments is `Subscription` rows filtered and re-sorted (M8 preamble) —
            // duplicating the type here would fork it in two places for no caller.
            implementation(projects.feature.subscriptions.domain)
        }

        commonTest.dependencies {
            // `WallosError` for a fake `DashboardRepository` failure — not a main-source
            // dependency, since nothing here constructs or matches on it directly.
            implementation(projects.core.domain)
        }
    }
}
