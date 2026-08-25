# Voice

Prepared, not blocking.

| Capability | Status |
|---|---|
| Microphone permission | not requested until STT is real |
| Speech-to-text | interface only |
| Speech output | web: browser `speechSynthesis` when present; Android: reserved |
| Push-to-talk | UI disabled with honest label |

Integration point on Android: bind a platform SpeechRecognizer in the voice engine when a device test confirms Samsung STT quality. Until then the chat remains typed.
