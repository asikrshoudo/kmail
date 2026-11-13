# Architecture decisions

## Build
- Gradle 9.6.0, set up by CI (`setup-gradle`). No wrapper is committed yet.
- AGP 9.5.1 with built-in Kotlin. Compose compiler plugin pinned to 2.3.21.
- Hilt 2.60.1 and Room 2.8.5 through KSP 2.3.13.
- compileSdk and targetSdk 37, minSdk 26.
- Debug builds use the `.debug` application id suffix.
- Dependency versions live only in `gradle/libs.versions.toml`.

## Security
- `allowBackup=false` and data extraction rules exclude all domains. Keystore keys do not survive backup restore.
- Cleartext traffic is disabled.
- Credentials never enter Room. They go to a Keystore-backed AES-GCM store in Phase 2.
- Email HTML is untrusted: JavaScript off, sanitize, restrict navigation (Phase 3).

## Data
- Message identity: account, folder, UIDVALIDITY and UID, all Long. Duplicates are ignored by a unique index.
- UIDVALIDITY change deletes stale rows. A moved message gets a new row at the destination. The old row is soft-removed.
- Account id is a random UUID, not the email address.
- Room 2.8.5 is used instead of Room 3 until Room 3 is evaluated.

## UI
- Dynamic color by default, Indigo fallback, 24dp base corner radius.
- System font with letter spacing 0 (Arabic joining, Indic line height).
- Localization: `values-xx` per language, `localeFilters`, `locales_config.xml`, AppCompat for per-app language below Android 13.

## Provider boundary
- `MailProvider` exchanges raw RFC 822 bytes and returns `MailResult`. MIME parsing is a separate layer.

## Open decisions
- Encrypt the whole database (SQLCipher) in Phase 5.
- Commit a Gradle wrapper later.
- Kotlin 2.4.x only after Hilt and KSP are verified against it.

## Signing and versioning
- Debug builds are signed with a public debug keystore (`app/debug.keystore`) so each CI debug APK updates the previous one.
- Release signing key lives only in GitHub Secrets (`KMAIL_KEYSTORE_B64`, `KMAIL_KEYSTORE_PASSWORD`, `KMAIL_KEY_ALIAS`, `KMAIL_KEY_PASSWORD`). It is never committed. Back it up outside GitHub.
- Release builds run only from `release.yml` (manual or `v*` tag), never for pull requests.
- Version comes only from `version.properties`. `versionCode = major*10000 + minor*100 + patch`, each part 0..99.
- Release builds are not minified yet. R8 rules are added during release preparation.
