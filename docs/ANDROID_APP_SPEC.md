# Soundboard — Android App Specification (as built)

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
- A **home-screen widget** for quick playback.
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

- All recorded clips (audio files) — stored in app-private internal storage.
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
2. `TFLiteAcousticClassifier.analyzeAmplitude()` derives a coarse tone hint
   (yelling / whining / talking / quiet) from **loudness only** — on-device.
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
| Widget | AppWidget provider + playback service |
| Navigation | Navigation-Compose |

Layering: Compose screens → `SoundboardRepository` (Room + files + backup) and
`UserPreferencesRepository` (DataStore); `AudioPlayer` owns single-stream playback;
`AutoParentEngine` drives the listen→transcribe→match loop.

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
- **Pro (one-time purchase):** unlimited boards & clips, **Auto-Parent AI listening**,
  audio trimming, custom acoustic trigger tags, all themes/colour packs.

> **Not yet wired:** the Pro unlock currently toggles a local entitlement flag via a
> tier dialog. **Google Play Billing is not integrated yet** — that is a required step
> before charging (see §9). The old three-tier FREE/PRO/ULTRA system and its
> subscription pricing were removed; it is now Free/Pro only.

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

Warm "drum-machine console" identity carried from the web prototype: near-black warm
ground, cream text, coral-red record accent, amber secondary, a curated pad-colour
palette. Ships a **Console Dark** default plus **Console Light** and **System** themes.
Large tappable pads, bottom-sheet dialogs, min 48dp touch targets.

---

## 8. Backup & restore

- Export via Storage Access Framework to a user-chosen JSON file (audio embedded as
  base64 — this is a **local file the user chooses**, not a network upload).
- Import supports the app's own format **and** the original web prototype's v1 backup
  (single implicit board → mapped to one Board; non-`.m4a` codecs handled on import).

---

## 9. Google Play readiness checklist

**Done / in place**
- Native release build config; branded application ID `app.repeatless` (brand: Repeatless — see `BUSINESS_PLAN.md`).
- Debug builds work with the default debug keystore (no manual setup).
- Adaptive launcher icons; app label "Soundboard".
- Firebase / google-services / Smart Home / cloud-audio paths removed.
- Permissions minimal and justified.

**Required before publishing**
1. **Google Play Billing** — integrate real one-time Pro purchase + "Restore purchases".
   Today's tier toggle is a local flag only.
2. **Data Safety form** — must be truthful. Declare that when Auto-Parent AI is used,
   **text (transcripts + labels)** is shared with Google (Gemini) for app functionality;
   **audio is not collected or shared**; recordings stay on-device.
3. **Privacy policy** — host publicly and link in the listing (draft in §10).
4. **Gemini API key hardening** — the key is embedded via `BuildConfig`. Restrict it in
   Google Cloud Console (Android app + API restrictions) or proxy via a small backend;
   keys in an APK are extractable.
5. **Signing** — create an upload key; use Play App Signing. Build `bundleRelease` (AAB).
6. **Store assets** — icon (512²), feature graphic, phone screenshots, short/full
   descriptions, content rating questionnaire (expected: Everyone / PEGI 3).
7. **Target-SDK / pre-launch report** — pass Play's current target-SDK policy and the
   automated pre-launch report; test API 24 → latest.
8. **Foreground behaviour** — confirm Auto-Parent continuous listening either runs only
   while the screen is active or uses a compliant foreground service + notification, and
   that mic-use disclosure meets Play's prominent-disclosure policy.

**Recommended polish**
- Rebrand the internal source package (`com.example`) to match the application ID
  (optional; not visible to users, does not block publishing).
- Add on-device encryption at rest for clips (Jetpack Security) if extra assurance is wanted.
- Enable R8/minification with proper keep rules once billing/Room are stable.

---

## 10. Privacy policy (draft to host)

> **Soundboard — Privacy Policy**
>
> Your recordings stay on your device. All audio clips, labels and boards you create are
> stored only on your device and are never uploaded to us or anyone else.
>
> **Microphone.** The microphone is used to record your clips. If you turn on the
> optional Auto-Parent AI feature, the microphone is also used to transcribe nearby
> speech to text **on your device**.
>
> **AI matching (optional, off by default).** When Auto-Parent AI is enabled, the
> on-device transcript (text) and your clip labels are sent to Google's Gemini API to
> decide which of your clips to play. **Your audio recordings are never sent.** This
> processing is governed by Google's API terms.
>
> **Your control.** Auto-Parent AI is off unless you enable it. You can export or delete
> your data at any time. Uninstalling the app removes all local data.
>
> Contact: <your support email>.

---

## 11. Testing

- Unit: backup round-trip (v1 import + own format), tier/limit gating, order normalisation.
- Instrumented: record → save → play → delete, reorder, board switching,
  permission-denied path, widget playback.
- Manual matrix: API 24 / 30 / latest; a device with no Google account (billing absent);
  Auto-Parent with and without a Gemini key; on-device STT available vs unavailable.

---

## 12. Assumptions locked in

- Android only; on-device audio; **text-only** cloud AI via Gemini Flash Lite; no Gemini
  Live; Free + one-time Pro; no Firebase; no Smart Home.
