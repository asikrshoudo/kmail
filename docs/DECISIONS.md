# Architecture decisions

## Build
- Gradle 9.6.0, set up by CI (`setup-gradle`). No wrapper is committed yet.
- AGP 9.5.1 with built-in Kotlin. Compose compiler plugin pinned to 2.3.21.
- Hilt 2.60.1 and Room 2.8.5 through KSP 2.3.13.
- compileSdk and targetSdk 37, minSdk 26.
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

## Signing, versioning, releases
- Only release builds exist. Every push to `main` runs `release.yml`: unit tests, signed APK and AAB, and a GitHub pre-release `v<version>-build.<run>` with the APK.
- The signing key lives only in GitHub Secrets (`KMAIL_KEYSTORE_B64`, `KMAIL_KEYSTORE_PASSWORD`, `KMAIL_KEY_ALIAS`, `KMAIL_KEY_PASSWORD`) and is never committed. Back it up outside GitHub.
- Version comes only from `version.properties`. `versionCode = major*10000 + minor*100 + patch`, each part 0..99.
- Single branch (`main`) for now. Test, production and release branches come near the Play Store release.
- Release builds are not minified yet. R8 rules are added during release preparation.

## Account setup (Phase 2)
- Auto-fill uses a built-in preset table. Unknown domains get a marked guess (imap.DOMAIN 993 TLS, smtp.DOMAIN 587 STARTTLS). There is no network autodiscovery, so the email domain is not sent to a third party.
- Presets are not verified against live servers. Gmail, Yahoo, iCloud, Fastmail and AOL need an app password. Outlook.com may need OAuth, which is not supported yet.
- Server forms are validated by `ServerForm` before `ServerEndpoint` is built, because `ServerEndpoint` throws on bad input.
- Passwords are held in ViewModel memory as a String while typing (Compose text fields need String). They are never put in saved state and are cleared when the setup flow ends. Process death means the password must be entered again.
- QR and file import stay disabled until the encrypted transfer design is approved (Phase 5).
- Hilt 1.4.0 provides `hiltViewModel()` in `androidx.hilt:hilt-lifecycle-viewmodel-compose` (package `androidx.hilt.lifecycle.viewmodel.compose`).
- Release signing certificate SHA-256: 1b73f5733631821dafc6aa61fde5f1bedd92eb2061ab0b4d5634601f6720f6a8. The APK is v2-signed only (enough for minSdk 26).
- There is no provider whitelist. Any IMAP/SMTP server the user enters is used as typed. Presets only pre-fill the form.
- TLS certificates are validated against the host name the user entered. A server whose certificate does not match that name fails with a TLS error. A "trust this certificate" option would need a separate approved design.
- Typing a well-known port (465, 993, 995, 587, 143) selects the matching security type automatically.

## Mail provider (Phase 3)
- `AngusMailProvider` opens one connection per operation and loads the password from the credential store each time. No long-lived connection and no IDLE yet.
- `mail.imap.peek=true`, so fetching a message never marks it as read.
- Header fetch leaves the snippet empty. It is filled from the body after the message is downloaded.
- `hasAttachments` is true for `multipart/mixed`. It can be wrong for unusual messages.
- Sent mail is not copied to the Sent folder yet. Most SMTP servers do not do it for IMAP clients.
- Start destination is chosen once at launch from the saved accounts (Home or Welcome).
- Sync runs on Home open and on pull to refresh. Inbox only at first, other folders when opened. Sync is serialized per account and per folder.
- Unified inbox shows messages of every account's INBOX folder, joined on the folder's current UIDVALIDITY.

## Reader (Phase 3)
- jsoup `Safelist` sanitizes HTML. Scripts, forms, frames, event handlers, `javascript:` links and SVG data images are removed. Styles are kept. CSS `url()`, `@import`, `expression()` and fixed positioning are removed.
- Remote images are blocked by default, by the sanitizer, by a CSP meta tag and by `WebView.blockNetworkLoads`. The user can allow them per message.
- WebView has JavaScript off and no file or content access. Link taps open a confirmation dialog that shows the full URL. Only http, https, mailto and tel are offered.
- `MimeParser` never calls `Part.getContent()`. It reads decoded streams and chooses the charset itself, so it does not depend on the activation mailcap registry.
- No verified-sender badge is shown, since no authentication result is checked.

## Compose (Phase 3)
- `MimeBuilder` writes messages by hand (quoted-printable body, base64 attachments, RFC 2231 file names). It does not use `MimeMessage.setContent()`, so it does not depend on the activation mailcap registry. Bcc recipients are only in the SMTP envelope.
- No `X-Mailer` or `User-Agent` header is added.
- After a successful send the message is appended to the Sent folder as a separate best-effort step. Sending is never reported as failed because of it.
- Reply goes to Reply-To, else From. Replying to your own message goes to its original recipients. Reply all removes your own address.
