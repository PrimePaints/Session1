# Soundboard — Android App Specification

**Purpose of this document:** a complete, self-contained build specification for
an AI coding agent (Gemini) to produce a production-, market-ready **Android**
app from an existing single-file web prototype. It states *what* to build and
the *constraints* to honour; the agent may choose implementation details where
this document is silent, but must not contradict anything stated here.

**Target outcome:** a polished, offline, native Android app published to the
Google Play Store. iOS/App Store is explicitly **out of scope**.

---

## 1. Executive summary (plain language)

The app is a **personal soundboard**. A user records short voice clips once
(e.g. "Brush your teeth!", "Well done!"), each clip becomes a big colourful
button, and tapping a button plays the clip. It is designed for one-handed
phone use. Everything is stored privately on the device — nothing is uploaded.

The current prototype is a working web app (single HTML file, ~720 lines,
included in this repository as `index.html`). This spec turns that prototype
into a real native Android product, keeping its look and behaviour but adding
the robustness, features, and store-readiness expected of a shipped app.

---

## 2. Product vision & target user

- **Who:** everyday people who repeat themselves a lot — parents, teachers,
  carers, coaches — plus hobbyist/meme soundboard users.
- **Core promise:** "Say it once. Tap it forever."
- **Principles:** instant to use, works fully offline, private by default,
  delightful and tactile, never gets in the user's way.

---

## 3. Scope for v1

**In scope (must ship in v1):**

1. Record, label, colour, play, re-record, rename, delete sound buttons ("pads").
2. **Multiple boards** (e.g. "Kids", "Work", "Funny") the user can create,
   rename, and switch between.
3. **Drag-to-reorder** pads within a board.
4. **Home-screen widget** with quick-play buttons for a chosen board.
5. **Haptic feedback** on record start/stop and on pad play.
6. **Backup & restore** to/from a single file (on-device only; no cloud).
7. **Free tier + optional one-time "Pro" unlock** via Google Play Billing.
8. Full offline operation. No account, no network dependency.

**Out of scope for v1 (note as future roadmap):**

- iOS / App Store.
- Cloud sync or accounts.
- Audio clip trimming/editing, importing external audio files, effects.
- Sharing clips to other apps.
- Folders/tags beyond the multi-board model.

---

## 4. Platform & recommended tech stack

The agent should build a **native Android** app (not a webview wrapper).

| Concern | Recommendation |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (Material You dynamic colour optional, but the brand palette in §12 takes precedence for pads/accents) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | Latest stable at build time (e.g. 35 / Android 15) |
| Audio recording | `MediaRecorder` → AAC in an `.m4a` (MPEG-4) container, mono, 44.1 kHz |
| Audio playback | `MediaPlayer` (or Media3/ExoPlayer if the agent prefers); low-latency, one clip at a time |
| Metadata storage | Room (SQLite) |
| Audio file storage | App-internal storage (`filesDir`), one file per clip |
| Preferences | Jetpack DataStore |
| Billing | Google Play Billing Library (latest, one-time product / non-consumable) |
| Widget | Jetpack Glance |
| DI (optional) | Hilt |
| Async | Coroutines + Flow |
| Min architecture | MVVM with a repository layer; unidirectional state in Compose |

The agent may substitute equivalent, well-maintained libraries if justified,
but must preserve: native UI, offline operation, and on-device-only storage.

---

## 5. Architecture overview

- **UI layer:** Compose screens + ViewModels exposing immutable UI state via
  `StateFlow`.
- **Domain/repository layer:** a `SoundboardRepository` mediating Room + file
  storage + backup serialization. All audio file I/O off the main thread.
- **Data layer:** Room database (metadata) + internal files (audio blobs) +
  DataStore (settings, Pro entitlement cache).
- **Playback controller:** a single component that owns the active
  `MediaPlayer`, guarantees only one clip plays at a time, releases resources
  on completion/stop, and survives configuration changes.

---

## 6. Data model

**Entities (Room):**

```
Board
  id: String (UUID)        // primary key
  name: String
  order: Int               // display order among boards
  createdAt: Long          // epoch millis

Pad
  id: String (UUID)        // primary key
  boardId: String          // FK -> Board.id, cascade delete
  label: String            // user-facing text on the button
  colorHex: String         // e.g. "#ff6b61"
  order: Int               // display order within the board
  audioFileName: String    // relative filename in internal storage
  mimeType: String         // e.g. "audio/mp4"
  durationMs: Long         // clip length, for progress UI
  createdAt: Long
```

