# Android

Package `com.vesper.mobile`. Min SDK 26. Target / compile 35. Compose + Navigation.

Optimized for modern Samsung: edge-to-edge, 48dp targets, IBM-like label tracking, dark parchment/steel/crimson. No neon, no fake sci-fi chrome.

## Storage

| Data | Where |
|---|---|
| Operator session | EncryptedSharedPreferences when Keystore works; private `MODE_PRIVATE` fallback otherwise. Invalid state logs the operator out. |
| Host / path / prefs | DataStore |
| Chat history | app files (`vesper_chat.json`) |
| Passphrase | never written |
| Signing key | never present |

## Notifications

Channels: Intake, Review, Release, Signing, System. Health poll is opt-in (15 min WorkManager). No spam.

## Voice

UI states exist. Speech-to-text is **not** bound. Do not treat Speak/PTT as a live engine.

## Play / sideload

Install the debug APK from Actions. Release Play signing is a remaining human step on the authorized PC (`docs/BUILD.md`).
