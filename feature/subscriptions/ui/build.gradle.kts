plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    // `SubscriptionsRoute` is a `@Serializable` `NavKey` — the shell serializes it into the
    // back stack, so this module does need serialization (`feature:setup:ui` does not).
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        // The image picker (7.9) is the one platform seam this feature needs — everything else
        // reaches the server through `core:api` alone. `rememberLauncherForActivityResult` is
        // this module's first reason to declare `androidMain` at all.
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        commonMain.dependencies {
            implementation(projects.feature.subscriptions.domain)

            // The editor's pickers (7.6) read these three repositories directly — reference data
            // has no cache to hide them behind, unlike `feature.subscriptions.domain` above.
            implementation(projects.feature.categories.domain)
            implementation(projects.feature.household.domain)
            implementation(projects.feature.paymentmethods.domain)

            // `uikit` carries `utils:ui` as `api`, so `NativeText` needs no declaration here.
            implementation(projects.uikit)
            implementation(projects.strings)

            // For `BaseUrlProvider` alone: a logo is a bare filename until the instance root is
            // put in front of it (API doc §4), and that is the one place the root is normalized.
            implementation(projects.core.api)

            implementation(projects.utils.formatter.decimal)
            implementation(projects.utils.formatter.datetime)

            // `coil-network-ktor3` is *not* declared here any more (4.5): this module drew its
            // network layer from that artifact's autodiscovery, and the loader is now built in
            // `:composeApp`, where the trust-aware engine is visible.
            implementation(libs.coil.compose)
        }

        commonTest.dependencies {
            // Test-only, as in `feature:setup:ui`: production code never names a `WallosError`,
            // it hands whatever it caught to `getErrorMessage`.
            implementation(projects.core.domain)
        }
    }
}
