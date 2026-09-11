plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.stability)
    // `PendingCertTrust` is `@Serializable` — `TrustedCertStorageImpl` (core:storage) JSON-encodes
    // it for the DataStore-backed pin list (18.1).
    alias(libs.plugins.wallosmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `PendingCertTrust`/`UntrustedCertificateException`/`resultOf`/`mapResult` live in
            // the kit now; `api` so every module reaching them through `core:domain` keeps
            // compiling without its own direct dependency on the kit.
            api(libs.grappim.kit.domain)
        }
    }
}
