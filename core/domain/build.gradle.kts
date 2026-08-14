plugins {
    alias(libs.plugins.wallosmobile.kmp.library)
    alias(libs.plugins.wallosmobile.kmp.library.stability)
    // `PendingCertTrust` is `@Serializable` — `TrustedCertStorageImpl` (core:storage) JSON-encodes
    // it for the DataStore-backed pin list (18.1).
    alias(libs.plugins.wallosmobile.kmp.serialization)
}
