plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    // `DashboardRoute` is a `@Serializable` `NavKey` — the shell serializes it into the back
    // stack, so this module needs serialization like `feature:subscriptions:ui` does.
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.dashboard.domain)

            // Upcoming payments arrives as `List<Subscription>` (M8 preamble); `dashboard:domain`'s
            // own dependency on this module is `implementation`, not `api`, so it isn't visible
            // here transitively (8.1/8.3's same reminder, now paid a third time).
            implementation(projects.feature.subscriptions.domain)

            // Unlike `subscriptions:ui` (test-only there), this card's own version-gating decision
            // — hide on `WallosError.UnsupportedEndpoint` rather than show an error — is made in
            // the ViewModel, so the type has to be a main-source dependency here.
            implementation(projects.core.domain)

            implementation(projects.uikit)
            implementation(projects.strings)

            implementation(projects.utils.formatter.decimal)
            implementation(projects.utils.formatter.datetime)
        }
    }
}
