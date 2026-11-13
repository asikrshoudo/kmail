# Kmail

Kmail is an independent, privacy-focused Android email client by TheKami.
It connects to your existing mail accounts over IMAP and SMTP. It is not a hosting service.

- Kotlin, Jetpack Compose, Material 3
- Local-first: mail and credentials stay on the device
- Package: `com.kmail.thekami`

## Status

Early development (Phase 1, foundation). Not usable as a mail client yet.
See [docs/PROGRESS.md](docs/PROGRESS.md) for what exists and what is untested.

## Build

Every push to `main` builds a signed release APK in GitHub Actions and publishes it as a GitHub pre-release.
No signing keys or secrets are stored in this repository.

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).
