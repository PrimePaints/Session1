# Repeatless — Business Plan

**Product:** Repeatless — a soundboard + AI app for parents who are tired of saying
the same thing over and over.
**Tagline:** *Say it once. Tap it forever.*
**Marketing hook:** *Stop sounding like a broken record.*
**Founder:** Dylan (Prime Paints, South Africa) · **Last reviewed:** 28 July 2026

Doc set: current state & forward plan → [ROADMAP.md](ROADMAP.md) · technical detail →
[ANDROID_APP_SPEC.md](ANDROID_APP_SPEC.md) · launch mechanics →
[LAUNCH_PLAYBOOK.md](LAUNCH_PLAYBOOK.md) · brand → [BRAND.md](BRAND.md).

**Status:** Android app built (Kotlin/Compose/Room/DataStore/widget); Google Play Billing
integrated (one-time `pro_unlock`); brand system applied; privacy policy written and
hosted. **Not yet:** domain registered, Play Console account, `pro_unlock` product
created, first compile pass, store graphics. Pre-revenue.

---

## 1. Executive summary

Parents repeat the same instructions dozens of times a day — "brush your teeth,"
"stop fighting," "eat your veggies." Repeatless lets a parent record those lines
**once, in their own voice**, turn each into a big tappable button, and replay them
on demand. Its differentiator is **Auto-Parent AI**: an opt-in mode that listens,
transcribes children's speech **on-device**, and automatically plays the parent's
matching clip — the parent's voice answers even when the parent is busy.

The privacy architecture is a core selling point, not a footnote: **audio never
leaves the phone**. Only on-device text transcripts are sent to Google's Gemini
Flash Lite API for matching.

