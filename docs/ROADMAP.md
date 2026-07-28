# Repeatless — Roadmap

Where the product is now, what ships next, and what we've deliberately said no to.
Strategy lives in [BUSINESS_PLAN.md](BUSINESS_PLAN.md); launch mechanics live in
[LAUNCH_PLAYBOOK.md](LAUNCH_PLAYBOOK.md); technical detail lives in
[ANDROID_APP_SPEC.md](ANDROID_APP_SPEC.md).

**Last reviewed:** 28 July 2026 · **Stage:** pre-launch, not yet on Google Play

---

## Now — where we actually are

| Area | State |
|---|---|
| Android app (Kotlin, Compose, Room, widget) | ✅ Built |
| Record / label / colour / reorder / boards | ✅ Built |
| Auto-Parent AI (on-device STT → text-only Gemini Flash Lite) | ✅ Built |
| Privacy architecture (audio never leaves device) | ✅ Built |
| Free + one-time PRO tiering | ✅ Built |
| Google Play Billing (`pro_unlock`) | ✅ Integrated in code |
| Brand system (palette, Baloo 2 + Nunito, icon) | ✅ Applied |
| Web prototype / demo (`index.html`) | ✅ Live at [primepaints.github.io/Repeatless-Android](https://primepaints.github.io/Repeatless-Android/) |
| Privacy: Auto Backup disabled + exclusion rules | ✅ Done (recordings can't reach Google Drive) |
| Kotlin package renamed off `com.example` | ✅ Done (`app.repeatless`) |
| Repo hygiene (gitignore for keys/`.env`, dead deps removed) | ✅ Done |
| Business plan, launch playbook, brand book | ✅ Written |
| **Android Studio compile + device test** | ❌ **Not done — next action** |
| Domain `repeatless.app` | ❌ Not registered |
| Play Console account + `pro_unlock` product | ❌ Not created |
| Store graphics (icon export, feature graphic, screenshots) | ❌ Not produced |

---

## Next — the critical path to launch

Ordered. Each step unblocks the one after it.

1. **Compile and run** the app in Android Studio; fix whatever the compiler and a real
   device surface. Nothing else matters until the app runs.
2. **Register `repeatless.app`** and claim the social handles (they're first-come).
3. **Create the Google Play account.** Decide personal (subject to the 12-testers ×
   14-days rule) vs organization via Prime Paints (exempt, needs D‑U‑N‑S). See the
   playbook — this choice sets the launch date.
4. **Create the `pro_unlock` in-app product** in Play Console and price it regionally.
   Add yourself as a license tester and complete one real test purchase.
5. **Ship the pre-launch hardening** in "v1.0 hardening" below.
6. **Produce store assets** — 512² icon, feature graphic, six phone screenshots (captions
   are written in the playbook §3).
7. **Complete the Play Data Safety form** — declare that *text* (transcripts + clip
   labels) is shared with Google when Auto-Parent is on, and that *audio is not*. Mapping
   is in [LAUNCH_PLAYBOOK.md](LAUNCH_PLAYBOOK.md) §2.
8. **Closed test → production review → launch**, with the content engine already running.
   Personal account: 12 testers × 14 days (§4). Organization account: exempt (§4b).

---

## v1.0 hardening — before the store listing goes live

Small, known, and each one is a real risk if skipped.

- **Restrict the Gemini API key** in Google Cloud Console (Android package + SHA-256,
  daily quota caps). The key ships inside the APK and can be extracted.
- **Daily Auto-Parent match cap** (~200/device/day) so a runaway listener can't run up
  API spend.
- **Prominent mic disclosure** on first Auto-Parent enable, satisfying Play's policy.
- **Verify continuous-listening compliance** — foreground-only, or a compliant
  foreground service with a visible notification.
- **Regenerate legacy raster launcher icons** (API 24–25) from the adaptive vector, and
  add a raster `favicon.png`/`apple-touch-icon` for the web pages (Safari ignores SVG icons).
- **Decide on the widget**: it currently only opens the board — `widget_pad_item.xml` and
  `AudioPlaybackService.playPad()` are unused. Either wire tap-to-play from the widget
  (needs a `RemoteViewsService` and a *foreground* service; API 26+ blocks starting a
  background service from a widget click) or keep describing it as a shortcut. All copy
  now describes the shortcut behaviour.
- **Open-source licenses screen** surfacing the bundled font attributions.
- **Create the upload keystore** and enrol in Play App Signing — without it
  `bundleRelease` emits an *unsigned* AAB that Play rejects (the build now warns).
- **Enable R8/minification** with keep rules for Room + Billing (`isMinifyEnabled = false`
  today) and re-test the release build end to end.
- **Decide: gate or keep-free** the extras still advertised nowhere but present for all —
  clip trimming, trigger tags, themes, colour packs. Either add `isPro` checks or leave
  them free; just never advertise an unenforced paid feature. (Pro copy currently claims
  only what the code enforces.)
- **Write the first real tests.** The AI-Studio scaffold tests were deleted (they asserted
  "My Application" and `com.example`, and one referenced a removed theme). Best first
  targets: backup round-trip, tier gating, Gemini response parsing.

---

## v1.1 — first update after launch, driven by reviews

- **Gemini proxy**: move the API call behind a thin Cloudflare Worker so no key ships in
  the binary and spend is centrally capped.
- **Pricing experiment**: $4.99 vs $6.99, measured on conversion not opinion.
- **Whatever reviews demand.** Ship the top-requested thing, not our favourite thing.
- Onboarding polish: guided first recording (activation is ≥3 clips recorded).
- Crash/vitals watch via Play Console.

---

## Later — earned, not assumed

Each of these is gated on a signal, so we don't build ahead of demand.

| Idea | Gate |
|---|---|
| **iOS app** | Android proves conversion ≥2.5%; measure demand via the landing-page iOS waitlist first (~R15k: Mac + Apple fee) |
| **Scheduled clips / routines** ("play 'Brush your teeth' at 19:30") | Requested repeatedly in reviews |
| **Shared family boards** (both parents' voices, one board) | Requested; needs a sync story, which breaks the no-backend simplicity — design carefully |
| **Bluetooth / speaker casting** | Requested |
| **Clip packs** (grandparent voices, languages, funny sets) | Retention data shows people run out of clips |
| **Localization** (Afrikaans first, then top install markets) | Non-English installs grow organically. Note the real cost: all copy is currently hardcoded in ~20 Compose files, so this needs a `strings.xml` extraction pass first |
| **Widget expansion** (multiple sizes, board picker) | Widget usage is high in analytics-free proxies (reviews, feedback) |

---

## Explicitly not doing

Saying no is a feature. These were considered and rejected — revisit only with a real reason.

- **Uploading audio anywhere.** Non-negotiable. "Your recordings never leave your phone"
  is the product's spine; it was the single biggest fix made to the AI-generated code.
- **Gemini Live / realtime voice streaming.** Streams live mic audio to the cloud —
  contradicts the above.
- **Subscriptions.** One-time PRO matches the "record once" promise and parents are
  subscription-fatigued. (A future additive "Repeatless+" is only ever *additive* —
  never move a purchased feature behind it.)
- **Ads.** Kills the trust the privacy story buys.
- **Accounts / cloud sync in v1.** Adds a backend, a breach surface, and a support load
  for a phone-local product.
- **"Designed for Families" Play programme.** The app is a tool *for parents*, not a
  children's app; enrolling would invite the wrong policy regime and the wrong framing.
- **Smart-home / Google Home casting.** Was in the AI-generated code, stripped out as
  scope creep with no user pull.

---

## How we'll know it's working

90-day post-launch targets (from the business plan): ≥5,000 installs, ≥125 PRO sales,
≥4.5★, D7 retention >20%, activation (≥3 clips recorded) >60%, one video ≥250k views,
and a made decision on iOS.

Break-even is ~25 PRO sales, so the downside is capped and the upside is a viral
consumer utility. Review this file whenever a gate above is hit or missed.
