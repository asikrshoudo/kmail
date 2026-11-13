# Kmail progress

"Passed" means the GitHub Actions build succeeded. It does not mean a feature was tested on a device.

## Phase 1: Foundation

| Step | Content | Status |
|------|---------|--------|
| 1.1 | Gradle skeleton, debug APK CI | Passed |
| 1.2 | Hilt via KSP | Passed |
| 1.3 | Theme: light, dark, AMOLED, dynamic color | Passed. On-device visual check not confirmed |
| 1.4 | Localization, 8 locales, per-app language | Passed. On-device glyph and Arabic RTL check not confirmed |
| 1.5 | Navigation skeleton | Passed |
| 1.6 | Loading, empty, error components | Passed. Not used by any screen yet |
| 1.7 | Room schema (accounts, folders, messages) | Passed. Database is not opened at runtime yet |
| 1.8 | MailProvider interface and models | Added, verify in CI |
| 1.9 | Docs and unit tests | Added, verify in CI |

## Known issues and open items

- The effective Kotlin version used by AGP 9.5.1 was not confirmed from build logs.
- `schemas/1.json` is not committed. Commit it before the first release.
- Pre-release database stays at version 1. After a schema change, uninstall the app before installing a new build.
- Theme mode, AMOLED and dynamic color are not wired to Settings yet.
- Per-app language below Android 13 cannot be tested until the Settings screen exists.
- Color contrast ratios are not measured yet.
- The cache identity (UIDVALIDITY + UID) is IMAP-shaped. A Gmail API or Graph provider will need a remote-id column and a migration.
- No INTERNET permission yet. It is added in Phase 2.
- No release signing and no release build yet.

## Next

Phase 2: account setup, IMAP and SMTP connection, Keystore-backed credential storage.
