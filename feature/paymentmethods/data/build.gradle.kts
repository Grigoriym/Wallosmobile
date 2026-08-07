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
            implementation(projects.core.asyncKmp)

            // `WallosCrudApi`, `CrudEndpoint` — the shared four-call implementation this module
            // composes by delegation rather than reimplementing (7.1).
            implementation(projects.core.crud)

            // `dto` and `domain` arrive through the mapper, which exposes both as `api`.
            implementation(projects.feature.paymentmethods.mapper)
        }

        commonTest.dependencies {
            // `ApiKeyStorage` for the fake `WallosApiClient` needs — core:crud's own test does
            // the same, since `core:api` only exposes it as `implementation`.
            implementation(projects.core.storage)

            // `HtmlUnescaper`, to construct a real `PaymentMethodMapper` — `feature:paymentmethods:
            // mapper` only takes it as `implementation`, so the repository test needs its own line.
            implementation(projects.feature.subscriptions.mapper)
        }
    }
}
