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
| 2.6 | Incoming and outgoing server screens, auto-fill | Passed. Seen on a device with one real account (Hostinger) |
| 2.7 | Connecting and connected screens, connection test, account saving | Passed. IMAP and SMTP test and account save worked on a device with one real account (Hostinger) |

Release APK signature checked in CI: v2 scheme, CN=Kmail O=TheKami, RSA 4096.

## Known issues (Phase 2)

- New strings exist in English only. `MissingTranslation` lint is ignored in `values/strings.xml` until step 2.9.
- Start destination is Welcome until step 2.8.
- Advanced server settings (spec screen 3) are not built yet.
- Display name is the email address and the account color is automatic until Settings exist.
- Going back to Credentials and pressing Continue again resets the server forms.

## Phase 3: Core email

| Step | Content | Status |
|------|---------|--------|
| 3.1 | IMAP and SMTP provider (folders, headers, raw message, flags, move, send) | Added, verify in CI. Never run against a real mailbox |

Steps 2.8 and 3.2 (start destination, sync engine, inbox list, sidebar): added, verify in CI and on a device.

Known issues (Phase 3):
- Only the newest 50 messages of a folder are loaded. There is no "load more" yet.
- Messages deleted or changed on the server are not detected yet. Only new messages are added.
- Message preview is empty until bodies are fetched (step 3.3).
- Tapping a message does nothing yet. There is no compose button yet.
- Search filters the loaded list only. It does not search the server.

Step 3.3 (message reader: MIME parser, HTML sanitizer, WebView, attachments): added, verify in CI and on a device.

Known issues (reader):
- Opening a message needs the network. Bodies are cached in memory only.
- Reply, forward, star, archive and delete are not in the reader yet.
- Quoted replies are not collapsed.
- The email page is always white, also in dark theme.
- Attachments are held in memory and saved through the system file picker. Large attachments are not streamed.
- The list preview is filled only for messages that were opened.
- HTML sanitizer and MIME parser are tested with unit tests only, not with a wide set of real emails.

## Next

Step 3.4: compose, send, save to Sent, reply and forward.
