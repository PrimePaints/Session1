<div align="center">

# Repeatless

### *Say it once. Tap it forever.*

**Stop sounding like a broken record.** Record the things you say to your kids every
single day — "brush your teeth", "stop fighting", "eat your veggies" — once, in your own
voice. Then tap a button. Or let the app answer for you.

[Web demo](https://primepaints.github.io/Repeatless-Android/) ·
[Privacy policy](https://primepaints.github.io/Repeatless-Android/privacy.html) ·
[Roadmap](docs/ROADMAP.md) ·
[Business plan](docs/BUSINESS_PLAN.md)

</div>

---

## What this repo is

The full working project for **Repeatless**, an Android app for parents who are tired of
repeating themselves — the app itself plus everything needed to take it to market.

| Path | What it is |
|---|---|
| **`android/`** | The real product: a native Kotlin + Jetpack Compose Android app. See [`android/README.md`](android/README.md) to build and run it. |
| **`index.html`** | The original single-file web prototype. Still useful as a zero-install demo and marketing page — served at the Pages link above. |
| **`privacy.html`** | The public privacy policy required by the Play listing. |
| **`docs/`** | Product spec, brand book, business plan, launch playbook, roadmap. |

**Status:** pre-launch. The app is built; it has not been compiled on a machine with the
Android SDK yet, and it is not on Google Play. Current state and next actions:
[`docs/ROADMAP.md`](docs/ROADMAP.md).

## What it does

- 🎙️ **Record once** — your real voice, not a robot reading text.
- 🔊 **Tap to play** — big colourful pads, built for one hand.
- 🗂️ **Boards** for each routine: mornings, bedtime, car rides, homework.
- 🏠 **Home-screen widget** — your current board, one tap from the home screen.
- 🧠 **Auto-Parent AI** — turn on listening mode and Repeatless hears the whining,
  works out what it's about, and plays *your* matching clip while you drink your coffee.
- 💾 **Backup to a file you own.** No account, no ads, no subscription.

The **free** tier gives you 2 boards of up to 12 clips each, the widget, and backup.
A **one-time PRO purchase** (no subscription) unlocks unlimited boards and clips plus
Auto-Parent AI.

## The privacy model (the part that matters)

This is the product's spine, and it's enforced in the architecture rather than promised in
a policy:

> **Your recordings never leave your phone.**

- Recorded clips live in app-private storage on the device. They are never uploaded.
- Auto-Parent AI transcribes nearby speech to text **on the device** (Android's
  on-device speech recognition). The captured audio is used locally and discarded.
- Only the resulting **text** — the transcript plus your clip labels — is sent to Google's
  **Gemini Flash Lite** to decide which of your clips to play. Never the audio.
- Auto-Parent AI is **off** until you turn it on. With no API key configured, the app falls
  back to fully on-device keyword matching.

Full detail, including the exact Play Data Safety mapping, is in
[`docs/ANDROID_APP_SPEC.md`](docs/ANDROID_APP_SPEC.md).

## Documentation

| Doc | Read it for |
|---|---|
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Where we are, what ships next, what we've said no to |
| [`docs/ANDROID_APP_SPEC.md`](docs/ANDROID_APP_SPEC.md) | Architecture, data model, privacy flows, Play readiness checklist |
| [`docs/BRAND.md`](docs/BRAND.md) | Palette, typography, logo, voice & tone |
| [`docs/BUSINESS_PLAN.md`](docs/BUSINESS_PLAN.md) | Market research, competition, pricing, unit economics, financials |
| [`docs/LAUNCH_PLAYBOOK.md`](docs/LAUNCH_PLAYBOOK.md) | Step-by-step to a live Play listing, with store copy and content plan |
| [`android/README.md`](android/README.md) | Build, run, sign, and release the Android app |

## Quick start

**Try the concept in 10 seconds** — open the
[web demo](https://primepaints.github.io/Repeatless-Android/) on your phone and record a
clip. Everything stays in your browser.

**Build the Android app:**

```bash
git clone https://github.com/PrimePaints/Repeatless-Android.git
```

Open the `android/` folder in Android Studio, let Gradle sync, and press Run. The optional
Gemini key goes in `android/.env` (see [`android/.env.example`](android/.env.example)) —
without it the app still runs, using the on-device fallback.

## Tech

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · `MediaRecorder` / `MediaPlayer` ·
Android `SpeechRecognizer` (on-device) · Gemini Flash Lite (text only) · Google Play
Billing · App Widget · bundled Baloo 2 + Nunito

---

© 2026 Prime Paints (South Africa). All rights reserved. **Proprietary and confidential** —
this repository is public for transparency and demonstration only. It is not open-source,
no licence to use, copy, modify, or distribute the code or the Repeatless brand is granted,
and issues/pull requests are not accepted. Full terms: [`COPYRIGHT.md`](COPYRIGHT.md).
Bundled fonts are licensed separately under the SIL OFL — see
[`android/FONTS_LICENSE.md`](android/FONTS_LICENSE.md).
