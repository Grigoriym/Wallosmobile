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
        commonMain.dependencies {
            implementation(projects.feature.subscriptions.domain)

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