- Deleting a `Board` deletes its `Pad`s and their audio files.
- Deleting a `Pad` deletes its audio file.
- `order` fields are re-normalised after reorder/delete.

**Settings (DataStore):**

- `activeBoardId: String`
- `widgetBoardId: String`
- `hapticsEnabled: Boolean` (default true)
- `themeMode` (see §12 — default: the app's dark console theme)
- `isPro: Boolean` (cached entitlement; source of truth is Play Billing)

---

## 7. Audio subsystem

**Recording:**

- Request `RECORD_AUDIO` permission at the moment the user first tries to record
  (rationale UI if previously denied; deep-link to settings if
  permanently denied).
- Format: AAC/`.m4a`, mono, 44.1 kHz, ~96 kbps.
- Enforce a **max clip length of 30 seconds** (matches prototype) with a live
  countdown; auto-stop at the limit.
- Show a live **input level meter** while recording (use
  `MediaRecorder.getMaxAmplitude()` polled on a timer, or an
  `AudioRecord`-based analyser).
- Provide a **review step**: play back the just-recorded clip, then
  **Save** or **Re-do** before it is committed.

**Playback:**

- Tap a pad → play its clip. Tap again → stop.
- Starting a new clip stops any currently playing clip (**one at a time**).
- Visual "playing" state on the pad plus a progress indicator driven by
  playback position.
- Respect the device's media/ring settings appropriately; play through the
  media stream. Do **not** duck or seize audio focus aggressively for such
  short clips — request transient focus only for the clip's duration.
- Release `MediaPlayer` resources promptly on stop/complete.

**Robustness:** handle missing/corrupt audio files gracefully (show the pad in
an error state with an option to re-record, never crash).

---

## 8. Screen-by-screen functional spec

### 8.1 Board screen (home)

- Sticky top bar: board name (tap to rename), a board switcher control
  (dropdown or bottom sheet listing boards + "New board"), and an overflow
  menu (⋯) with Backup, Restore, Settings, and (if free) "Upgrade to Pro".
- Grid of pads: 2 columns on phones (3 on large/landscape). Each pad shows a
  1-based index, its label, and a colour fill; a small ⋯ opens pad actions.
- A dashed **"＋ Record"** tile at the end of the grid starts a new recording.
- **Long-press + drag** a pad to reorder; order persists.
- Empty state (no pads): friendly explanation + prominent Record affordance.
- Empty state (no boards): auto-create a default board named "My Soundboard".

### 8.2 Record flow (modal / bottom sheet)

- States: **idle** → **recording** (timer + level meter + Stop) → **review**
  (Play, Re-do, Use) → on Use, if creating new, prompt for a **label** and
  auto-assign the next colour from the palette; if re-recording, replace audio
  in place.
- Cancel at any point discards the in-progress recording and cleans up temp
  files.

### 8.3 Pad actions (bottom sheet)

- Play, Rename, Re-record, Change colour (swatch picker from the palette in
  §12), Delete (with confirm dialog).

### 8.4 Board management

- Create board (prompt for name), rename board, delete board (confirm; blocks
  deleting the last remaining board — always keep ≥1), reorder boards.
- **Free tier limit:** up to **2 boards** and **up to 12 pads per board**.
  **Pro** removes both limits. When a limit is hit, show a non-blocking upsell
  explaining Pro.

### 8.5 Settings

- Toggle haptics.
- Theme selection (see §12).
- Backup / Restore.
- Upgrade to Pro / Restore purchases.
- About, privacy policy link, open-source licenses.

### 8.6 Home-screen widget (Glance)

- Configurable widget: on placement, the user picks which board it shows.
- Renders that board's pads as a compact tappable grid (respect widget size;
  paginate or cap gracefully on small widgets).
- Tapping a widget pad plays the clip via a background service/receiver
  without necessarily opening the app.
- Widget updates when the underlying board changes.
- **Free tier:** 1 widget / limited pads; **Pro:** unlimited. (Agent may tune
  exact free caps; document whatever it picks.)

---

## 9. Monetization — Free vs Pro

- Model: **free app with a single one-time in-app purchase** ("Soundboard Pro")
  via Google Play Billing (non-consumable product).
- **Free** includes full core functionality: recording, playback, 1 widget,
  backup/restore, up to **2 boards** and **12 pads per board**.
- **Pro** unlocks: **unlimited boards & pads**, **all themes**, **unlimited
  widgets**, and any premium colour packs.
- Requirements:
  - Verify entitlement on launch and cache it; degrade gracefully offline
    (last known entitlement honoured).
  - Provide a **"Restore purchases"** action.
  - No dark patterns; upsells are honest and dismissible.
  - Never gate a user's **existing** data behind Pro (e.g. if a Pro user lapses
    — not applicable to one-time purchase, but never delete over-limit data).

