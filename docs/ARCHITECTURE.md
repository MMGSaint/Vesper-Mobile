# Architecture

Vesper Mobile is a **client**. It is not Mortis. It is not the local-PC Vesper core.

```
                         VESPER (phone)
                           │
             ┌────────────┐             ┌────────────┐
             │                           │
        PERSONAL AI                  OPERATOR
             │                           │
        conversation                MORTIS CONTROL
             │                           │
     LocalVesper UNAVAILABLE        Cloudflare APIs
     RemoteProvider (optional)           │
     FutureProvider reserved             │
                         ┌──────────────┼───────────────┐
                         │               │               │
                       INBOX          RELEASE        APPLICATIONS
                         │               │               │
                      STAGING        CHANNELS       UPDATE STATE
```

Later:

```
                    LOCAL PC VESPER
                           │
                           ▼
                    VESPER CORE
                           │
                 ┌────────┴─────────┐
                 │                   │
             PHONE APP           PC APP
```

## Layers

| Layer | Authority | Lives |
|---|---|---|
| Personal conversation | Remote inference if bound; otherwise honest unavailable | Phone UI |
| Local Vesper core | Not connected | Future PC host |
| Operator session | Mortis Worker | Memory / encrypted session store |
| Canon / staging / seal | Mortis D1 ledger | Cloudflare |
| Dataset publish | Mortis after verified signature | Cloudflare R2 + D1 |
| Binary signing | Authorized PC only | Offline Ed25519 key |
| Player clients | Scaffold | No binary release |

## Android (`com.vesper.mobile`)

Kotlin + Jetpack Compose. Direct HTTPS to `https://mortis-relay.mmg-wolfpoolyt.workers.dev`.

- Public: `GET /v1/health`
- Operator: `/admin/{ADMIN_PATH_SEG}/api/...` with Bearer session
- Step-up: `POST /api/session/stepup` then `x-mortis-confirm`

The APK never contains `ADMIN_PATH_SEG`, `ADMIN_SECRET`, `RELEASE_TOKEN`, or the Ed25519 private key.

## Distinctions (non-negotiable)

- Staging is not canon
- Approval is not publication
- Tease is not reveal
- Trigger is not reveal
- Seal is not publication
- Dataset release is not a binary release
- DM is not a player client
- Windows player = NO BINARY RELEASE
- Android player = NO BINARY RELEASE
