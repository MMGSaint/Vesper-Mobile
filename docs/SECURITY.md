# Security

## Never in the APK, repo, Cloudflare, D1, R2, or browser storage

- Ed25519 private signing key
- `ADMIN_SECRET`
- `RELEASE_TOKEN`
- Operator passphrase (except in-flight to `/api/session/unlock` or `/api/session/stepup`)
- Session tokens in logs

`ADMIN_PATH_SEG` is a capability URL. The operator types it in Settings. It is not a source constant.

## Session

- Mortis issues a short-lived session (idle 15 minutes, absolute 60 minutes).
- Android stores the token in EncryptedSharedPreferences. Backup is disabled.
- Web companion keeps the token in memory only.
- Privileged mutations require a one-time `x-mortis-confirm` nonce from step-up.
- Unlock, Settings, and Release screens set `FLAG_SECURE` so passphrase, admin path, and signed payloads are not captured in screenshots.

## Transport

- TLS only (`usesCleartextTraffic=false`)
- Admin APIs are same-origin on the Worker; the Android client is the operator device
- Operator HTTP client does not follow redirects (prevents bearer leakage)
- Redirects on operator calls are rejected by the web proxy

## Logging

Log keys matching passphrase / token / secret / bearer / ed25519 / confirm are redacted.

## Signing handoff

```
EXPORT UNSIGNED
→ SIGN ON AUTHORIZED PC
→ RETURN SIGNED ARTIFACT
→ IMPORT
→ VERIFY
→ PUBLISH
```

Import accepts a **signature / signed JSON**, never a private key. Publish requires typing `PUBLISH` plus step-up.

## Permissions

| Permission | Why |
|---|---|
| INTERNET | Mortis + optional remote Vesper |
| ACCESS_NETWORK_STATE | Honest offline banners |
| POST_NOTIFICATIONS | Operator attention channels |

Microphone is **not** requested until a real STT engine is bound.

## Player / DM

Windows and Android player applications are `NO_BINARY_RELEASE`. DM is not a player client. Dataset pointer changes do not bump binary versions.
