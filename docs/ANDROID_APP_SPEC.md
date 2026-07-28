# Repeatless — Android App Specification (as built)

Doc set: current state & forward plan → [ROADMAP.md](ROADMAP.md) · strategy →
[BUSINESS_PLAN.md](BUSINESS_PLAN.md) · launch mechanics →
[LAUNCH_PLAYBOOK.md](LAUNCH_PLAYBOOK.md) · brand → [BRAND.md](BRAND.md).

**Status:** this describes the actual Android app in `../android/`, its architecture,
its privacy model, and what remains to reach the Google Play Store. It supersedes the
earlier "fully offline, nothing leaves the device" draft: the app now keeps **audio**
on-device but **does** use Google's **Gemini Flash Lite** for text-based AI matching.

Platform: **Android only** (Google Play). iOS/App Store is out of scope.

---

## 1. What it is

A personal soundboard. The user records short voice clips once, organises them into
one or more **boards**, and plays them with a tap. Beyond the basic soundboard it adds:

- **Multiple boards**, drag-to-reorder pads, rename/recolour, haptics.
- A **home-screen widget** that opens the chosen board. (Tap-to-play *from* the widget is
  not implemented yet — see §9.)
- **Backup / restore** to a local file (and import of the original web prototype's format).
- **Auto-Parent AI** — an optional mode that listens for children's speech and
  auto-plays the most fitting clip.

Positioning: "Say it once. Tap it forever." Audience: parents, teachers, carers, coaches.

---

## 2. Privacy & data model  ← the defining constraint

The rule the app is built to honour:

> **Users' recordings and any captured microphone audio never leave the device.
> Only text may be sent to the cloud. The AI features are enabled via Gemini Flash Lite.**

### What stays on the device (always)

- All recorded clips (audio files) — stored in app-private internal storage
  (`filesDir/audio_clips`), with Android Auto Backup **disabled** so the platform never
  copies them to Google Drive.
- All labels, boards, ordering, settings — Room + DataStore, on-device.
- Any microphone audio captured during Auto-Parent listening — transcribed locally
  and discarded; never uploaded.

### What may leave the device

| Data | Leaves device? | Destination | When |
|---|---|---|---|
| Audio recordings / clips | **Never** | — | — |
| Live microphone audio | **Never** | — | — |
| On-device **transcript text** (+ a loudness tone hint) | Yes | Google Gemini API | Only while Auto-Parent AI is **on** |
| Clip **labels** (as candidate options for matching) | Yes | Google Gemini API | Only while Auto-Parent AI is **on** |

Auto-Parent AI is **off by default**. With no Gemini API key configured, the app runs
entirely on-device using a keyword-heuristic fallback.

### How the AI pipeline works

1. `OnDeviceTranscriber` uses Android `SpeechRecognizer` (on-device / offline-preferred)
   to turn nearby speech into **text**. Audio is processed locally and discarded.
2. `AcousticHeuristic.analyzeAmplitude()` derives a coarse tone hint
   (yelling / whining / talking / quiet) from **loudness only** — on-device. (Despite an
   earlier name, there is no TensorFlow Lite model in the project; it is a plain
   loudness/energy heuristic.)
3. `GeminiAudioClassifier.classifySituationText()` sends **only** the transcript +
   tone hint + the list of the user's clip labels to **Gemini Flash Lite**
   (`gemini-3.1-flash-lite`), which returns the best-matching label.
4. The app plays the corresponding **local** clip.

Model choice is Flash Lite for lowest latency; the call uses low temperature and a
small output-token cap for speed.

---

## 3. Architecture & tech stack (as built)

| Concern | Implementation |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Min / target SDK | `minSdk 24` (Android 7) / `targetSdk 36` |
| Recording | `MediaRecorder` → AAC/`.m4a`, mono |
| Playback | `MediaPlayer`, one clip at a time |
| On-device STT | `SpeechRecognizer` (offline-preferred) |
| AI matching | Gemini Flash Lite via OkHttp (text only) |
| Metadata | Room (SQLite) |
| Audio files | App-internal storage (`filesDir`) |
| Preferences / entitlement | DataStore |
| Widget | AppWidget provider (board shortcut; pad playback not yet wired) |
| Purchases | Google Play Billing 9.1.0 (`billing-ktx`), one-time INAPP product `pro_unlock` |
| Navigation | Navigation-Compose |

Layering: Compose screens → `SoundboardRepository` (Room + files + backup) and
`UserPreferencesRepository` (DataStore); `AudioPlayer` owns single-stream playback;
`AutoParentEngine` drives the listen→transcribe→match loop; `BillingManager` owns the
Play connection and pushes the entitlement into DataStore (`isPro`).

---

## 4. Data model (Room)

```
Board(id, name, order, createdAt)
Pad(id, boardId → Board, label, colorHex, order, audioFileName, mimeType,
    durationMs, triggerTag?, createdAt)
```

- Deleting a board cascades to its pads and their audio files.
- `triggerTag` supports optional on-device acoustic trigger tags.

Settings (DataStore): `activeBoardId`, `widgetBoardId`, `hapticsEnabled`, `themeMode`,
`isPro`.

---

## 5. Monetization — Free + one-time Pro

Single model, no subscriptions, no ads:

- **Free:** full core soundboard — recording, playback, 1 widget, backup/restore,
  up to **2 boards** and **12 pads/board**.
- **Pro (one-time purchase):** unlimited boards & clips, plus **Auto-Parent AI
  listening**. These three are the limits actually enforced in code
  (`SoundboardRepository.FREE_MAX_BOARDS/FREE_MAX_PADS_PER_BOARD`, and the `isPro`
  checks in `BoardManagementDialog`, `BoardScreen` and `AutoParentSettingsScreen`).

> **Billing is wired.** Google Play Billing (Billing Library 9.1.0) is integrated in
> `billing/BillingManager.kt`, selling the non-consumable one-time product
> **`pro_unlock`**: it shows Play's localised price, acknowledges purchases, offers
> "Restore purchases", and treats Play as the source of truth while caching the
> entitlement to DataStore (`isPro`) so PRO keeps working offline. A "(Debug) Simulate
> PRO" switch exists on debug builds only. The remaining work is external: create the
> `pro_unlock` product in Play Console and price it (§9). The old three-tier
> FREE/PRO/ULTRA system and its subscription pricing were removed; it is now Free/Pro.

> **Not gated yet:** clip trimming, trigger tags, themes and colour packs were once
> advertised as Pro but are **not** restricted in code, so the Pro copy no longer
> claims them. Decide before launch whether to gate them or leave them free — see
> [ROADMAP.md](ROADMAP.md) v1.0 hardening. Never advertise an unenforced paid feature.

---

## 6. Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Record clips; Auto-Parent listening |
| `INTERNET` | **Text-only** Gemini Flash Lite calls (Auto-Parent) |
| `VIBRATE` | Haptics |
| `<queries>` speech `RecognitionService` | Bind on-device speech recognition (Android 11+) |

No location, camera, contacts, or storage-scope permissions.

---

## 7. Design system

Warm "drum-machine console" identity carried from the web prototype. The applied brand
system: ground `#1A1620`, cream text `#F4ECE2`, coral `#FF5147` (record/primary), amber
`#FFB020` (confirm/PRO), teal `#2DD4BF`, plus the 10-colour pad palette. Typography is
bundled **Baloo 2** (display) + **Nunito** (body) wired through `ui/theme/Type.kt`;
themes are `RepeatlessTheme` (Compose) and `Theme.Repeatless` (XML). Ships a **Console
Dark** default plus **Console Light** and **System**. Large tappable pads, bottom-sheet
dialogs, min 48dp touch targets. [BRAND.md](BRAND.md) is the source of truth.

---

## 8. Backup & restore

- Export via Storage Access Framework to a user-chosen JSON file (audio embedded as
  base64 — this is a **local file the user chooses**, not a network upload).
- Import supports the app's own format **and** the original web prototype's v1 backup
  (single implicit board → mapped to one Board; non-`.m4a` codecs handled on import).
- Portability is **one-way**: web v1 → Android v2. The Android v2 file nests pads under
  `boards[]`, which the web prototype's restore does not read, so Android backups are
  not importable into the web demo. The `{"app":"soundboard"}` envelope key and the
  IndexedDB name are frozen format identifiers, not brand text — do not "rebrand" them
  or v1 import breaks.

---

## 9. Google Play readiness checklist

**Done / in place**
- Native release build config; branded application ID and namespace `app.repeatless`
  (brand: Repeatless — see [BRAND.md](BRAND.md)).
- Kotlin source package renamed off the `com.example` AI-Studio placeholder.
- **Google Play Billing integrated** (Billing Library 9.1.0, `BillingManager.kt`,
  one-time product `pro_unlock`, Play-supplied localised pricing, purchase
  acknowledgement, Restore purchases, debug-only entitlement toggle).
- Debug builds work with the default debug keystore (no manual setup); a release build
  without a keystore now logs a loud warning instead of silently emitting an unsigned AAB.
- Adaptive launcher icon (coral pad mark on `#1A1620`) with an Android 13 themed
  monochrome variant; app label **"Repeatless"**; theme `Theme.Repeatless`.
- Bundled brand fonts (Baloo 2 + Nunito, SIL OFL — `android/FONTS_LICENSE.md`).
- **Auto Backup disabled** (`android:allowBackup="false"`) with real exclusion rules for
  `audio_clips` and `soundboard.db`, so the platform can never upload recordings to
  Google Drive. This is what makes the privacy claim literally true.
- Firebase / google-services / Smart Home / cloud-audio paths removed; unused
  version-catalog entries and the unused OkHttp logging interceptor removed.
- Gemini failure logs no longer write response payloads to logcat in release builds.
- Permissions minimal and justified. Privacy policy written and hosted (`privacy.html`).

**Required before publishing**
1. **Create the `pro_unlock` in-app product** in Play Console (Monetise → Products →
   In-app products), price it regionally, add a license tester, and complete one real
   test purchase on the internal-testing track. Client-side billing is already done.
2. **Data Safety form** — must be truthful. Declare that when Auto-Parent AI is used,
   **text (transcripts + labels)** is shared with Google (Gemini) for app functionality;
   **audio is not collected or shared**; recordings stay on-device.
3. **Privacy policy URL** — `privacy.html` is written and live at
   `https://primepaints.github.io/Repeatless-Android/privacy.html`; paste it into the
   listing (re-point to `repeatless.app/privacy` once the domain is registered). Keep it,
   the in-app dialog in `SettingsScreen.kt`, and the Data Safety form in sync.
4. **Gemini API key hardening** — the key is embedded via `BuildConfig`. Restrict it in
   Google Cloud Console (Android app + API restrictions) or proxy via a small backend;
   keys in an APK are extractable.
5. **Signing** — create an upload key; use Play App Signing. Build `bundleRelease` (AAB)
   and verify it is actually signed (`apksigner verify`) before uploading.
6. **Store assets** — icon (512²), feature graphic, phone screenshots, short/full
   descriptions, content rating questionnaire (expected: Everyone / PEGI 3).
7. **Target-SDK / pre-launch report** — pass Play's current target-SDK policy and the
   automated pre-launch report; test API 24 → latest.
8. **Foreground behaviour + mic disclosure** — Auto-Parent is currently tied to the
   Activity lifecycle (stopped in `onDispose`) with a visible listening status, but there
   is **no prominent mic-use disclosure dialog and no foreground service yet**. Both are
   pre-launch blockers under Play's prominent-disclosure policy.
9. **Widget playback** — the widget currently only opens the board; `widget_pad_item.xml`
   and `AudioPlaybackService.playPad()` are unused. Either wire tap-to-play (needs a
   `RemoteViewsService` adapter and a *foreground* service, since API 26+ forbids starting
   a background service from a widget click) or keep the copy describing a shortcut.
10. **Regenerate legacy raster launcher icons** — `mipmap-{m,h,xh,xxh,xxxh}dpi` still hold
   the pre-rebrand AI-Studio images, which is what API 24–25 devices show.
11. **First Android Studio compile pass** — the project has never been built with an
    Android SDK present.

**Recommended polish**
- Decide whether to gate or keep-free the un-gated extras (trimming, trigger tags,
  themes, colour packs) — see §5.
- Add on-device encryption at rest for clips (Jetpack Security) if extra assurance is wanted.
- Enable R8/minification with keep rules for Room + Billing (`isMinifyEnabled = false`
  today) and re-test the release build end to end.
- Extract user-facing strings into `strings.xml` (currently all hardcoded in Compose),
  which is also the prerequisite for localization.

The forward plan and its ordering live in [ROADMAP.md](ROADMAP.md).

---

## 10. Privacy policy

The shipped policy is **`../privacy.html`** at the repo root, published at
`https://primepaints.github.io/Repeatless-Android/privacy.html`. It names the controller
and the support contact, and it is the URL that goes in the Play listing.

Three surfaces state the same facts and **must be kept in sync** — if one changes, change
all three in the same commit:

1. `privacy.html` (the public policy),
2. the in-app privacy dialog in `SettingsScreen.kt`,
3. the Play **Data Safety** form (mapping in
   [LAUNCH_PLAYBOOK.md](LAUNCH_PLAYBOOK.md) §2).

The substance: recordings and microphone audio never leave the device; only on-device
transcript text plus clip labels go to Gemini Flash Lite, and only while Auto-Parent AI
is switched on; no accounts, no analytics, no cloud copies.

---

## 11. Testing

There are currently **no tests** — the AI-Studio scaffold tests were deleted because they
asserted placeholder values ("My Application", `com.example`) and one referenced a removed
theme, so the suite could not compile. Highest-value tests to write first:

- Unit: backup round-trip (v1 import + own format), tier/limit gating, order normalisation.
- Instrumented: record → save → play → delete, reorder, board switching,
  permission-denied path, widget playback.
- Manual matrix: API 24 / 30 / latest; a device with no Google account (billing absent);
  Auto-Parent with and without a Gemini key; on-device STT available vs unavailable.

---

## 12. Assumptions locked in

- Android only; on-device audio; **text-only** cloud AI via Gemini Flash Lite; no Gemini
  Live; Free + one-time Pro; no Firebase; no Smart Home.
