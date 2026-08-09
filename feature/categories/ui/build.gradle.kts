plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    // `CategoriesRoute`/`CategoryEditorRoute` are `@Serializable` `NavKey`s — the shell serializes
    // them into the back stack, so this module needs serialization like `feature:subscriptions:ui`.
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.categories.domain)

            implementation(projects.uikit)
            implementation(projects.strings)
        }

        commonTest.dependencies {
            // Test-only, as in `feature:subscriptions:ui`: production code never names a
            // `WallosError`, it hands whatever it caught to `getErrorMessage`.
            implementation(projects.core.domain)
        }
    }
}
