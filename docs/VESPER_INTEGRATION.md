# Vesper integration

## Providers

```
AIProvider
├── LocalVesper     UNAVAILABLE until the PC core exists
├── RemoteProvider  optional endpoint / xAI in the web companion
└── FutureProvider  reserved
```

The UI never talks to a model directly. It asks the provider. If no provider is available, the surface says so and does not invent a reply.

## PC bridge (intentionally empty)

Interfaces exist so the PC runtime can plug in later **without rewriting the app**:

- `VesperCoreClient`
- `VesperConversationProvider`
- `VesperToolProvider`
- `VesperMemoryProvider`
- `VesperPresenceProvider`

Modes: LOCAL / REMOTE / CLOUD / OFFLINE. The protocol is **not invented here**. Completing it is blocked on the unfinished local-PC Vesper core.

## Personality

Calm, capable, intelligent, observant, concise, slightly mysterious, useful. Mortis influence is atmospheric. No lore dumps. Epistemic stance:

- I checked.
- I think.
- I recommend.
- I requested.
- I changed. (only after a real mutation)
- I could not access.

## What this phone will not claim

- That the PC core ran
- That a tool executed locally
- Inbox counts, canon versions, or signatures that were not fetched
- That approve / tease / seal / publish happened because the UI looked ready
