plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Both `api`: they are the two ends of the mapper's public signature.
            api(projects.feature.paymentmethods.dto)
            api(projects.feature.paymentmethods.domain)

            // `HtmlUnescaper` — payment method names go through the same `htmlspecialchars` as
            // every other server-rendered string, and CLAUDE.md is explicit that it gets reused
            // rather than reimplemented per feature.
            implementation(projects.feature.subscriptions.mapper)
        }
    }
}