The plan: launch free on Google Play with a **one-time Pro unlock** (no ads, no
subscription), grow through short-form video (the product demos itself — a child
whines and Mom's voice instantly booms back), and keep costs near zero
(< US$100 to launch; AI cost ≈ US$0.0001 per match).

Year-1 realistic outcome: a profitable micro-business with genuine viral upside;
every scenario above ~25 Pro sales is cash-flow positive because fixed costs are
trivial.

---

## 2. Problem & insight

**Problem.** Repetition is one of the most-cited daily frustrations of parenting.
The instructions are predictable, the phrasing is identical, and the emotional cost
(nagging fatigue, shouting guilt) is high.

**Insights.**
1. Kids respond to *their own parent's real voice* — not a robot voice. Existing
   "parent soundboard" attempts use text-to-speech and feel like a gimmick.
2. The repetition is *predictable*, which makes it automatable: a small set of
   recorded lines covers the vast majority of daily nagging.
3. The demo is inherently funny and shareable. "My mom cloned her nagging" is a
   TikTok-native story. The product's best ad is 15 seconds of it working.
4. Parents are privacy-sensitive about children's audio. "Your recordings never
   leave your phone" is a competitive weapon.

---

## 3. Product

**Shipped (Android, native Kotlin + Jetpack Compose):**
- **Google Play Billing:** one-time `pro_unlock` purchase with Play-supplied localised
  pricing, purchase acknowledgement and Restore purchases; entitlement cached to
  DataStore so PRO survives offline.
- Record/label/colour clips; multiple boards; drag-reorder; haptics; themes.
- Home-screen widget (opens the chosen board).
- Local backup/restore (also imports the original web prototype's backups).
- **Auto-Parent AI (Pro):** on-device speech-to-text → text-only call to Gemini
  Flash Lite → plays the best-matching clip. Sensitivity, cooldown, feedback
  training, test simulator.
- Web prototype (also serves as a free marketing demo).

**Privacy model:** recordings and microphone audio stay on-device, always. Only
transcript text + clip labels go to the Gemini API, only while Auto-Parent is on.

**Moat.** No structural moat (soundboards are copyable), but a real head start:
own-voice + AI auto-trigger + privacy-first is a combination no incumbent has, and
the brand/UGC flywheel ("what would your button say?") compounds. Speed and
distribution are the strategy; polish and trust are the retention.

---

## 4. Market

Analyst estimates for the global parenting-apps market in 2025 range from
**US$0.66B to US$1.94B** depending on scope, with double-digit CAGRs and North
America ~36.5% of revenue (sources in §12). Take the ranges as directional: the
category is large, growing, and dominated by pregnancy/tracking apps — the
"daily-parenting utility" niche Repeatless occupies is underserved.

- **TAM (directional):** parenting-app spend, ~US$1–2B/yr and growing.
- **SAM:** Android-first, English-speaking parents of kids ~2–12 who buy utility
  apps — tens of millions of households (Android ≈ 70%+ of global smartphones).
- **SOM (yr 1):** 5k–250k installs driven almost entirely by short-form video
  performance; monetizable at a 2–4% one-time-purchase conversion.

South Africa is the founder's home market and a good soft-launch/test market
(English content, low CPMs), but the product is global from day one.

---

## 5. Competition

| Competitor | What it is | Own voice? | AI auto-trigger? | Privacy story | Threat |
|---|---|---|---|---|---|
| Parents Soundboard (iOS/Android) | Hobby app; predefined sentences via **text-to-speech** | ❌ robot voice | ❌ | – | Low |
| SoundingBoard (AAC) | Assistive-communication boards, recordable | ✅ | ❌ | – | Low (different job/audience) |
| Generic soundboard/meme apps | Meme sounds, ad-heavy | rarely | ❌ | poor (ads/trackers) | Low |
| Voice memo apps | Recording, no board UX | ✅ | ❌ | – | Low |
| Smart-speaker routines (Alexa/Google announcements) | Scheduled/manual announcements | partly | ❌ (no listening-and-matching) | cloud audio | Medium (adjacent behaviour) |
| Fast follower / copycat | Post-virality clone | – | – | – | **Highest real threat** |

**Positioning:** *the only parenting app that answers your kids in your own voice —
automatically — without your recordings ever leaving your phone.*

---

## 6. Brand & naming (decided)

**Name: Repeatless.** Benefit-led ("repeat less"), invented and ownable,
adult-facing (deliberately avoids kid-app aesthetics — important for Play policy,
see §10), clean search results.

- **Domain:** `repeatless.app` — checked **available (DNS NXDOMAIN) on 2026-07-27**, still
  unregistered as of 2026-07-28.
  Availability changes by the minute: **register immediately** (~US$12–15/yr at
  Cloudflare/Namecheap/Porkbun). Optionally add `repeatless.co.za` (~R60–100/yr).
  `repeatless.com` is registered to a third party (not a consumer app; a small
  automation consultancy uses repeatless.in) — acquirable later if ever needed.
- **Android application ID:** `app.repeatless` (reverse-DNS of the domain).
- **Handles to claim now:** @repeatless / @repeatless.app on TikTok, Instagram,
  YouTube, X, Facebook.
- **Names evaluated and rejected:** *SayOnce* (existing SoftOrbits voice-dictation
  product of the same name — direct collision in voice software), *Broken Record*
  (famous music podcast; keep as marketing copy only), *OnRepeat* (music
  connotation, generic), *NagBot* (domains taken; negative frame), *PocketParrot*
  (reads as a children's app — policy and positioning risk).
- **To do:** formal trademark screening (ZA + US/EU classes 9/42) before spending
  on brand assets. Search-level screening found no consumer-app conflicts.

---

## 7. Business model & pricing

**Model:** Free + one-time Pro unlock (no ads, no subscription). Decided earlier in
the project and validated by the audience: parents are subscription-fatigued, and
"pay once" matches the "record once" brand promise.

| Tier | Price | Includes |
|---|---|---|
| Free | R0 / $0 | 2 boards × 12 clips, 1 widget, backup/restore |
| **Pro** | **US$4.99 one-time** (launch price; raise to $6.99 once reviews > 200) | Unlimited boards/clips, **Auto-Parent AI** |

- Google's fee is **15%** on the first $1M/yr (enrol in the 15% service-fee tier)
  → net ≈ **$4.24** per Pro sale.
- Price regionally (Play's price templates); keep SA/India/Brazil affordable.
- **Future option (not v1):** if Auto-Parent heavy usage grows, introduce an
  optional "Repeatless+" subscription for power features (family sharing, cloud
  voice packs) — never move already-purchased features behind it.

### Unit economics (AI cost)

Gemini 3.1 Flash-Lite: **$0.125/M input, $0.75/M output tokens** (July 2026).
A match call ≈ 350 in + 60 out tokens ≈ **$0.00009 (~1/100th of a cent)**.

| Auto-Parent usage | Calls/month | Cost/user/month |
|---|---|---|
| Casual (10 matches/day) | 300 | ~$0.03 |
| Typical (30/day) | 900 | ~$0.08 |
| Heavy (100/day) | 3,000 | ~$0.27 |

Even a *heavy* Pro user costs ~$3.30/yr against a $4.24 net one-time payment —
acceptable at launch scale, but it means: (a) Auto-Parent stays **Pro-only** ✅
(already true), (b) add a generous per-device daily cap (e.g. 200 matches) in
v1.1, (c) move the API key behind a thin proxy (free-tier Cloudflare Worker)
before scale — embedded keys can be extracted from APKs (see §10).

---

## 8. Go-to-market

**Phase 0 — Foundation (weeks 1–2)**
Register domain + handles; hosted privacy policy (done — in this repo); landing
page with mailing-list capture; Play listing assets; create the `pro_unlock` in-app
product in Play Console (client-side billing is already integrated).

**Phase 1 — Closed testing that doubles as marketing (weeks 2–5)**
Google Play requires new *personal* dev accounts to run a closed test with
**12 testers opted-in for 14 consecutive days** before production access
(accounts after Nov 2023; **organization accounts are exempt** — see §11).
Recruit testers from r/Parenting, r/daddit, r/Mommit, local parent WhatsApp/
Facebook groups, and friends — frame it as a founding-parents beta; these become
launch-day reviewers.

**Phase 2 — Launch (weeks 5–8)**
- **Short-form video is the entire engine.** The money shot: child whines about
  broccoli → phone lights up → *Mom's actual voice*: "EAT YOUR VEGGIES." Cut to
  mom drinking coffee in the next room. 15 seconds, endlessly remixable.
- Formats: skit demos; "POV: your mom automated herself"; build-in-public founder
  clips; **UGC bait: "What would YOUR button say?"** (comment-section goldmine →
  duet/stitch loop).
- Seed 10–20 micro parenting creators (5k–100k followers) with free Pro codes;
  pay-per-post only after organic signal.
- Product Hunt + r/SideProject + local SA tech press ("SA dad builds an app that
  nags his kids for him") for the founding story.
- ASO: target "parent soundboard", "record your voice buttons", "stop repeating
  yourself", "kids routine reminder" (full keyword sheet in LAUNCH_PLAYBOOK.md).

**Phase 3 — Compounding (months 3–12)**
Ship requested features (shared family boards, schedules/alarms, Bluetooth-speaker
casting); localize top non-English markets; evaluate iOS once Android proves
conversion (~R15k/yr cost: Mac + Apple fee — gate on revenue).

---

## 9. Financial plan

**Costs to launch (once-off):**

| Item | Cost |
|---|---|
| Google Play developer account | $25 (~R450) |
| Domain repeatless.app | ~$14/yr |
| Privacy policy hosting | $0 (GitHub Pages — live at primepaints.github.io/Repeatless-Android/privacy.html) |
| Design assets (DIY + free tools) | $0–$50 |
| **Total** | **< $100** |

**Running costs:** Gemini API (usage-scaled, table above), $0 servers (no backend
at launch). At 1,000 *heavy* Auto-Parent users: ~$270/mo — by which point revenue
(≥ tens of thousands of installs) dwarfs it.

**Year-1 revenue scenarios** (3% of installs buy Pro at $4.24 net):

| Scenario | Installs yr 1 | Pro sales | Net revenue |
|---|---|---|---|
| Conservative (no viral hit) | 5,000 | 150 | ~$640 (~R11.5k) |
| Base (one modest viral video) | 25,000 | 750 | ~$3,200 (~R57k) |
| Upside (one big TikTok moment) | 250,000 | 7,500 | ~$31,800 (~R570k) |

Assumptions are explicit and testable: conversion 2–4% (tune with pricing tests),
installs driven ~90% by short-form video. Break-even is ~25 Pro sales.
**This is a near-zero-downside bet; the work, not capital, is the investment.**

---

## 10. Risks & mitigations

| Risk | Severity | Mitigation |
|---|---|---|
| **Children's-data optics** (app transcribes kids' speech) | High (reputational/regulatory) | Audio never leaves device (architectural); transcripts ephemeral, no accounts; app is **for parents**, not children — do NOT enrol in Play "Designed for Families"; privacy policy states children's-data handling plainly; Data Safety form scrupulously accurate |
| Embedded Gemini API key extracted & abused | Medium-High | Restrict key in Cloud Console; daily quota caps; Pro-gating limits exposure; move to thin proxy (Cloudflare Worker, free) before scale |
| Play policy (background mic use) | Medium | Auto-Parent is tied to the Activity lifecycle (stopped in `onDispose`) with a visible listening status — but there is **no prominent mic-use disclosure dialog and no foreground service yet**; both are pre-launch blockers (see ROADMAP v1.0 hardening) |
| Fast-follower copycats post-virality | Medium | Ship the roadmap fast; own the brand/UGC loop; reviews moat |
| "Lazy-parenting app" press angle | Medium | Lean into humour; frame as *consistency tool* ("same words, calm voice, every time"); parent testimonials |
| Gemini API price/model changes | Low-Med | Text-only calls are trivially portable (any LLM or on-device model later) |
| Single founder bandwidth | Medium | Keep scope brutal: Android + one marketing channel until revenue |
| Trademark surprise on "Repeatless" | Low | Formal screening before paid branding (§6) |

---

## 11. Launch path & timeline (from 27 Jul 2026)

**Account decision first:**
- **Path A — Personal account ($25):** subject to the 12-testers × 14-days rule →
  realistic production launch **mid/late September 2026**.
- **Path B — Organization account via Prime Paints ($25):** exempt from the tester
  rule; needs D‑U‑N‑S number + business verification (days–weeks) → potentially
  live **late August 2026**, and keeps the app under a business entity (cleaner
  for taxes/liability). **Recommended if Prime Paints is a registered entity.**

| Weeks | Milestone |
|---|---|
| 1–2 | Domain + handles registered; Play account created; `pro_unlock` product created + priced; **first Android Studio compile pass**; internal QA build |
| 2–4 | Closed testing (12+ testers) *(Path A)* or verification wait *(Path B)*; landing page + content backlog (10 videos) |
| 5–6 | Production review; store listing final; press/creator kit out |
| 6–8 | **Public launch**; daily content cadence; respond to every review |
| 9–12 | v1.1: usage caps, top-requested feature, pricing test ($4.99 vs $6.99) |

**KPIs:** installs, D7 retention (>20% good for utility), pads recorded per user
(activation ≥3), Pro conversion (≥2.5%), Auto-Parent DAU, video view→install rate,
rating (≥4.5).

**90-day definition of success:** live on Play, ≥5k installs, ≥125 Pro sales,
≥4.5★, one video ≥250k views, decision made on iOS.

---

## 12. Sources

Market size: [InsightAce Analytic](https://www.insightaceanalytic.com/report/parenting-apps-market/3219) ·
[Roots Analysis](https://www.rootsanalysis.com/reports/parenting-apps-market.html) ·
[The Business Research Company](https://www.thebusinessresearchcompany.com/report/parenting-apps-global-market-report) ·
[Global Growth Insights](https://www.globalgrowthinsights.com/market-reports/parenting-apps-market-102179) ·
[Data Insights Reports](https://www.datainsightsreports.com/reports/global-parenting-apps-market-2557)

Competitors: [Parents Soundboard (App Store)](https://apps.apple.com/us/app/parents-soundboard/id1434425575) ·
[Parents Soundboard write-up (dev.to)](https://dev.to/mokkapps/parents-soundboard-let-the-smartphone-speak-for-you-m3) ·
[SoundingBoard (App Store)](https://apps.apple.com/us/app/soundingboard/id390532167) ·
[Custom Soundboard & Voice Memo (App Store)](https://apps.apple.com/us/app/custom-soundboard-and-voice-memo-recorder/id617755214)

Play testing policy: [Google Play Console Help — app testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en) ·
[Google Play community guide — 12 testers](https://support.google.com/googleplay/android-developer/community-guide/255621488/everything-about-the-12-testers-requirement?hl=en)

Gemini pricing: [Gemini API pricing (Google)](https://ai.google.dev/gemini-api/docs/pricing) ·
[Gemini 3.1 Flash-Lite pricing (pricepertoken.com)](https://pricepertoken.com/pricing-page/model/google-gemini-3.1-flash-lite) ·
[CloudZero Gemini pricing guide](https://www.cloudzero.com/blog/gemini-pricing/)

Naming collisions: [SayOnce dictation software (SoftOrbits)](https://www.softorbits.net/voice-dictation-software/) ·
[Repeatless consultancy (repeatless.in)](https://www.repeatless.in/)

*Domain availability was verified by DNS (NXDOMAIN) on 2026-07-27; re-confirm at the
registrar at the moment of purchase.*
