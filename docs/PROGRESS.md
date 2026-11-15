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

Step 3.4 (compose, send, copy to Sent, reply, reply all, forward, attachments): added, verify in CI and on a device.

Known issues (compose):
- Plain text only. No rich text editor yet.
- No contact suggestions and no draft autosave yet. Leaving with changes asks to discard.
- The signature is a fixed text ("Sent using Kmail") that can be edited in the message. Signature settings are not built yet.
- Sent messages are copied to the Sent folder only if the server has one. A failed copy is silent.
- Attachment limit is 15 MB in total. Whole files are held in memory.
- Recipients are plain text fields, not chips.
- Reply and forward need the original message from the server or from the in-memory cache.

Fix: attaching a file in the composer crashed the app (nested vertical scroll). Removed the extra scroll container.

Step 4.1 (archive, delete, mark read or unread, star; multi-select in the list; action bar in the reader): added, verify in CI and on a device.
Reader surface and screen transitions reworked (rounded email page, horizontal slide with predictive back). Visual check on a device not done yet.

Known issues (actions):
- Delete moves to the Trash folder. Messages in Trash are not permanently deleted yet.
- Moved messages appear in the target folder after its next sync.
- No undo.
- Swipe actions, move to a chosen folder and reading-bar customization are not built yet.
- Flag changes made on other devices are not picked up by sync yet.

Step 5.1 (Settings: accounts, remove account, App Lock with PIN, biometrics and auto-lock, About; privacy policy text): added, verify in CI and on a device.

Known issues (settings and lock):
- App Lock has no security-question recovery yet. A forgotten PIN means clearing app data.
- The PIN hash is not bound to the Android Keystore yet.
- The lock screen is a cover inside the app. Android's recent-apps preview is not hidden and screenshots are not blocked.
- Lockout timing uses the device clock.
- Settings has no search field and only three sections. Appearance, Sync, Notifications and other categories are not built.
- The privacy policy URL in strings.xml (settings_privacy_url) must point to a real page. `docs/PRIVACY_POLICY.md` is the text; CONTACT_EMAIL must be replaced.
- The TheKami logo asset has not been supplied. Settings shows the text "TheKami" and the version.

Step 5.1 on a device: App Lock with PIN and fingerprint worked in a manual test.

Steps 6.1 and 6.2 (Appearance: theme, AMOLED, dynamic color; background sync with WorkManager; new-mail notifications): added, verify in CI and on a device.

Known issues (sync and notifications):
- Background sync is periodic (15 minutes at best) and Android may delay it. There is no push or IMAP IDLE.
- Tapping a notification opens the app, not the message.
- Notifications use one channel; no per-account settings, sound or quiet hours yet.
- A new-mail notification is skipped for an inbox that was never synced before.

Steps 6.1 and 6.2 on a device: build installed and ran. Appearance and background sync were not checked in detail.

Step 7.1 (sync reconcile: server-side deletions and flag changes; load older messages): added, verify in CI and on a device.

Known issues (sync):
- Reconcile runs on foreground refresh only, not in background sync, to save battery and connections.
- Reconcile compares only messages that are cached. Older messages are checked when they are loaded.
- Folders other than the inbox are synced only when opened.
- Server-side deletions hide the cached row. Hidden rows are never purged yet.

Step 7.1 on a device: build installed. Reconcile and load-more were not checked in detail.

Step 8.1 (swipe to archive or delete, sort and filter menu): added, verify in CI and on a device.

Known issues (list):
- Swipe actions are fixed (right archives, left deletes). They are not configurable yet.
- Sort and filter apply to the loaded messages only, and they reset when the app restarts.
- Swiping a row near the screen edge may compete with the drawer gesture. Not checked on a device.

Step 8.1 on a device: build installed. Swipe, sort and filter were not checked in detail.

Step 9.1 (draft autosave, unsent drafts list, signature settings): added, verify in CI and on a device.

Known issues (drafts and signatures):
- Drafts live on this device only (encrypted files). They are not saved to the server Drafts folder and do not appear on other clients.
- A draft cannot be restored if the Keystore key is lost (for example after a lock screen reset on some devices).
- Signatures are plain text. The clickable Play Store link needs the rich text composer, which is not built.
- There is no way to turn off the signature for one account only; an empty override uses the default.
- The signature is added when the composer opens. Changing the From account does not change it.

Step 9.1 on a device: build installed. Drafts and signatures were not checked in detail.

Step 9.2 (recipient suggestions from sent addresses, cached senders and optional phone contacts): added, verify in CI and on a device.

Known issues (suggestions):
- Phone contacts need the READ_CONTACTS permission, which is asked once when the To field first gets focus. Without it, the other two sources still work.
- Suggestions are matched by simple substring. No ranking by how recently a contact was used, except that sent addresses come first.
- Sent addresses are remembered only for messages sent from this app version on.
- Play Console: READ_CONTACTS is used on the device only and nothing is uploaded; answer the Data safety form accordingly.

Step 9.2 on a device: build installed. Suggestions were not checked in detail.

Step 10.1 (App Lock recovery questions): added, verify in CI and on a device.

Known issues (recovery):
- Exactly two questions. Answers are compared after lower-casing, trimming and Unicode normalization.
- Questions are stored in plain text on the device so they can be shown on the lock screen. Answers are only stored as PBKDF2 hashes (120000 iterations, own salt each).
- Lockout for wrong answers starts at 3 failures (1 minute, doubling, at most 6 hours) and uses the device clock.
- The answer hashes are not bound to the Android Keystore yet.
- The recovery questions are asked for right after the PIN is set; they can be skipped and set up later in Settings.
- Recovery logic (store) is covered by policy unit tests only. The store itself has no unit tests.

## Next

Translations for the other seven languages, then final release checks.
