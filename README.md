# Vesper Mobile

Personal Vesper companion and native Mortis Operator client for the operator’s Samsung phone.

This repository is **not** PC Vesper and **not** a Mortis backend. Cloudflare `mortis-relay` remains the authority. The Ed25519 signing key never enters this app.

```
Open Vesper
→ Ask what’s happening
→ See Mortis attention
→ Inbox → proposal → approve / edit / reject / hide
→ prepare / tease / schedule / seal
→ inspect release → export unsigned
→ (PC signs)
→ import signed → verify → step-up → publish
```

## Status

| Surface | State |
|---|---|
| Android app (`com.vesper.mobile`) | Implemented — debug APK via Actions |
| Operator Room client | Live against `mortis-relay` |
| Local PC Vesper core | Unavailable by design until the PC runtime exists |
| Player binaries | `NO_BINARY_RELEASE` |
| Drive native Worker poll | Honest `NOT_IMPLEMENTED` until `GOOGLE_SA_JSON` is bound |

## Install

1. Wait for the **CI** workflow on `main` and download `app-debug.apk`.
2. Sideload on the Samsung device.
3. Settings → Mortis host (default is the live Worker) → operator-room path segment.
4. Operator tab → unlock with identity + passphrase.

Do not put the admin path, passphrase, or signing key in this repo.

## Docs

- [ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [SECURITY.md](docs/SECURITY.md)
- [API.md](docs/API.md)
- [BUILD.md](docs/BUILD.md)
- [VESPER_INTEGRATION.md](docs/VESPER_INTEGRATION.md)
- [OPERATOR.md](docs/OPERATOR.md)
- [ANDROID.md](docs/ANDROID.md)
- [NOTIFICATIONS.md](docs/NOTIFICATIONS.md)
- [VOICE.md](docs/VOICE.md)
- [KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md)

## Distinctions

Staging ≠ canon. Approve ≠ publish. Tease ≠ reveal. Trigger ≠ reveal. Seal ≠ publication. Dataset ≠ binary. DM ≠ player client.