---

## 10. Permissions & privacy

- **`RECORD_AUDIO`** — required, requested contextually with clear rationale.
- **`POST_NOTIFICATIONS`** (Android 13+) — only if the widget/playback uses a
  foreground notification; otherwise omit.
- No `INTERNET` permission required except what Play Billing needs; the app must
  not transmit user audio or personal data anywhere.
- **Privacy stance:** all recordings and labels stay on-device. This must be
  reflected accurately in the Play **Data Safety** form (no data collected/shared)
  and in a short privacy policy (see §16).

---

## 11. Backup & restore (on-device, file-based)

- **Export:** produce a single portable file the user saves via the Android
  Storage Access Framework (`ACTION_CREATE_DOCUMENT`).
- **Import:** pick a file via `ACTION_OPEN_DOCUMENT`, validate, then merge or
  replace (ask the user which).
- **Format:** JSON, self-describing and versioned. For continuity with the
  existing web prototype, the Android app **must be able to import** the web
  app's backup format:

```json
{
  "app": "soundboard",
  "version": 1,
  "title": "My Soundboard",
  "pads": [
    {
      "id": "…",
      "label": "Brush your teeth!",
      "color": "#ff6b61",
      "order": 0,
      "mime": "audio/webm",
      "audio": "data:audio/webm;base64,…"
    }
  ]
}
```

- The web format has a single implicit board (`title` + flat `pads`); on import,
  map it to one Board. Audio is a base64 data URL — decode and store as a file;
  transcode to `.m4a` if the incoming codec (e.g. Opus/WebM) is not natively
  playable across target devices.
- The Android app should define a **v2** format that adds multiple boards, and
  must both read v1 and write v2 (and read its own v2). Document the schema.

---

## 12. Design system (carry the prototype's identity forward)

The prototype is a warm, tactile "drum-machine console". Preserve this feel.

**Palette (core):**

| Token | Hex | Use |
|---|---|---|
| ground | `#1a1620` | app background |
| panel | `#241e2b` | sheets, bars |
| panel-2 | `#2e2735` | inputs, raised rows |
| line | `#3a3242` | borders/dividers |
| cream | `#f4ece2` | primary text |
| muted | `#a495ad` | secondary text |
| rec (accent) | `#ff5147` | record / primary action |
| amber | `#ffb020` | confirm / secondary accent |

**Pad colour palette (assign cyclically to new pads):**
`#ff6b61  #ffb020  #2dd4bf  #a78bfa  #a3e635  #f472b6  #38bdf8  #fb923c  #f9d94a  #5eead4`

**Typography:** a friendly rounded family (Android equivalent of the
prototype's rounded system font — e.g. a rounded Google Font such as *Baloo 2*,
*Fredoka*, or *Nunito* bundled in-app). Heavy weights + slight negative tracking
for titles/labels; small uppercase tracked text for control captions; tabular
figures for the recording timer.

**Components:** large tappable pads with a subtle inner top-highlight and bottom
shadow (embossed/hardware look); rounded corners (~20dp); bottom-sheet dialogs;
a "power"/record indicator dot in the top bar. Minimum touch target 48dp.

**Theme:** the prototype commits to a single dark console theme. For the store
app, ship the **dark console theme as default**, and provide a **light variant
of equal quality** (do not naively invert — keep contrast and accent legible).
Additional themes may be Pro. Honour reduced-motion and large-font settings.

**Motion:** tactile press feedback, a recording pulse, and a play progress
sweep. Keep it purposeful; respect `prefers-reduced-motion` equivalent.

---

## 13. Non-functional requirements

- **Offline:** 100% of core features work with no network.
- **Performance:** cold start < 2s on mid-range devices; pad tap → audio start
  latency < 150ms perceptible; smooth 60fps grid scrolling with 50+ pads.
- **Reliability:** no data loss on process death, low storage, or interrupted
  recording (incoming call, etc.). Recording interrupted by an audio-focus loss
  should stop cleanly and offer to keep what was captured.
- **Accessibility:** TalkBack labels on every control; pads announce their
  label and "plays a sound" role; sufficient contrast; supports large fonts and
  display scaling; full keyboard/switch navigation where applicable.
- **Localization:** externalise all strings (`strings.xml`); ship English (en);
  structure for easy translation. Right-to-left safe layouts.
- **Storage hygiene:** show total space used by clips; deleting content frees
  files immediately.

