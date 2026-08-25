# Known limitations

These are honest blockers, not unfinished UI.

| Item | Status | Owner |
|---|---|
| Vesper Core transport (`vesper.client` v1) | NOT CONNECTED — in-process on PC only | PC repo |
| Local PC Vesper core | UNAVAILABLE | PC repo |
| Remote Vesper on Android | needs a configured endpoint | operator Settings |
| xAI remote on web companion | needs `XAI_API_KEY` in the preview env | environment |
| Worker Drive native poll | NOT_IMPLEMENTED without `GOOGLE_SA_JSON` | Cloudflare Worker |
| Import-signed verify | Worker may 501 if `EMBEDDED_PUBLIC_KEY` unbound | Cloudflare secret |
| Ed25519 dataset signing | authorized PC only | human + airgapped key |
| Play Store / release keystore | not in this environment | human |
| Player binaries | NO_BINARY_RELEASE | Mortis apps |
| Voice STT | architecture only | later device pass |
| Better Auth / Neon | **off**. Operator auth is Mortis | — |

Do not treat any of the above as a green status in the UI.
