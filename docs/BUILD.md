# Build

## Android (installable product)

```
android/
  app/                 Kotlin + Compose (com.vesper.mobile)
  gradle/libs.versions.toml
```

Requirements: JDK 17, Android SDK 35, Gradle 8.10.2.

```bash
cd android
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions on push/PR runs unit tests and `assembleDebug`, then uploads the APK.

Debug APKs from CI runs before the 0.1.1 startup-resilience series are superseded. They could die during EncryptedSharedPreferences / Keystore init.

### Release signing (human)

This environment has no Play / upload keystore. Debug APK is the automated artifact.

To produce a release APK on the authorized PC:

1. Create a keystore **off GitHub**. Never commit it.
2. `android/keystore.properties` (gitignored):

```
storeFile=/absolute/path/to/vesper.jks
storePassword=...
keyAlias=vesper
keyPassword=...
```

3. Wire `signingConfigs.release` in `app/build.gradle.kts`.
4. `gradle assembleRelease`

This is **application signing**, not Mortis dataset Ed25519 signing. Those keys are unrelated.

## Versioning

| Field | Meaning |
|---|---|
| `versionName` / `versionCode` | This APK |
| Mortis dataset version (`PD-…`) | Canon dataset, not this APK |
| Player binary version | Does not exist (NO_BINARY_RELEASE) |
