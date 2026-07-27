# Soundboard (Android)

A personal soundboard: record short voice clips once, organise them into boards,
and play them with a tap — plus an optional **Auto-Parent AI** mode that listens,
transcribes speech **on-device**, and auto-plays the clip that best fits.

Native Kotlin + Jetpack Compose (Material 3), Room, DataStore, and a home-screen
widget. See [`../docs/ANDROID_APP_SPEC.md`](../docs/ANDROID_APP_SPEC.md) for the
full architecture and Play Store readiness notes.

## Privacy model (important)

- **Your recordings never leave the device.** Audio clips are stored in app-private
  storage and are only ever played locally.
- **AI matching sends text only.** When Auto-Parent AI is enabled, nearby speech is
  transcribed to text **on the device** (Android speech recognition), and only that
  **text** is sent to Google's **Gemini Flash Lite** API to pick which clip to play.
  The raw audio is never uploaded.
- Auto-Parent AI is **off by default** and only runs while the user turns it on.

## Run it locally

**Prerequisite:** [Android Studio](https://developer.android.com/studio) (latest stable).

1. Open Android Studio → **Open** → select this `android/` folder.
2. Let Gradle sync. Android Studio will generate the Gradle wrapper if needed.
3. (Optional, for AI) Create a file named **`.env`** in this folder with:
   ```
   GEMINI_API_KEY=your_real_key_here
   ```
   Without a real key the app still runs fully — Auto-Parent AI falls back to an
   on-device keyword heuristic. See `.env.example`.
4. Press **Run** to launch on an emulator or a connected device.

Debug builds need no signing setup (Android's default debug keystore is used).

## Build a release for Google Play

1. Create an upload keystore (or use Play App Signing's upload key).
2. Provide signing via environment variables before building:
   ```
   KEYSTORE_PATH=/path/to/upload-key.jks
   STORE_PASSWORD=…
   KEY_ALIAS=upload
   KEY_PASSWORD=…
   ```
   (Or place the keystore at `android/upload-key.jks`.)
3. Build the App Bundle:
   ```
   ./gradlew bundleRelease
   ```
   The signed `.aab` under `app/build/outputs/bundle/release/` is what you upload to
   the Play Console.

## Notes

- `minSdk 24` (Android 7) · `targetSdk 36`.
- Application ID: `app.repeatless` (brand: **Repeatless** / repeatless.app) — this is **permanent** once published.
- The Gemini API key is embedded in the app via `BuildConfig`. Before publishing,
  restrict the key (Google Cloud Console → API key → application restrictions) or
  route calls through a small backend proxy; keys shipped in an APK can be extracted.
