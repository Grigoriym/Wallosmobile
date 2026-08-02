plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.compose)
    alias(libs.plugins.wallosmobile.kmp.di)
    // `SettingsRoute` is a `@Serializable` `NavKey` — a drawer destination, so the shell
    // serializes it into the back stack.
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // There is no `feature:settings:domain` yet: disconnect is a single call on a single
            // storage seam, and CLAUDE.md's rule is that a single call goes straight from the
            // ViewModel. The data/domain layers arrive with the server display settings (plan §8,
            // Phase 5).
            implementation(projects.core.storage)
            implementation(projects.core.domain)

            // `uikit` carries `utils:ui` as `api`, so `NativeText` needs no declaration here.
            implementation(projects.uikit)
            implementation(projects.strings)
        }
    }
}
