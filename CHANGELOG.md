# Changelog

All notable changes to Repeatless. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); each released section maps to
the Android `versionCode`/`versionName` in `android/app/build.gradle.kts` so a Play
release can be traced back to a commit. Entries here double as Play release notes.

## [Unreleased] — targeting 1.0 (versionCode 1)

### Added
- Native Android app: record, label, colour, reorder and play clips; multiple boards;
  a home-screen widget that opens the chosen board; haptics; Console Dark / Console
  Light / System themes.
- **Auto-Parent AI**: listens, transcribes speech **on-device**, and auto-plays the
  best-matching clip. Sensitivity, cooldown, feedback training and a test simulator.
- **Google Play Billing** (Billing Library 9.1.0) for the one-time `pro_unlock` purchase,
  with Play-supplied localised pricing, purchase acknowledgement and Restore purchases.
- Backup / restore to a user-chosen file, including import of the original web
  prototype's v1 backups.
- Repeatless brand system: coral/amber/warm-dark palette, bundled Baloo 2 + Nunito,
  adaptive launcher icon with an Android 13 themed variant.
- Proprietary copyright notice, roadmap, and this changelog.

### Changed
- Kotlin source package and Gradle namespace renamed from the AI-Studio placeholder
  `com.example` to **`app.repeatless`**, matching the application ID.
- Tiering simplified from FREE/PRO/ULTRA (with subscription pricing) to **Free + one-time
  PRO**. Pro copy now claims only the limits the code actually enforces.
- Pro/Free copy discloses the free cap explicitly (2 boards × 12 clips).
- User-facing "TFLite" jargon removed; the class is now honestly named
  `AcousticHeuristic` (there is no TensorFlow Lite model in the project).

### Fixed
- **Privacy: Android Auto Backup is now disabled** (`allowBackup="false"`) with real
  exclusion rules for `audio_clips` and `soundboard.db`. Previously the platform would
  have uploaded users' voice recordings to Google Drive, contradicting the app's core
  promise that recordings never leave the device.
- Gemini error paths no longer write response payloads (transcript-derived content) to
  logcat on release builds.
- Release builds now warn loudly when no signing keystore is present, instead of silently
  producing an unsigned AAB that Play rejects.

### Removed
- Firebase, google-services, and the Smart Home feature (AI-generated scope creep).
- The raw-audio-to-Gemini upload path — replaced by on-device transcription with a
  text-only API call.
- AI-Studio scaffold tests that asserted placeholder values and referenced a deleted
  theme (the suite could not compile).
- 13 unused version-catalog libraries (camera, Coil, Retrofit, Accompanist, credentials,
  Google Sign-In, location, and the OkHttp logging interceptor) plus 12 orphan version
  entries.

### Security
- Added a root `.gitignore` and widened `android/.gitignore` so `.env`, `*.jks`,
  `*.keystore`, `keystore.properties` and IDE state can never be committed to this
  public repo. The build script's default keystore path is now covered.
