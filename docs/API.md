# API

Live Worker: `https://mortis-relay.mmg-wolfpoolyt.workers.dev`

This client does not reimplement Mortis. It calls the existing Operator Room.

## Public

| Method | Path | Notes |
|---|---|---|
| GET | `/v1/health` | CORS enabled. Used for Mortis connection status. |
| GET | `/v1/channel` | Channel pointer. Dataset ≠ binary. |
| GET | `/v1/release` | Published dataset metadata. |

## Operator (after unlock)

Base: `/admin/{ADMIN_PATH_SEG}`

| Method | Path | Confirm |
|---|---|---|
| POST | `/api/session/unlock` | `{ passphrase, operator_id }` |
| POST | `/api/session/logout` | |
| POST | `/api/session/refresh` | |
| POST | `/api/session/stepup` | `{ passphrase, op }` → `{ confirm }` |
| GET | `/api/dashboard` | live tiles |
| GET | `/api/status` | drive / signing flags |
| GET | `/api/inbox` | filters: q, state, source, class, sensitivity, sort, attention, limit, offset |
| POST | `/api/inbox/sync` | may be NOT_IMPLEMENTED without Worker Drive creds |
| POST | `/api/intake/push` | existing Drive/agent path |
| GET | `/api/staging` | |
| GET | `/api/staging/{id}` | |
| GET | `/api/staging/{id}/diff` | |
| POST | `/api/staging/{id}/{review\|edit\|approve\|reject\|hide\|unhide\|prepare\|tease\|schedule\|seal}` | approve/reject/hide/seal need step-up |
| GET | `/api/discovery` | teasers + fragments. Tease ≠ reveal |
| GET | `/api/schedule` | upcoming / due / completed / cancelled |
| POST | `/api/schedule/{id}/cancel` | |
| GET | `/api/applications` | apps, channels, published datasets |
| GET | `/api/releases` | |
| GET | `/api/release/candidate` | |
| GET | `/api/release/diff` | |
| POST | `/api/release/generate` | |
| POST | `/api/release/leakscan` | |
| POST | `/api/release/export-unsigned` | |
| POST | `/api/release/import-signed` | `release.import` |
| POST | `/api/release/publish` | `release.publish` + phrase `PUBLISH` |
| POST | `/api/channels/assign` | STABLE requires `channel.assign` |
| POST | `/api/channels/promote` | always confirm |
| POST | `/api/channels/schedule` | DEV/TEST/PREVIEW only |
| GET | `/api/audit` | append-only |
| GET | `/api/fragments` | |

Legal staging actions are copied from the Worker. The client must not invent transitions.

## Auth

Unlock returns `{ session, exp, abs, scopes, actor }`. Subsequent calls send `Authorization: Bearer <session>`. Step-up returns a one-time nonce sent as `x-mortis-confirm`.
