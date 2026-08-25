# Notifications

Android channels (created at process start):

| Channel | Events |
|---|---|
| Intake | new Drive / AI files |
| Review | items needing operator review |
| Release | candidate ready / published |
| Signing | unsigned export waiting on PC |
| System | Mortis down / session problems |

Preferences live in Settings. In-app log is local and is not the Mortis audit trail.

Health polling is **off** until enabled. When on, it checks `/v1/health` every 15 minutes and only notifies on genuine unavailability.
