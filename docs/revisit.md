# Revisit list

Findings worth fixing that are bigger than "small and isolated" — deferred here instead of fixed
inline, per `docs/CHECKLIST.md`'s M17 instructions. Numbered, not dated; each entry stays until
it's actually done, then gets deleted (git has the history).

## 1. No way to revoke a trusted TOFU certificate pin from inside the app

Filed 2026-08-11, MASVS-NETWORK review (17.3).

`TrustedCertStorage` (`core/storage/src/commonMain/kotlin/com/grappim/wallosmobile/core/storage/
cert/TrustedCertStorage.kt`) only exposes `isTrusted`/`trust` — no `untrust`/`clear`, and there is
no Settings screen listing accepted pins (TaigaMobileNova has one,
`feature/settings/ui/.../trustedcerts/TrustedCertificatesScreen.kt`; WallosMobile has no
`TrustedCert*` file under `feature/`). A user who accepts a certificate by mistake, or wants to
drop a pin for a decommissioned instance, has no in-app path to do it short of clearing all app
data (which also drops the stored API key, server URL and every preference).

Not a live security hole: the pin is per-`(host, sha256Fingerprint)`
(`TrustedCertStorageImpl.entry`), so a legitimate cert rotation on an already-trusted host doesn't
silently keep trusting the old fingerprint — it just fails the match and re-triggers TOFU, prompting
the user again. This is a hygiene/UX gap (no way to *proactively* clean up), not an escalation path.

Fix shape: a `TrustedCertStorage.untrust(host, fingerprint)` (or `untrustAll(host)`) plus a
Settings-screen list, same shape as Taiga's. Not small enough to fold into 17.3 itself.
