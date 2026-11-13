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
| 1.8 | MailProvider interface and models | Passed |
| 1.9 | Docs and unit tests | Passed |

## Known issues and open items

- The effective Kotlin version used by AGP 9.5.1 was not confirmed from build logs.
- `schemas/1.json` is not committed. Commit it before the first release.
- Pre-release database stays at version 1. After a schema change, uninstall the app before installing a new build.
- Theme mode, AMOLED and dynamic color are not wired to Settings yet.
- Per-app language below Android 13 cannot be tested until the Settings screen exists.
- Color contrast ratios are not measured yet.
- The cache identity (UIDVALIDITY + UID) is IMAP-shaped. A Gmail API or Graph provider will need a remote-id column and a migration.
- No release signing and no release build yet.

## Phase 2: Account connection

| Step | Content | Status |
|------|---------|--------|
| 2.1 | Keystore-backed credential store (AES-GCM, AAD-bound) | Passed in CI. Not tested on a device |
| 2.2 | Server settings model, account repository | Passed in CI. Not tested on a device |
| 2.3 | Angus Mail IMAP and SMTP connection tester, strict TLS | Passed in CI. Never run against a real server |
| 2.4 | Deps (lifecycle, hilt viewmodel, coroutines), provider presets, server form validation | Passed in CI with 11 new unit tests. Preset values not verified against live servers |
| 2.5 | Welcome and credentials screens | Passed. Credentials screen and help dialog seen on a device (dark theme). Welcome screen and error states not confirmed |
| 2.6 | Incoming and outgoing server screens, auto-fill | In progress |
| 2.7 | Connecting and connected screens, connection test, account saving | In progress |

Release APK signature checked in CI: v2 scheme, CN=Kmail O=TheKami, RSA 4096.

## Known issues (Phase 2)

- New strings exist in English only. `MissingTranslation` lint is ignored in `values/strings.xml` until step 2.9.
- Start destination is Welcome until step 2.8.
- Advanced server settings (spec screen 3) are not built yet.
- Display name is the email address and the account color is automatic until Settings exist.
- Going back to Credentials and pressing Continue again resets the server forms.

## Next

Step 2.8: start destination logic and account list on Home. Then Phase 3: IMAP sync, reading, sending.