---

## 14. Edge cases & error handling

- Microphone permission denied / permanently denied → clear guidance, no crash.
- Mic in use by another app → friendly error, retry.
- Storage full during recording → stop, inform, don't corrupt existing data.
- Corrupt/missing audio file for a pad → error state + re-record option.
- Very long labels → truncate/wrap gracefully.
- Importing a malformed or non-soundboard file → reject with a clear message.
- Rapid double-taps on a pad → debounce; never stack players.
- Device rotation / dark-light switch mid-record or mid-play → state preserved.

---

## 15. Analytics & crash reporting (optional, privacy-first)

- If included, use only privacy-respecting crash reporting (e.g. Play Console
  vitals / Firebase Crashlytics) with **no PII and no audio** ever collected.
- Any analytics must be disclosed in Data Safety and ideally opt-in. Default
  recommendation for v1: **crash reporting only**, or none.

---

## 16. Play Store launch checklist (agent should produce these)

- App name, short & full descriptions, feature graphic, screenshots (phone +
  optional 7"/10" tablet), high-res icon (512×512), adaptive launcher icon.
- Content rating questionnaire answers (expected: Everyone).
- **Data Safety** form completed truthfully (no data collected/shared;
  recordings stay on device).
- **Privacy policy** (short, hosted; must state on-device-only storage, mic
  usage purpose, no data sharing). A hostable draft should be generated.
- Signed release build (Android App Bundle, Play App Signing), version code/name
  scheme, ProGuard/R8 config that doesn't break Room/Billing/Glance.
- Target-SDK compliance for current Play requirements.
- Pre-launch report clean; tested on a range of API levels (24 → latest).

---

## 17. Testing requirements

- **Unit tests:** repository logic, backup serialization/deserialization
  (round-trip v1→import and v2→export→import), order normalisation, Pro-limit
  gating.
- **Instrumented/UI tests:** record→save→play flow, delete, reorder, board
  switching, permission-denied path, widget play.
- **Manual test matrix:** at least API 24, 30, and latest; a low-RAM device; a
  device with no Google account (billing unavailable path).
- Definition of done for each feature includes its tests passing in CI.

---

## 18. Suggested delivery milestones

1. **M1 — Core loop:** project scaffold, data model, record → save → play →
   delete on a single board. Brand theme applied.
2. **M2 — Boards & organisation:** multiple boards, switching, drag-to-reorder,
   rename/recolour, haptics.
3. **M3 — Data portability:** backup/restore (v2 write, v1+v2 read), storage
   management.
4. **M4 — Widget:** Glance widget with configuration and background playback.
5. **M5 — Monetization:** Play Billing, Pro entitlement, free-tier limits &
   upsells, restore purchases.
6. **M6 — Store readiness:** accessibility pass, localization scaffolding,
   privacy policy, store assets, release signing, testing matrix, launch.

Each milestone should be independently buildable and demoable.

---

## 19. Acceptance criteria (definition of done for v1)

- A user can install from the Play Store and, offline, create a board, record a
  clip (≤30s), label & colour it, play it, re-record, rename, reorder, and
  delete — with no crashes and no data loss across restarts.
- Multiple boards work within free limits; Pro unlock removes limits and
  restores across reinstalls.
- A home-screen widget plays a chosen board's clips.
- Backup exports a file that re-imports faithfully; a web-prototype v1 backup
  imports successfully.
- No user data leaves the device; Data Safety and privacy policy accurately
  reflect this.
- Passes accessibility checks (TalkBack usable end-to-end) and the automated
  test suite; meets current Play target-SDK and policy requirements.

---

## 20. Reference material provided

- `index.html` in this repository is the working prototype and the source of
  truth for behaviour, flows, copy tone, and visual identity. When this spec and
  the prototype disagree, **this spec wins**; where the spec is silent, mirror
  the prototype.

## 21. Assumptions & decisions already made (do not re-litigate)

- Android only; iOS out of scope.
- Storage is **on-device only**; no accounts, no backend, no cloud sync in v1.
- Monetization is a **single one-time Pro in-app purchase**; no ads.
- v1 scope = polished native port + widget, multiple boards, drag-reorder,
  haptics (per §3).

## 22. Open items for the product owner to confirm (nice-to-have, non-blocking)

- Final **app name** and Play Store listing copy (placeholder: "Soundboard").
- Exact **free-tier caps** (suggested: 2 boards, 12 pads/board, 1 widget) and
  **Pro price**.
- Whether to include crash reporting in v1 (§15).
- Which extra **themes/colour packs** (if any) are Pro.
